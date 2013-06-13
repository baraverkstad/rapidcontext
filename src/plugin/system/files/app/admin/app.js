/**
 * Creates a new admin app.
 */
function AdminApp() {
    this._defaults = { operatorId: RapidContext.App.user().id };
    this._types = {};
    this._cxnIds = null;
    this._cxnCount = 0;
    this._currentProc = null;
    this._batch = { running: false, delay: 5, queue: [],
                    stat: { success: 0, failed: 0 },
                    func: MochiKit.Base.bind("_processBatch", this) };
}

/**
 * Starts the app and initializes the UI.
 */
AdminApp.prototype.start = function () {
    // Create procedure callers
    this.proc = RapidContext.Procedure.mapAll({
        typeList: "System.Type.List",
        cxnList: "System.Connection.List",
        cxnValidate: "System.Connection.Validate",
        appList: "System.App.List",
        plugInList: "System.PlugIn.List",
        procList: "System.Procedure.List",
        procDelete: "System.Procedure.Delete",
        procTypes: "System.Procedure.Types",
        userList: "System.User.List",
        userChange: "System.User.Change"
    });

    // All views
    MochiKit.Signal.connect(this.ui.root, "onenter", MochiKit.Base.bind("selectChild", this.ui.tabContainer, null));
    MochiKit.Signal.connect(this.proc.typeList, "onsuccess", this, "_updateTypeCache");

    // Connection view
    RapidContext.UI.connectProc(this.proc.cxnList, this.ui.cxnLoading, this.ui.cxnReload);
    MochiKit.Signal.connect(this.proc.cxnList, "onsuccess", this.ui.cxnTable, "setData");
    MochiKit.Signal.connect(this.proc.cxnList, "onsuccess", this, "_showConnection");
    MochiKit.Signal.connect(this.ui.cxnTable, "onselect", this, "_showConnection");
    MochiKit.Signal.connect(this.ui.cxnValidate, "onclick", this, "_validateConnections");
    MochiKit.Signal.connect(this.ui.cxnAdd, "onclick", this, "_addConnection");
    MochiKit.Signal.connect(this.ui.cxnRemove, "onclick", this, "_removeConnection");
    MochiKit.Signal.connect(this.ui.cxnEdit, "onclick", this, "_editConnection");
    MochiKit.Signal.connect(this.ui.cxnEditType, "onchange", this, "_updateConnectionEdit");
    MochiKit.Signal.connect(this.ui.cxnEditShowAll, "onchange", this, "_updateConnectionEdit");
    MochiKit.Signal.connect(this.ui.cxnEditForm, "onclick", this, "_addRemoveConnectionProps")
    MochiKit.Signal.connect(this.ui.cxnEditCancel, "onclick", this.ui.cxnEditDialog, "hide");
    MochiKit.Signal.connect(this.ui.cxnEditSave, "onclick", this, "_storeConnection");
    var statusRenderer = function (td, value, data) {
        if (data._error || data._lastError) {
            td.appendChild(RapidContext.Widget.Icon({ ref: "ERROR" }));
        } else if (!data._openChannels) {
            td.appendChild(RapidContext.Widget.Icon({ ref: "WARNING", tooltip: "Unknown" }));
        } else {
            td.appendChild(RapidContext.Widget.Icon({ ref: "OK" }));
        }
    };
    var typeRenderer = function (td, value, data) {
        if (/^connection\//.test(value)) {
            td.appendChild(RapidContext.Util.createTextNode(value.substr(11)));
        }
    };
    this.ui.cxnTable.getChildNodes()[1].setAttrs({ renderer: statusRenderer });
    this.ui.cxnTable.getChildNodes()[2].setAttrs({ renderer: typeRenderer });

    // App view
    MochiKit.Signal.connectOnce(this.ui.appTab, "onenter", this, "loadApps");
    RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
    MochiKit.Signal.connect(this.proc.appList, "onsuccess", this.ui.appTable, "setData");
    MochiKit.Signal.connect(this.proc.appList, "onsuccess", this, "_showApp");
    MochiKit.Signal.connect(this.ui.appTable, "onselect", this, "_showApp");
    MochiKit.Signal.connect(this.ui.appLaunch, "onclick", this, "_launchApp");
    MochiKit.Signal.connect(this.ui.appLaunchWindow, "onclick", this, "_launchAppWindow");
    var urlRenderer = function (td, value, data) {
        if (value) {
            var styles = { marginLeft: "3px" };
            var attrs = { ref: "EXPAND", tooltip: "Open in new window", style: styles };
            var img = RapidContext.Widget.Icon(attrs);
            var link = MochiKit.DOM.A({ href: value, target: "_blank" }, value, img);
            td.appendChild(link);
        }
    };
    this.ui.appResourceTable.getChildNodes()[1].setAttrs({ renderer: urlRenderer });

    // Plug-in view
    MochiKit.Signal.connectOnce(this.ui.pluginTab, "onenter", this, "loadPlugins");
    MochiKit.Signal.connect(this.ui.pluginTab, "onenter", this, "_pluginUploadInit");
    MochiKit.Signal.connect(this.ui.pluginFile, "onselect", this, "_pluginUploadStart");
    MochiKit.Signal.connect(this.ui.pluginInstall, "onclick", this, "_pluginInstall");
    MochiKit.Signal.connect(this.ui.pluginReset, "onclick", this, "resetServer");
    MochiKit.Signal.connect(this.ui.pluginFileDelete, "onclick", this, "_pluginUploadInit");
    RapidContext.UI.connectProc(this.proc.plugInList, this.ui.pluginLoading, this.ui.pluginReload);
    MochiKit.Signal.connect(this.proc.plugInList, "onsuccess", this.ui.pluginTable, "setData");
    MochiKit.Signal.connect(this.proc.plugInList, "onsuccess", this, "_showPlugin");
    MochiKit.Signal.connect(this.ui.pluginTable, "onselect", this, "_showPlugin");
    MochiKit.Signal.connect(this.ui.pluginLoad, "onclick", this, "_togglePlugin");
    MochiKit.Signal.connect(this.ui.pluginUnload, "onclick", this, "_togglePlugin");
    var statusRenderer = function (td, value, data) {
        if (value) {
            td.appendChild(RapidContext.Widget.Icon({ ref: "OK", tooltip: "Loaded" }));
        } else {
            td.appendChild(RapidContext.Widget.Icon({ ref: "ERROR", tooltip: "Not loaded" }));
        }
    };
    this.ui.pluginTable.getChildNodes()[0].setAttrs({ renderer: statusRenderer });

    // Procedure view
    MochiKit.Signal.connectOnce(this.ui.procTab, "onenter", this, "loadProcedures");
    RapidContext.UI.connectProc(this.proc.procList, this.ui.procTreeLoading, this.ui.procTreeReload);
    MochiKit.Signal.connect(this.proc.procList, "onsuccess", this, "_callbackProcedures");
    RapidContext.UI.connectProc(this.proc.procDelete);
    MochiKit.Signal.connect(this.proc.procDelete, "oncall", this.ui.overlay, "show");
    MochiKit.Signal.connect(this.proc.procDelete, "onresponse", this.ui.overlay, "hide");
    MochiKit.Signal.connect(this.proc.procDelete, "onsuccess", this.proc.procList, "recall");
    MochiKit.Signal.connect(this.ui.procTree, "onselect", this, "_showProcedure");
    MochiKit.Signal.connect(this.ui.procAdd, "onclick", this, "_addProcedure");
    MochiKit.Signal.connect(this.ui.procRemove, "onclick", this, "_removeProcedure");
    MochiKit.Signal.connect(this.ui.procEdit, "onclick", this, "_editProcedure");
    MochiKit.Signal.connect(this.ui.procEditType, "onchange", this, "_updateProcEdit");
    MochiKit.Signal.connect(this.ui.procEditAdd, "onclick", this, "_addProcBinding");
    MochiKit.Signal.connect(this.ui.procEditCancel, "onclick", this.ui.procEditDialog, "hide");
    MochiKit.Signal.connect(this.ui.procEditSave, "onclick", this, "_saveProcedure");
    MochiKit.Signal.connect(this.ui.procReload, "onclick", this, "_showProcedure");
    MochiKit.Signal.connect(this.ui.procExec, "onclick", this, "_executeProcedure");
    MochiKit.Signal.connect(this.ui.procBatch, "onclick", this, "_createBatch");
    MochiKit.Signal.connect(this.ui.procExecResult, "onexpand", this, "_showExecData");
    MochiKit.Signal.connect(this.ui.procArgCancel, "onclick", this.ui.procArgDialog, "hide");
    MochiKit.Signal.connect(this.ui.procArgSave, "onclick", this, "_updateProcArg");
    this.ui.procExecLoading.hide();
    this.ui.procExecResult.resizeContent = function () {
        var pos = MochiKit.Style.getElementPosition(this, this.parentNode);
        var dim = MochiKit.Style.getElementDimensions(this.parentNode);
        MochiKit.Style.setElementDimensions(this, { h: dim.h - pos.y });
    }

    // Batch view
    MochiKit.Signal.connect(this.ui.batchDelete, "onclick", this, "_clearBatch");
    MochiKit.Signal.connect(this.ui.batchDelay, "onclick", this, "_configBatchDelay");
    MochiKit.Signal.connect(this.ui.batchResume, "onclick", this, "_toggleBatch");

    // User view
    MochiKit.Signal.connectOnce(this.ui.userTab, "onenter", this, "loadUsers");
    RapidContext.UI.connectProc(this.proc.userList, this.ui.userLoading, this.ui.userReload);
    MochiKit.Signal.connect(this.proc.userList, "onsuccess", this.ui.userTable, "setData");
    MochiKit.Signal.connect(this.ui.userTable, "onselect", this, "_editUser");
    MochiKit.Signal.connect(this.ui.userAdd, "onclick", this, "_addUser");
    MochiKit.Signal.connect(this.ui.userSave, "onclick", this, "_saveUser");
    RapidContext.UI.connectProc(this.proc.userChange);
    MochiKit.Signal.connect(this.proc.userChange, "onsuccess", this.proc.userList, "recall");

    // Log view
    MochiKit.Signal.connect(this.ui.logTab, "onenter", this, "_showLogs");
    MochiKit.Signal.connect(this.ui.logError, "onclick", MochiKit.Base.bind("setLogLevel", this, LOG.ERROR));
    MochiKit.Signal.connect(this.ui.logWarning, "onclick", MochiKit.Base.bind("setLogLevel", this, LOG.WARNING));
    MochiKit.Signal.connect(this.ui.logInfo, "onclick", MochiKit.Base.bind("setLogLevel", this, LOG.INFO));
    MochiKit.Signal.connect(this.ui.logTrace, "onclick", MochiKit.Base.bind("setLogLevel", this, LOG.TRACE));
    MochiKit.Signal.connect(this.ui.logClear, "onclick", this, "_clearLogs");
    MochiKit.Signal.connect(this.ui.logReload, "onclick", this, "_showLogs");
    MochiKit.Signal.connect(this.ui.logTable, "onselect", this, "_showLogDetails");

    // TODO: Security test should be made on access, not role name
    var user = RapidContext.App.user();
    if (!user || !user.role || MochiKit.Base.findValue(user.role, "admin") < 0) {
        this.ui.procAdd.hide();
        this.ui.procRemove.addClass("hidden");
        this.ui.tabContainer.removeChildNode(this.ui.cxnTab);
        this.ui.tabContainer.removeChildNode(this.ui.pluginTab);
        this.ui.tabContainer.removeChildNode(this.ui.userTab);
        RapidContext.Widget.destroyWidget(this.ui.cxnTab);
        RapidContext.Widget.destroyWidget(this.ui.pluginTab);
        RapidContext.Widget.destroyWidget(this.ui.userTab);
        this.ui.cxnTab = null;
        this.ui.pluginTab = null;
        this.ui.userTab = null;
    }

    // Initialize data
    if (this.ui.cxnTab) {
        this.proc.cxnList();
    } else {
        this.loadApps();
    }
    this._showProcedure();
    this._stopBatch();
}

/**
 * Stops the app.
 */
AdminApp.prototype.stop = function () {
    for (var name in this.proc) {
        MochiKit.Signal.disconnectAll(this.proc[name]);
    }
}

/**
 * Updates the type cache.
 */
AdminApp.prototype._updateTypeCache = function (res) {
    this._types = {};
    for (var i = 0; i < res.length; i++) {
        var type = res[i];
        this._types[type.id] = type;
        type.property = type.property || [];
        type.properties = {};
        for (var j = 0; j < type.property.length; j++) {
            var prop = type.property[j];
            type.properties[prop.name] = prop;
        }
        delete type.property;
    }
    for (var id in this._types) {
        var type = this._types[id];
        var parts = id.split("/");
        while (parts.length > 1) {
            parts.pop();
            var baseId = parts.join("/");
            var baseType = this._types[baseId];
            if (baseType && baseType.properties) {
                MochiKit.Base.setdefault(type.properties, baseType.properties);
            }
        }
    }
}

/**
 * Validates all connections. The connection list is updated before
 * the validation starts.
 */
AdminApp.prototype._validateConnections = function () {
    this.ui.overlay.setAttrs({ message: "Validating..." });
    this.ui.overlay.show();
    var d = this.proc.cxnList();
    d.addBoth(MochiKit.Base.bind("_validateCallback", this));
}

/**
 * Connection validation callback handler. This method will iterate
 * over all the connections in the connection table one by one. This
 * callback method will be called between each validation.
 *
 * @return {Deferred} a MochiKit.Async.Deferred that will callback
 *         when the validation is complete
 */
AdminApp.prototype._validateCallback = function () {
    if (this._cxnIds == null) {
        var data = this.ui.cxnTable.getData();
        this._cxnIds = MochiKit.Base.map(MochiKit.Base.itemgetter("id"), data);
        this._cxnCount = this._cxnIds.length;
    }
    if (this._cxnIds.length == 0) {
        this._cxnIds = null;
        this.ui.overlay.hide();
        return this.proc.cxnList();
    } else {
        var id = this._cxnIds.shift();
        var pos = this._cxnCount - this._cxnIds.length;
        var msg = "Validating " + pos + " of " + this._cxnCount + "...";
        this.ui.overlay.setAttrs({ message: msg });
        var d = this.proc.cxnValidate(id);
        d.addBoth(MochiKit.Base.method(this, "_validateCallback"));
        return d;
    }
}

/**
 * Shows the specified connection in the Admin UI. This method will
 * show the connection pane (if not already displaying), validate
 * the connection specified, update the connection list and finally
 * display the connection details.
 *
 * @param {String} id the connection identifier
 */
AdminApp.prototype.showConnection = function (id) {
    this.ui.tabContainer.selectChild(this.ui.cxnTab);
    var cxnList = this.proc.cxnList;
    var cxnTable = this.ui.cxnTable;
    var d = this.proc.cxnValidate(id);
    d.addBoth(function () { return cxnList(); });
    d.addCallback(function () { cxnTable.setSelectedIds(id); });
}

/**
 * Shows detailed connection information.
 */
AdminApp.prototype._showConnection = function () {
    var data = this.ui.cxnTable.getSelectedData();
    data = MochiKit.Base.update({}, data);
    if (/^@\d+$/.test(data._lastUsedTime)) {
        var dttm = new Date(+data._lastUsedTime.substr(1));
        data.lastAccess = MochiKit.DateTime.toISOTimestamp(dttm);
    }
    this.ui.cxnForm.reset();
    this.ui.cxnForm.update(data);
    if (data.plugin == "local") {
        this.ui.cxnRemove.show();
    } else {
        this.ui.cxnRemove.hide();
    }
    if (data.plugin && data.id) {
        this.ui.cxnEdit.show();
        var url = "rapidcontext/storage/connection/" + data.id;
        MochiKit.DOM.setNodeAttribute(this.ui.cxnLink, "href", url);
        MochiKit.DOM.removeElementClass(this.ui.cxnLink, "hidden");
    } else {
        this.ui.cxnEdit.hide();
        MochiKit.DOM.addElementClass(this.ui.cxnLink, "hidden");
    }
    while (this.ui.cxnTemplate.previousSibling.className == "template") {
        RapidContext.Widget.destroyWidget(this.ui.cxnTemplate.previousSibling);
    }
    var hidden = ["lastAccess", "id", "type", "plugin", "maxOpen", "_maxOpen",
                  "_usedChannels", "_openChannels", "_lastUsedTime"];
    RapidContext.Util.mask(data, hidden);
    for (var k in data) {
        if (!/^_/.test(k) || !(k.substr(1) in data)) {
            var title = RapidContext.Util.toTitleCase(k);
            var value = data[k];
            if (value == null) {
                value = "";
            }
            if (/error$/i.test(k)) {
                value = MochiKit.DOM.SPAN({ "class": "important" }, value);
            } else if (/password$/i.test(k)) {
                value = RapidContext.Widget.Field({ name: k, value: value, mask: true });
            }
            var tr = this.ui.cxnTemplate.cloneNode(true);
            tr.className = "template";
            MochiKit.DOM.appendChildNodes(tr.firstChild, title + ":");
            MochiKit.DOM.appendChildNodes(tr.lastChild, value);
            MochiKit.DOM.insertSiblingNodesBefore(this.ui.cxnTemplate, tr);
        }
    }
}

/**
 * Opens the connection editing dialog for a new connection.
 */
AdminApp.prototype._addConnection = function () {
    this._initConnectionEdit({});
}

/**
 * Removes a connection (after user confirmation).
 */
AdminApp.prototype._removeConnection = function () {
    var data = this.ui.cxnTable.getSelectedData();
    var msg = "Delete the connection '" + data.id + "'?";
    if (confirm(msg)) {
        var path = "connection/" + data.id;
        var d = AdminApp.storageDelete(path);
        d.addErrback(RapidContext.UI.showError);
        d.addCallback(MochiKit.Base.method(this.proc.cxnList, "recall"));
    }
}

/**
 * Opens the connection editing dialog for an existing connection.
 */
AdminApp.prototype._editConnection = function () {
    var data = this.ui.cxnTable.getSelectedData();
    this._initConnectionEdit(data);
}

/**
 * Initializes the connection editing dialog.
 */
AdminApp.prototype._initConnectionEdit = function (data) {
    this.ui.cxnEditDialog.data = data;
    this.ui.cxnEditForm.reset();
    this.ui.cxnEditForm.update(data);
    var self = this;
    var d = this.proc.typeList();
    d.addCallback(function () {
        var select = self.ui.cxnEditType;
        while (select.firstChild.nextSibling) {
            RapidContext.Widget.destroyWidget(select.firstChild.nextSibling);
        }
        for (var k in self._types) {
            if (/connection\//.test(k)) {
                var attrs = { value: k };
                if (data.type === k) {
                    attrs.selected = true;
                }
                select.appendChild(MochiKit.DOM.OPTION(attrs, k));
            }
        }
    });
    d.addCallback(MochiKit.Base.bind("_updateConnectionEdit", this));
    d.addCallback(MochiKit.Base.bind("show", this.ui.cxnEditDialog));
    d.addErrback(RapidContext.UI.showError);
    return d;
}

/**
 * Updates the connection editing dialog.
 */
AdminApp.prototype._updateConnectionEdit = function () {
    var data = this.ui.cxnEditForm.valueMap();
    var showAll = (data._showAll == "yes");
    this.ui.cxnEditForm.reset();
    MochiKit.Base.setdefault(data, this.ui.cxnEditDialog.data);
    while (this.ui.cxnEditTemplate.previousSibling.className == "template") {
        RapidContext.Widget.destroyWidget(this.ui.cxnEditTemplate.previousSibling);
    }
    var hiddenProps = { id: true, type: true, plugin: true };
    var props = {};
    if (this._types[data.type]) {
        var type = this._types[data.type];
        MochiKit.Base.update(props, type.properties);
        MochiKit.DOM.replaceChildNodes(this.ui.cxnEditTypeDescr, type.description);
    } else {
        MochiKit.DOM.replaceChildNodes(this.ui.cxnEditTypeDescr);
    }
    for (var name in data) {
        var value = MochiKit.Format.strip(data[name]);
        if (!/^_/.test(name) && !(name in props) && !(name in hiddenProps) && value) {
            props[name] = { name: name, title: name, custom: true,
                            description: "User-specified parameter." };
            if (/password$/i.test(name)) {
                props[name].format = "password";
            }
        }
    }
    for (var name in props) {
        var p = props[name];
        var title = p.title || RapidContext.Util.toTitleCase(name);
        var value = (data[name] != null) ? "" + data[name] : "";
        var defaultValue = (data["_" + name] != null) ? "" + data["_" + name] : "";
        var valueLines = AdminApp.splitLines(value, 58);
        var tr = this.ui.cxnEditTemplate.cloneNode(true);
        tr.className = "template";
        MochiKit.DOM.appendChildNodes(tr.firstChild, title + ":");
        var attrs = { name: name, cols: 58, size: 60 };
        if (defaultValue) {
            attrs.placeholder = defaultValue;
        }
        var input = null;
        if (p.format == "text" || valueLines.length > 1) {
            attrs.rows = Math.min(Math.max(2, valueLines.length), 20);
            input = RapidContext.Widget.TextArea(attrs);
        } else if (p.format == "password") {
            attrs.type = "password";
            input = RapidContext.Widget.TextField(attrs);
        } else {
            input = RapidContext.Widget.TextField(attrs);
        }
        MochiKit.DOM.appendChildNodes(tr.lastChild, input);
        if (p.custom) {
            var style = { "margin-left": "3px" };
            var icon = RapidContext.Widget.Icon({ ref: "REMOVE", style: style });
            icon["data-remove"] = true;
            MochiKit.DOM.appendChildNodes(tr.lastChild, icon);
        }
        if (p.required && p.format != "password") {
            var attrs = { name: name, display: "icon" };
            var validator = RapidContext.Widget.FormValidator(attrs);
            MochiKit.DOM.appendChildNodes(tr.lastChild, validator);
        }
        if (p.description) {
            var help = MochiKit.DOM.DIV({ "class": "helptext preformatted" }, p.description);
            MochiKit.DOM.appendChildNodes(tr.lastChild, help);
        }
        if (!showAll && !p.required && !p.custom && !value) {
            tr.style.display = "none";
        }
        MochiKit.DOM.insertSiblingNodesBefore(this.ui.cxnEditTemplate, tr);
    }
    this.ui.cxnEditForm.update(data);
}

/**
 * Handles addition and removal of custom connection parameters.
 */
AdminApp.prototype._addRemoveConnectionProps = function (evt) {
    var elem = evt.target();
    if (elem === this.ui.cxnEditAdd) {
        var name = MochiKit.Format.strip(this.ui.cxnEditAddParam.value);
        if (/[a-z0-9_-]+/i.test(name)) {
            this.ui.cxnEditAddParam.setAttrs({ name: name, value: "value" });
            this._updateConnectionEdit();
            this.ui.cxnEditAddParam.setAttrs({ name: "_add", value: "" });
        } else {
            alert("Invalid parameter name.");
        }
    } else if (elem["data-remove"] === true) {
        var tr = MochiKit.DOM.getFirstParentByTagAndClassName(elem, "TR");
        RapidContext.Widget.destroyWidget(tr);
    }
}

/**
 * Stores the edited or new connection to the RapidContext storage.
 */
AdminApp.prototype._storeConnection = function () {
    this.ui.cxnEditForm.validateReset();
    if (this.ui.cxnEditForm.validate()) {
        var data = this.ui.cxnEditForm.valueMap();
        var prevData = this.ui.cxnEditDialog.data;
        var path = "connection/" + data.id;
        for (var name in data) {
            var value = MochiKit.Format.strip(data[name]);
            if (/^_/.test(name) || value == "") {
                delete data[name];
            }
        }
        var d = AdminApp.storageWrite(path, data);
        if (prevData.id && prevData.id != data.id) {
            var oldPath = "connection/" + prevData.id;
            d.addCallback(MochiKit.Base.partial(AdminApp.storageDelete, oldPath));
        }
        d.addErrback(RapidContext.UI.showError);
        d.addCallback(MochiKit.Base.method(this.ui.cxnEditDialog, "hide"));
        d.addCallback(MochiKit.Base.method(this, "showConnection", data.id));
    }
}

/**
 * Loads the app list.
 */
AdminApp.prototype.loadApps = function () {
    this.proc.appList();
}

/**
 * Shows detailed app information.
 */
AdminApp.prototype._showApp = function () {
    var data = this.ui.appTable.getSelectedData();
    this.ui.appForm.reset();
    if (data) {
        this.ui.appForm.update(data);
        var img = null;
        for (var i = 0; i < data.resources.length; i++) {
            var res = data.resources[i];
            if (res.type == "icon") {
                img = MochiKit.DOM.IMG({ src: res.url });
            }
        }
        MochiKit.DOM.replaceChildNodes(this.ui.appIcon, img);
        var url = "rapidcontext/storage/app/" + data.id;
        MochiKit.DOM.setNodeAttribute(this.ui.appLink, "href", url);
        MochiKit.DOM.removeElementClass(this.ui.appLink, "hidden");
        this.ui.appResourceTable.show();
        this.ui.appResourceTable.setData(data.resources);
    } else {
        MochiKit.DOM.replaceChildNodes(this.ui.appIcon);
        MochiKit.DOM.addElementClass(this.ui.appLink, "hidden");
        this.ui.appResourceTable.hide();
    }
    this.ui.appLaunch.setAttrs({ disabled: !data });
    this.ui.appLaunchWindow.setAttrs({ disabled: !data });
}

/**
 * Launches the currently selected app.
 */
AdminApp.prototype._launchApp = function () {
    var data = this.ui.appTable.getSelectedData();
    RapidContext.App.startApp(data.id);
}

/**
 * Launches the currently selected app in separate window.
 */
AdminApp.prototype._launchAppWindow = function () {
    var data = this.ui.appTable.getSelectedData();
    RapidContext.App.startApp(data.id, window.open());
}

/**
 * Loads the table of available plug-ins.
 */
AdminApp.prototype.loadPlugins = function () {
    return this.proc.plugInList();
}

/**
 * Shows the details for the currently selected plug-in.
 */
AdminApp.prototype._showPlugin = function () {
    var data = this.ui.pluginTable.getSelectedData();
    this.ui.pluginForm.reset();
    if (data) {
        this.ui.pluginForm.update(data);
        MochiKit.DOM.removeElementClass(this.ui.pluginLink, "hidden");
        var path = "/storage/plugin/" + data.id + "/";
        var url = "rapidcontext/storage" + path;
        MochiKit.DOM.setNodeAttribute(this.ui.pluginLink, "href", url);
        if (data.loaded) {
            this.ui.pluginLoad.hide();
            this.ui.pluginUnload.show();
        } else {
            this.ui.pluginLoad.show();
            this.ui.pluginUnload.hide();
        }
        if (data.id === "system" || data.id === "local") {
            this.ui.pluginUnload.hide();
        }
    } else {
        MochiKit.DOM.addElementClass(this.ui.pluginLink, "hidden");
        this.ui.pluginLoad.hide();
        this.ui.pluginUnload.hide();
    }
}

/**
 * Loads or unloads the currently selected plug-in.
 */
AdminApp.prototype._togglePlugin = function () {
    this.ui.pluginReload.hide();
    this.ui.pluginLoading.show();
    var data = this.ui.pluginTable.getSelectedData();
    var proc = data.loaded ? "System.PlugIn.Unload" : "System.PlugIn.Load";
    var d = RapidContext.App.callProc(proc, [data.id]);
    d.addErrback(RapidContext.UI.showError);
    if (data.loaded && data.className) {
        d.addCallback(function () {
            alert("Note: Unloading Java resources requires a full server restart.");
        });
    }
    // TODO: This should be handled internally on the server...
    d.addBoth(MochiKit.Base.bind("resetServer", this));
    d.addBoth(MochiKit.Base.bind("loadPlugins", this))
}

/**
 * Initializes the plug-in file upload and installation interface.
 */
AdminApp.prototype._pluginUploadInit = function () {
    this.ui.pluginFile.show();
    this.ui.pluginProgress.hide();
    this.ui.pluginFileInfo.hide();
    this.ui.pluginInstall.disable();
}

/**
 * Handles the plug-in file upload init.
 */
AdminApp.prototype._pluginUploadStart = function () {
    this.ui.pluginFile.hide();
    this.ui.pluginProgress.show();
    this.ui.pluginProgress.setAttrs({ min: 0, max: 100, ratio: 0 });
    this._pluginUploadProgress();
}

/**
 * Shows the progress for the plug-in file upload.
 *
 * @param {Object} [res] the optional session data object
 */
AdminApp.prototype._pluginUploadProgress = function (res) {
    var selfCallback = MochiKit.Base.bind("_pluginUploadProgress", this);
    function pluginLoadStatus() {
        var d = RapidContext.App.callProc("System.Session.Current");
        d.addBoth(selfCallback);
    }
    if (res && res.files && res.files.plugin) {
        this._pluginUploadInfo(res.files.plugin);
    } else {
        MochiKit.Async.callLater(1, pluginLoadStatus);
        if (res && res.files && res.files.progress) {
            this.ui.pluginProgress.setAttrs({ ratio: res.files.progress });
        }
    }
}

/**
 * Shows the information for an uploaded plug-in file.
 *
 * @param {Object} file the session file data object
 */
AdminApp.prototype._pluginUploadInfo = function (file) {
    this.ui.pluginFile.hide();
    this.ui.pluginProgress.hide();
    this.ui.pluginFileInfo.show();
    var value = parseInt(file.size);
    if (value > 1000000) {
        file.approxSize = MochiKit.Format.roundToFixed(value / 1048576, 1) + " MiB";
    } else if (value > 2000) {
        file.approxSize = MochiKit.Format.roundToFixed(value / 1024, 1) + " KiB";
    } else {
        file.approxSize = file.size + " bytes";
    }
    this.ui.pluginUploadForm.update(file);
    this.ui.pluginInstall.enable();
}

/**
 * Performs a plug-in installation.
 */
AdminApp.prototype._pluginInstall = function () {
    var self = this;
    var id;
    this.ui.overlay.setAttrs({ message: "Installing..." });
    this.ui.overlay.show();
    var d = RapidContext.App.callProc("System.PlugIn.Install", ["plugin"]);
    d.addCallback(function (res) {
        id = res;
    });
    // TODO: This should be handled internally on the server...
    d.addCallback(MochiKit.Base.bind("resetServer", this));
    d.addCallback(MochiKit.Base.bind("loadPlugins", this));
    d.addCallback(function () {
        self.ui.pluginTable.setSelectedIds(id);
        self._pluginUploadInit();
        self._showPlugin();
    });
    d.addErrback(RapidContext.UI.showError);
    d.addBoth(function () {
        self.ui.overlay.hide();
    });
}

/**
 * Sends a server reset (restart) request.
 */
AdminApp.prototype.resetServer = function (e) {
    var d = RapidContext.App.callProc("System.Reset");
    if (e instanceof MochiKit.Signal.Event) {
        d.addCallback(function (data) { alert("Server reset complete"); });
        d.addErrback(RapidContext.UI.showError);
    }
    return d;
}

/**
 * Loads the procedures for the procedure tree.
 */
AdminApp.prototype.loadProcedures = function () {
    this.proc.procList();
}

/**
 * Callback function for loading the procedure tree.
 *
 * @param {Object} res the result object
 */
AdminApp.prototype._callbackProcedures = function (res) {
    this.ui.procTree.markAll();
    for (var i = 0; i < res.length; i++) {
        var path = res[i].split(".");
        var node = this.ui.procTree.addPath(path);
        node.data = res[i];
    }
    this.ui.procTree.removeAllMarked();
    this._showProcedure();
}

/**
 * Shows the specified procedure in the procedure tree and in the
 * details area.
 *
 * @param {String} name the procedure name
 */
AdminApp.prototype.showProcedure = function (name) {
    var node = this.ui.procTree.findByPath(name.split("."));
    if (node == null) {
        throw new Error("failed to find procedure '" + name + "'");
    } else if (node.isSelected()) {
        this._showProcedure();
    } else {
        node.select();
    }
}

/**
 * Loads detailed procedure information.
 */
AdminApp.prototype._showProcedure = function () {
    var node = this.ui.procTree.selectedChild();
    this.ui.procForm.reset();
    this.ui.procRemove.hide();
    this.ui.procEdit.hide();
    this.ui.procReload.hide();
    this.ui.procLoading.hide();
    MochiKit.DOM.replaceChildNodes(this.ui.procArgTable);
    this.ui.procExec.disable();
    this.ui.procBatch.disable();
    this.ui.procExecResult.removeAll();
    RapidContext.Util.resizeElements(this.ui.procExecResult);
    if (node != null && node.data != null) {
        var args = [node.data];
        var d = RapidContext.App.callProc("System.Procedure.Read", args);
        d.addBoth(MochiKit.Base.bind("_callbackShowProcedure", this));
        this.ui.procLoading.show();
    }
}

/**
 * Callback function for showing detailed procedure information.
 *
 * @param {Object} res the result object or error
 */
AdminApp.prototype._callbackShowProcedure = function (res) {
    this.ui.procReload.show();
    this.ui.procLoading.hide();
    if (res instanceof Error) {
        RapidContext.UI.showError(res);
    } else {
        this._currentProc = res;
        if (res.plugin == "local") {
            this.ui.procRemove.show();
        }
        if (res.type != "built-in") {
            this.ui.procEdit.show();
        }
        this.ui.procForm.update(res);
        MochiKit.DOM.replaceChildNodes(this.ui.procArgTable);
        var count = 0;
        for (var i = 0; i < res.bindings.length; i++) {
            var b = res.bindings[i];
            if (b.type == "argument") {
                var attrs = { "class": "label", style: "padding-top: 4px; padding-right: 6px;" };
                var text = RapidContext.Util.toTitleCase(b.name).replace(" ", "\u00A0") + ":";
                var col1 = MochiKit.DOM.TD(attrs, text);
                var value = this._defaults[b.name] || "";
                var attrs = { name: "arg" + count, value: value, style: "margin-right: 6px;" };
                var field = RapidContext.Widget.TextField(attrs);
                var icon = RapidContext.Widget.Icon({ ref: "EDIT", style: { "verticalAlign": "middle" } });
                icon.onclick = MochiKit.Base.bind("_editProcArg", this, count);
                var col2 = MochiKit.DOM.TD({ style: "padding-right: 6px; white-space: nowrap;" }, field, icon);
                var col3 = MochiKit.DOM.TD({ style: "padding-top: 4px;" }, b.description);
                var tr = MochiKit.DOM.TR({}, col1, col2, col3);
                this.ui.procArgTable.appendChild(tr);
                count++;
            }
        }
        this.ui.procExec.enable();
        this.ui.procBatch.enable();
        this.ui.procExecResult.removeAll();
        RapidContext.Util.resizeElements(this.ui.procExecResult);
    }
}

/**
 * Returns the current procedure arguments array. Each argument in
 * the array is a data object containing a "type" and a "value"
 * property. Both have string values.
 *
 * @return {Array} the procedure arguments
 */
AdminApp.prototype._getProcArgs = function () {
    var args = [];
    var rows = this.ui.procArgTable.childNodes;
    for (var i = 0; i < rows.length; i++) {
        var td = rows[i].childNodes[1];
        var field = td.firstChild;
        var type = field.dataType || "string";
        var value = field.dataValue || field.getValue();
        args.push({ type: type, value: value });
    }
    return args;
}

/**
 * Opens the procedure argument editor for a specific argument.
 *
 * @param {Number} idx the argument index
 */
AdminApp.prototype._editProcArg = function (idx) {
    var args = this._getProcArgs();
    this.ui.procArgForm.update(args[idx]);
    this.ui.procArgForm.argumentIndex = idx;
    this.ui.procArgDialog.show();
}

/**
 * Updates a procedure argument after editing in the dialog.
 */
AdminApp.prototype._updateProcArg = function () {
    var form = this.ui.procArgForm.valueMap();
    var idx = this.ui.procArgForm.argumentIndex;
    var tr = this.ui.procArgTable.childNodes[idx];
    var td = tr.childNodes[1];
    var field = td.firstChild;
    field.dataType = form.type;
    var lines = form.value.split("\n");
    if (form.type === "string" && lines.length <= 1) {
        field.disabled = false;
        field.dataValue = undefined;
        field.setAttrs({ value: form.value });
    } else if (form.type === "json") {
        field.disabled = true;
        field.dataValue = form.value;
        var text = "JSON object";
        try {
            var o = MochiKit.Base.evalJSON(form.value);
            if (o instanceof Array) {
                text = "JSON array, " + o.length + " rows";
            }
        } catch (e) {
            text = "Invalid JSON string";
        }
        field.setAttrs({ value: text });
    } else {
        field.disabled = true;
        field.dataValue = form.value;
        var text = lines.length + " lines, " + form.value.length + " chars";
        field.setAttrs({ value: text });
    }
    this.ui.procArgDialog.hide();
}

/**
 * Opens the procedure editing dialog for a new procedure.
 */
AdminApp.prototype._addProcedure = function () {
    var data = { name: "", type: "javascript", description: "",
                 bindings: {}, defaults: {} };
    var d = this._initProcEdit(data);
    d.addCallback(MochiKit.Base.bind("_updateProcEdit", this));
}

/**
 * Removes the currently shown procedure (if in the local plug-in).
 */
AdminApp.prototype._removeProcedure = function () {
    var p = this._currentProc;
    var msg = "Do you really want to delete the\nprocedure '" + p.name + "'?";
    if (confirm(msg)) {
        this.ui.overlay.setAttrs({ message: "Deleting..." });
        this.proc.procDelete(p.name);
    }
}

/**
 * Opens the procedure editing dialog for an existing procedure.
 */
AdminApp.prototype._editProcedure = function () {
    var p = this._currentProc;
    var data = { name: p.name, type: p.type, description: p.description,
                 bindings: {}, defaults: {} };
    for (var i = 0; i < p.bindings.length; i++) {
        var b = MochiKit.Base.clone(p.bindings[i]);
        if (b.type === "argument") {
            b.value = b.description;
            b.description = "";
        }
        data.bindings[b.name] = b;
    }
    this._initProcEdit(data);
}

/**
 * Initializes the procedure editing dialog.
 */
AdminApp.prototype._initProcEdit = function (data) {
    this.ui.procEditDialog.data = data;
    var select = this.ui.procEditType;
    var d = this.proc.procTypes();
    d.addCallback(function (res) {
        MochiKit.DOM.replaceChildNodes(select);
        var types = MochiKit.Base.keys(res).sort();
        for (var i = 0; i < types.length; i++) {
            var k = types[i];
            select.appendChild(MochiKit.DOM.OPTION({ value: k }, k));
            var values = res[k].bindings
            var keys = MochiKit.Base.map(MochiKit.Base.itemgetter("name"), values);
            data.defaults[k] = RapidContext.Util.dict(keys, values);
        }
    });
    d.addCallback(MochiKit.Base.bind("_renderProcEdit", this));
    d.addCallback(MochiKit.Base.bind("show", this.ui.procEditDialog));
    d.addErrback(RapidContext.UI.showError);
    return d;
}

/**
 * Renders the procedure editing form from the data object stored in
 * the dialog.
 */
AdminApp.prototype._renderProcEdit = function () {
    var data = this.ui.procEditDialog.data;
    RapidContext.Widget.destroyWidget(this.ui.procEditConns.childNodes);
    RapidContext.Widget.destroyWidget(this.ui.procEditData.childNodes);
    RapidContext.Widget.destroyWidget(this.ui.procEditProcs.childNodes);
    RapidContext.Widget.destroyWidget(this.ui.procEditArgs.childNodes);
    var parents = { connection: this.ui.procEditConns,
                    data: this.ui.procEditData,
                    procedure: this.ui.procEditProcs,
                    argument: this.ui.procEditArgs };
    for (var k in data.bindings) {
        var b = data.bindings[k];
        var defaults = data.defaults[data.type][b.name];
        var strong = MochiKit.DOM.STRONG({}, b.name + ": ");
        var icon = (defaults == null) ? RapidContext.Widget.Icon({ ref: "REMOVE" }) : null;
        var style = { "padding-top": icon ? "0px" : "3px", "padding-bottom": "4px" };
        var div = MochiKit.DOM.DIV({ style: style }, strong, icon, b.description);
        var attrs = { name: "binding." + b.name, value: b.value,
                      style: { "width": "100%", "margin-bottom": "8px" } };
        if (b.value.indexOf("\n") < 0) {
            var field = RapidContext.Widget.TextField(attrs);
        } else {
            attrs.rows = Math.min(20, b.value.match(/\n/g).length + 2);
            attrs.wrap = "off";
            var field = RapidContext.Widget.TextArea(attrs);
        }
        var container = MochiKit.DOM.DIV({}, div, field);
        if (icon) {
            var func = function (name) {
                delete data.bindings[name];
                this._updateProcEdit();
            };
            MochiKit.Signal.connect(icon, "onclick", MochiKit.Base.bind(func, this, b.name));
        }
        parents[b.type].appendChild(container);
    }
    for (var k in parents) {
        var node = parents[k];
        if (node.childNodes.length == 0) {
            var style = { "padding-top": "3px" };
            var div = MochiKit.DOM.DIV({ style: style }, "< None >");
            node.appendChild(div);
        }
    }
    var className = (data.type == "javascript") ? "" : "hidden";
    this.ui.procEditProcs.parentNode.className = className;
    var opts = this.ui.procEditAddType.getElementsByTagName("option");
    opts[0].disabled = (data.type != "javascript");
    opts[1].disabled = (data.type != "javascript");
    opts[2].disabled = (data.type != "javascript");
    if (data.type != "javascript") {
        this.ui.procEditAddType.selectedIndex = 3;
    }
    this.ui.procEditForm.update(data);
}

/**
 * Adds a procedure binding from the input control.
 */
AdminApp.prototype._addProcBinding = function () {
    var data = this.ui.procEditDialog.data;
    var type = this.ui.procEditAddType.value;
    var name = MochiKit.Format.strip(this.ui.procEditAddName.getValue());
    if (name == "") {
        RapidContext.UI.showError("Procedure binding name cannot be empty.");
        this.ui.procEditAddName.focus();
    } else if (data.bindings[name] != null) {
        RapidContext.UI.showError("Procedure binding name already exists.");
        this.ui.procEditAddName.focus();
    } else {
        var b = { name: name, type: type, description: "" };
        b.value = (type == "data") ? "\n" : "";
        data.bindings[name] = b;
        this.ui.procEditAddName.setAttrs({ value: "" });
        this._updateProcEdit();
    }
}

/**
 * Updates the procedure editing data from the form values. This
 * method will automatically handle removed bindings, added bindings
 * or modification of the procedure type. It will also trigger
 * re-rendering of the form.
 */
AdminApp.prototype._updateProcEdit = function () {
    var data = this.ui.procEditDialog.data;
    var values = this.ui.procEditForm.valueMap();
    data.name = values.name;
    data.type = values.type;
    data.description = values.description;
    for (var k in values) {
        if (k.indexOf("binding.") == 0) {
            var name = k.substring(8);
            if (data.bindings[name]) {
                data.bindings[name].value = values[k];
            }
        }
    }
    var defaults = data.defaults[data.type];
    for (var k in defaults) {
        if (data.bindings[k] == null) {
            var b = MochiKit.Base.clone(defaults[k]);
            b.value = (b.type == "data") ? "\n" : "";
            data.bindings[k] = b;
        }
    }
    for (var k in data.bindings) {
        var b = data.bindings[k];
        var v = MochiKit.Format.strip(b.value);
        if (defaults[k] == null && v == "" && b.description != "") {
            delete data.bindings[k];
        }
    }
    this._renderProcEdit();
}

/**
 * Saves the procedure from the procedure edit dialog.
 */
AdminApp.prototype._saveProcedure = function () {
    var data = this.ui.procEditDialog.data;
    this._updateProcEdit();
    var bindings = [];
    for (var k in data.bindings) {
        var b = data.bindings[k];
        var res = { name: b.name, type: b.type };
        if (b.type == "argument") {
            res.description = b.value;
        } else {
            res.value = b.value;
        }
        bindings.push(res);
    }
    var args = [data.name, data.type, data.description, bindings];
    var d = RapidContext.App.callProc("System.Procedure.Write", args);
    d.addCallback(MochiKit.Base.bind("hide", this.ui.procEditDialog));
    d.addCallback(MochiKit.Base.bind("recall", this.proc.procList));
    d.addCallback(MochiKit.Base.bind("showProcedure", this, data.name));
    d.addErrback(RapidContext.UI.showError);
}

/**
 * Executes a procedure call for the current procedure.
 */
AdminApp.prototype._executeProcedure = function () {
    var proc = this._currentProc;
    var args = [];
    var values = this._getProcArgs();
    for (var i = 0; i < proc.bindings.length; i++) {
        var b = proc.bindings[i];
        if (b.type == "argument") {
            var value = values[args.length].value;
            if (values[args.length].type === "json") {
                try {
                    value = MochiKit.Base.evalJSON(value);
                } catch (e) {
                    RapidContext.UI.showError("Invalid JSON: " + value);
                    return;
                }
            } else if (value.length < 50) {
                this._defaults[b.name] = value;
            }
            args.push(value);
        }
    }
    this.ui.procExecLoading.show();
    this.ui.procExec.disable();
    this.ui.procBatch.disable();
    this.ui.procExecResult.removeAll();
    var d = RapidContext.App.callProc(proc.name, args);
    d.addBoth(MochiKit.Base.bind("_callbackExecute", this));
}

/**
 * Callback function for procedure execution.
 *
 * @param {Object} res the response data or error
 */
AdminApp.prototype._callbackExecute = function (res) {
    this.ui.procExecLoading.hide();
    this.ui.procExec.enable();
    this.ui.procBatch.enable();
    if (res instanceof Error) {
        RapidContext.UI.showError(res);
    } else {
        this._showExecData(this.ui.procExecResult, res);
    }
}

/**
 * Expands a procedure result tree node by adding child data nodes.
 * The node data can be retrieved from the node or by using a
 * specified value.
 *
 * @param {Node} node the tree node (or tree) widget
 * @param {Object} [data] the data object to add
 */
AdminApp.prototype._showExecData = function (node, data) {
    function typeName(value) {
        if (typeof(value) == "object") {
            if (value == null) {
                return "null";
            } else if (value instanceof Number) {
                return "Number";
            } else if (value instanceof Boolean) {
                return "Boolean";
            } else if (value instanceof String) {
                return "String";
            } else if (value instanceof Array) {
                return "Array";
            } else if (value instanceof Date) {
                return "Date";
            } else if (value instanceof RegExp) {
                return "RegExp";
            } else {
                return "Object";
            }
        } else {
            return typeof(value);
        }
    }
    function dataLabel(type, value, truncate) {
        if (value == null) {
            return "<null>";
        } else if (type === "Array") {
            return "[Array, length: " + value.length + "]";
        } else if (type !== "Object") {
            try {
                var str = "[" + type + "] " + value.toString();
                return truncate ? MochiKit.Text.truncate(str, 50, "...") : str;
            } catch (e) {
                return "[" + type + "]";
            }
        } else {
            try {
                if (value.hasOwnProperty("toString")) {
                    var str = value.toString();
                    return truncate ? MochiKit.Text.truncate(str, 50, "...") : str;
                }
            } catch (ignore) {
                // Fallback to key enumeration
            }
            var keys = MochiKit.Base.keys(value);
            return "[Object, " + keys.length + " properties]";
        }
    }
    data = (typeof(data) !== "undefined") ? data : node.data;
    var type = typeName(data);
    if (node.getChildNodes().length > 0) {
        // Do nothing
    } else if (type === "Array" || type === "Object") {
        for (var k in data) {
            var v = data[k];
            if (typeof(v) === "undefined") {
                v = data[parseInt(k)];
            }
            var vt = typeName(v);
            var attrs = { name: k + ": " + dataLabel(vt, v, true) };
            if (vt === "Array" || vt === "Object") {
                attrs.folder = true;
            } else if (v != null) {
                attrs.tooltip = dataLabel(vt, v, false);
            }
            var child = RapidContext.Widget.TreeNode(attrs);
            if (vt === "Array" || vt === "Object") {
                child.data = v;
            }
            node.addAll(child);
        }
    } else {
        var attrs = { name: dataLabel(type, data, true),
                      tooltip: dataLabel(type, data, false) };
        node.addAll(RapidContext.Widget.TreeNode(attrs));
    }
}

/**
 * Creates a new set of batch calls for the current procedure.
 */
AdminApp.prototype._createBatch = function () {
    var proc = this._currentProc;
    var args = [];
    var count = null;
    var values = this._getProcArgs();
    for (var i = 0; i < proc.bindings.length; i++) {
        var b = proc.bindings[i];
        if (b.type == "argument") {
            var value = values[args.length].value;
            if (values[args.length].type === "json") {
                try {
                    value = MochiKit.Base.evalJSON(value);
                } catch (e) {
                    RapidContext.UI.showError("Invalid JSON: " + value);
                    return;
                }
            } else {
                value = MochiKit.Base.map(MochiKit.Format.strip, value.split("\n"));
                if (value.length <= 1) {
                    value = value[0];
                }
            }
            if (value instanceof Array) {
                if (count == null) {
                    count = value.length;
                } else if (value.length != count) {
                    var msg = "Mismatching line count for field " + name +
                              ": expected " + count + ", but found " +
                              value.length;
                    RapidContext.UI.showError(msg);
                    return;
                }
            }
            args.push(value);
        }
    }
    if (count == null) {
        RapidContext.UI.showError("Multiple argument values required for batch creation");
        return;
    }
    for (var i = 0; i < count; i++) {
        var callArgs = [];
        for (var j = 0; j < args.length; j++) {
            var value = args[j];
            if (value instanceof Array) {
                callArgs.push(value[i]);
            } else {
                callArgs.push(value);
            }
        }
        this._batch.queue.push({ proc: proc.name, args: callArgs });
    }
    this.ui.tabContainer.selectChild(this.ui.batchTab);
    this._startBatch();
}

/**
 * Configure the batch delay.
 */
AdminApp.prototype._configBatchDelay = function () {
    var value;

    value = prompt("Enter order processing delay (in seconds):",
                   "" + this._batch.delay);
    if (value != null) {
        value = parseInt(value, 10);
        if (!isNaN(value)) {
            this._batch.delay = Math.max(value, 0);
        }
    }
}

/**
 * Starts the batch processing if not already running.
 */
AdminApp.prototype._startBatch = function () {
    if (!this._batch.running) {
        this._batch.running = true;
        this._batch.stat.success = 0;
        this._batch.stat.failed = 0;
        this.ui.batchProgress.setAttrs({ min: 0, max: this._batch.queue.length, text: null });
        MochiKit.DOM.replaceChildNodes(this.ui.batchResume, "Pause");
        this.ui.batchLoading.show();
        this._processBatch();
    }
}

/**
 * Stops the batch processing if running.
 */
AdminApp.prototype._stopBatch = function () {
    this._batch.running = false;
    MochiKit.DOM.replaceChildNodes(this.ui.batchResume, "Resume");
    this.ui.batchLoading.hide();
}

/**
 * Toggles the batch processing.
 */
AdminApp.prototype._toggleBatch = function () {
    if (this._batch.running) {
        this._stopBatch();
    } else {
        this._startBatch();
    }
}

/**
 * Stops the batch processing and clears the queue.
 */
AdminApp.prototype._clearBatch = function () {
    this._stopBatch();
    this._batch.queue = [];
    this._batch.stat.success = 0;
    this._batch.stat.failed = 0;
    this.ui.batchForm.reset();
    this.ui.batchProgress.setAttrs({ min: 0, max: 0, text: "Stopped" });
}

/**
 * Processes the first item in the batch queue.
 */
AdminApp.prototype._processBatch = function () {
    var item;

    if (this._batch.running) {
        item = this._batch.queue.shift();
        if (item == null) {
            this._stopBatch();
        } else {
            var d = RapidContext.App.callProc(item.proc, item.args);
            d.addBoth(MochiKit.Base.bind("_callbackBatch", this));
        }
    }
}

/**
 * Callback function for batch processing.
 *
 * @param {Object} res the call result data or error
 */
AdminApp.prototype._callbackBatch = function (res) {
    var delay;
    if (res instanceof Error) {
        this._batch.stat.failed++;
    } else {
        this._batch.stat.success++;
    }
    this.ui.batchForm.update(this._batch.stat);
    var done = this._batch.stat.success + this._batch.stat.failed;
    var total = this._batch.queue.length + done;
    if (this.ui.batchProgress.max != total) {
        this.ui.batchProgress.setAttrs({ min: 0, max: total });
    }
    this.ui.batchProgress.setAttrs({ value: done });
    if (this._batch.queue.length > 0) {
        setTimeout(this._batch.func, this._batch.delay * 1000);
    } else {
        this._stopBatch();
    }
}

/**
 * Loads the list of users.
 */
AdminApp.prototype.loadUsers = function () {
    this.proc.userList();
}

/**
 * Modifies the user form for adding a new user.
 */
AdminApp.prototype._addUser = function () {
    this.ui.userForm.reset();
    this.ui.userForm.update({ enabled: true,
                              passwordHint: "Minimum 5 characters" });
    this.ui.userId.enable();
}

/**
 * Modifies the user form for editing an existing user.
 */
AdminApp.prototype._editUser = function () {
    var data = this.ui.userTable.getSelectedData();
    this.ui.userForm.reset();
    var extra = { roles: (data.role) ? data.role.join(" ") : "",
                  passwordHint: "Leave blank for unmodified" };
    this.ui.userForm.update(MochiKit.Base.update(extra, data));
    this.ui.userId.disable();
}

/**
 * Saves the user modification form.
 */
AdminApp.prototype._saveUser = function () {
    var data = this.ui.userForm.valueMap();
    if (this.ui.userForm.validate()) {
        this.proc.userChange(data.id,
                             data.name,
                             data.email,
                             data.description,
                             data.enabled ? "1" : "0",
                             data.password,
                             data.roles);
    }
}

/**
 * Sets a new log level.
 */
AdminApp.prototype.setLogLevel = function (level) {
    this.ui.logForm.update({ level: level });
    LOG.enabled[LOG.TRACE] = (level == LOG.TRACE);
    LOG.enabled[LOG.INFO] = (level == LOG.TRACE || level == LOG.INFO);
    LOG.enabled[LOG.WARNING] = (level != LOG.ERROR);
    LOG.enabled[LOG.ERROR] = true;
}

/**
 * Clears the log.
 */
AdminApp.prototype._clearLogs = function () {
    LOG.clear();
    this.ui.logTable.setData([]);
    MochiKit.DOM.replaceChildNodes(this.ui.logStack);
    MochiKit.DOM.replaceChildNodes(this.ui.logData);
}

/**
 * Refreshes the log levels and table.
 */
AdminApp.prototype._showLogs = function () {
    if (LOG.enabled[LOG.TRACE]) {
        this.ui.logForm.update({ level: LOG.TRACE });
    } else if (LOG.enabled[LOG.INFO]) {
        this.ui.logForm.update({ level: LOG.INFO });
    } else if (LOG.enabled[LOG.WARNING]) {
        this.ui.logForm.update({ level: LOG.WARNING });
    } else {
        this.ui.logForm.update({ level: LOG.ERROR });
    }
    this.ui.logTable.setData(LOG.entries.slice(0));
}

/**
 * Show log details for the selected log data.
 */
AdminApp.prototype._showLogDetails = function () {
    var data = this.ui.logTable.getSelectedData();
    var text;
    if (data == null || data.stackTrace == null) {
        text = "<no stack trace>";
    } else {
        text = "";
        for (var i = 0; i < data.stackTrace.length; i++) {
            if (data.stackTrace[i] != null) {
                text += data.stackTrace[i] + "\n";
            } else {
                text += "<anonymous>\n";
            }
        }
    }
    MochiKit.DOM.replaceChildNodes(this.ui.logStack, text);
    if (data == null || data.data == null) {
        text = "<no additional data>";
    } else if (typeof data.data == "string" || data.data instanceof String) {
        // TODO: Replace this string splitting with something more dynamic
        var list = data.data.split("\n");
        for (var i = 0; i < list.length; i++) {
            var str = list[i];
            var res = "";
            while (str.length > 1000) {
                var pos = str.lastIndexOf(" ", 1000);
                pos = (pos < 500) ? 1000 : pos + 1;
                res += str.substring(0, pos) + "\n    ";
                str = str.substring(pos);
            }
            list[i] = res + str;
        }
        text = MochiKit.DOM.PRE(null, list.join("\n"));
    } else {
        text = MochiKit.Base.serializeJSON(data.data);
    }
    MochiKit.DOM.replaceChildNodes(this.ui.logData, text);
}

/**
 * Stores an object to the RapidContext storage. Note that only
 * read-write storages can be used for storage (typically the local
 * plug-in). If the storage path does not point to a single mounted
 * storage, the first writable overlay storage will be used (always
 * the local plug-in).
 *
 * @param {String} path the storage path
 * @param {Object} data the data object to store (JSON data)
 *
 * @return {Deferred} a MochiKit.Async.Deferred object that will
 *         callback when the data has been stored
 */
AdminApp.storageWrite = function (path, data) {
    if (MochiKit.Text.startsWith("/", path)) {
        path = path.substring(1);
    }
    var options = { method: "POST", timeout: 60, headers: {} };
    options.headers["Content-Type"] = "application/json";
    options.sendContent = MochiKit.Base.serializeJSON(data);
    var d = RapidContext.App.loadXHR("rapidcontext/storage/" + path, null, options);
    d.addCallback(function (xhr) {
        if (xhr.status == 200 || xhr.status == 201) {
            return path;
        } else {
            throw new MochiKit.Async.XMLHttpRequestError(xhr, "unrecognized response code");
        }
    });
    d.addErrback(function (err) {
        if (err instanceof MochiKit.Async.XMLHttpRequestError) {
            var xhr = err.req;
            var msg = xhr.responseText || xhr.statusText || err.message;
            msg = msg.replace(/^HTTP \d+\s/i, "");
            msg = msg.replace(/^.*:\s*([\s\S]+)/, "$1");
            err.message = msg;
            return err;
        } else {
            return err;
        }
    });
    return d;
}

/**
 * Removes an object from the RapidContext storage. Note that only
 * objects in read-write storages can be removed (typically the local
 * plug-in).
 *
 * @param {String} path the storage path
 *
 * @return {Deferred} a MochiKit.Async.Deferred object that will
 *         callback when the data has been removed
 */
AdminApp.storageDelete = function (path) {
    if (MochiKit.Text.startsWith("/", path)) {
        path = path.substring(1);
    }
    var options = { method: "DELETE", timeout: 60 };
    var d = RapidContext.App.loadXHR("rapidcontext/storage/" + path, null, options);
    d.addCallback(function (res) {
        if (res.status == 204) {
            return null;
        } else {
            throw new Error(res.statusText.substr(4));
        }
    });
    return d;
}

/**
 * Splits a text string into multiple lines. Existing line breaks
 * will be preserved (recognizing "\n", "\r" or "\r\n"), but
 * additional line breaks may be added by specifying a maximum line
 * length. If the keepspace flag is set, the newlines and space
 * characters at the end of each line will be preserved.
 *
 * @param {String} str the string to split
 * @param {Number} [maxlen] the maximum line length, or zero for
 *            unlimited (defaults to 0)
 * @param {Boolean} [keepspace] the boolean flag to keep
 *            trailing whitespace (defaults to false)
 *
 * @return {Array} an array of string for each line
 *
 * @example
 *
 */
AdminApp.splitLines = function (str, maxlen, keepspace) {
    var lines = [];
    str = str || "";
    maxlen = maxlen || 0;
    function splitPush(str) {
        if (!keepspace) {
            str = str.replace(/\s+$/, "");
        }
        while (maxlen > 0 && str.length > maxlen) {
            var line = str.substring(0, maxlen);
            var m = /^\s+/.exec(str.substring(maxlen));
            if (!keepspace && m) {
                line += m[0];
            } else {
                m = /[^\s\).:,;=-]*$/.exec(line);
                if (m && m.index > 0) {
                    line = line.substring(0, m.index);
                }
            }
            str = str.substring(line.length);
            if (!keepspace) {
                line = line.replace(/\s+$/, "");
            }
            lines.push(line);
        }
        if (str.length > 0 || line == null) {
            lines.push(str);
        }
    }
    var re = /\n|\r\n|\r/g;
    var m;
    var pos = 0;
    while ((m = re.exec(str)) != null) {
        splitPush(str.substring(pos, re.lastIndex));
        pos = re.lastIndex;
    }
    splitPush(str.substring(pos));
    return lines;
}
