/**
 * Creates a new admin app.
 */
function AdminApp() {
    this._defaults = { operatorId: RapidContext.App.user().id };
    this._types = {};
    this._cxnIds = null;
    this._cxnCount = 0;
    this._currentProc = null;
    this._batch = {
        running: false,
        delay: 5,
        queue: [],
        stat: { success: 0, failed: 0 },
        func: () => this._processBatch()
    };
}

/**
 * Starts the app and initializes the UI.
 */
AdminApp.prototype.start = function () {
    // Create procedure callers
    this.proc = RapidContext.Procedure.mapAll({
        typeList: "system/type/list",
        cxnList: "system/connection/list",
        appList: "system/app/list",
        plugInList: "system/plugin/list",
        procList: "system/procedure/list",
        procRead: "system/procedure/read",
        procDelete: "system/procedure/delete",
        procTypes: "system/procedure/types",
        userList: "system/user/list",
        userChange: "system/user/change"
    });

    // All views
    MochiKit.Signal.connect(this.ui.root, "onenter", () => this.ui.tabContainer.selectChild(null));
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
    MochiKit.Signal.connect(this.ui.cxnEditForm, "onclick", this, "_addRemoveConnectionProps");
    MochiKit.Signal.connect(this.ui.cxnEditForm, "onsubmit", this, "_storeConnection");
    var statusRenderer = function (td, value, data) {
        let usedAt = data && data._lastUsedTime || "@0";
        let since = Date.now() - parseInt(usedAt.substr(1), 10);
        let cls = "fa fa-check";
        if (data._error || data._lastError) {
            cls = "fa fa-exclamation-circle widget-red";
        } else if (!data._openChannels && since > 5 * 60 * 1000) {
            cls = "fa fa-exclamation-triangle widget-yellow";
        }
        td.append(RapidContext.Widget.Icon({ "class": cls }));
    };
    var typeRenderer = function (td, value, data) {
        if (/^connection\//.test(value)) {
            td.append(value.substr(11));
        }
    };
    this.ui.cxnTable.getChildNodes()[1].setAttrs({ renderer: statusRenderer });
    this.ui.cxnTable.getChildNodes()[2].setAttrs({ renderer: typeRenderer });

    // App view
    MochiKit.Signal.connectOnce(this.ui.appTab, "onenter", this, "loadApps");
    RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
    MochiKit.Signal.connect(this.proc.appList, "onsuccess", this, "_callbackLoadApps");
    MochiKit.Signal.connect(this.ui.appTable, "onselect", this, "_showApp");
    MochiKit.Signal.connect(this.ui.appLaunch, "onclick", this, "_launchApp");
    MochiKit.Signal.connect(this.ui.appLaunchWindow, "onclick", this, "_launchAppWindow");
    var urlRenderer = function (td, value, data) {
        if (value) {
            var ico = RapidContext.Widget.Icon({ "class": "fa fa-external-link-square ml-1" });
            var link = MochiKit.DOM.A({ href: value, target: "_blank" }, value, ico);
            td.append(link);
        }
    };
    this.ui.appResourceTable.getChildNodes()[1].setAttrs({ renderer: urlRenderer });

    // Plug-in view
    MochiKit.Signal.connectOnce(this.ui.pluginTab, "onenter", this, "loadPlugins");
    MochiKit.Signal.connect(this.ui.pluginFile, "onchange", this, "_pluginUpload");
    MochiKit.Signal.connect(this.ui.pluginReset, "onclick", this, "resetServer");
    RapidContext.UI.connectProc(this.proc.plugInList, this.ui.pluginLoading, this.ui.pluginReload);
    MochiKit.Signal.connect(this.proc.plugInList, "onsuccess", this.ui.pluginTable, "setData");
    MochiKit.Signal.connect(this.proc.plugInList, "onsuccess", this, "_showPlugin");
    MochiKit.Signal.connect(this.ui.pluginTable, "onselect", this, "_showPlugin");
    MochiKit.Signal.connect(this.ui.pluginLoad, "onclick", this, "_togglePlugin");
    MochiKit.Signal.connect(this.ui.pluginUnload, "onclick", this, "_togglePlugin");

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
    MochiKit.Signal.connect(this.ui.procEditForm, "onclick", this, "_addRemoveProcBinding");
    MochiKit.Signal.connect(this.ui.procEditForm, "onsubmit", this, "_saveProcedure");
    MochiKit.Signal.connect(this.ui.procReload, "onclick", this, "_showProcedure");
    MochiKit.Signal.connect(this.ui.procExec, "onclick", this, "_executeProcedure");
    MochiKit.Signal.connect(this.ui.procBatch, "onclick", this, "_createBatch");
    MochiKit.Signal.connect(this.ui.procExecResult, "onexpand", this, "_showExecDataExpand");
    MochiKit.Signal.connect(this.ui.procArgForm, "onsubmit", this, "_updateProcArg");
    this.ui.procExecLoading.hide();
    this.ui.procExecResult.resizeContent = function () {
        var pos = MochiKit.Style.getElementPosition(this, this.parentNode);
        var dim = MochiKit.Style.getElementDimensions(this.parentNode);
        MochiKit.Style.setElementDimensions(this, { w: dim.w - 2, h: dim.h - pos.y - 2 });
    };

    // Batch view
    MochiKit.Signal.connect(this.ui.batchDelete, "onclick", this, "_clearBatch");
    MochiKit.Signal.connect(this.ui.batchDelay, "onclick", this, "_configBatchDelay");
    MochiKit.Signal.connect(this.ui.batchPlay, "onclick", this, "_toggleBatch");
    MochiKit.Signal.connect(this.ui.batchPause, "onclick", this, "_toggleBatch");

    // User view
    MochiKit.Signal.connectOnce(this.ui.userTab, "onenter", this, "loadUsers");
    RapidContext.UI.connectProc(this.proc.userList, this.ui.userLoading, this.ui.userReload);
    MochiKit.Signal.connect(this.proc.userList, "onsuccess", this.ui.userTable, "setData");
    MochiKit.Signal.connect(this.ui.userTable, "onselect", this, "_editUser");
    MochiKit.Signal.connect(this.ui.userAdd, "onclick", this, "_addUser");
    MochiKit.Signal.connect(this.ui.userForm, "onsubmit", this, "_saveUser");
    RapidContext.UI.connectProc(this.proc.userChange);
    MochiKit.Signal.connect(this.proc.userChange, "onsuccess", this.proc.userList, "recall");

    // Log view
    MochiKit.Signal.connect(this.ui.logTab, "onenter", this, "_showLogs");
    MochiKit.Signal.connect(this.ui.logForm, "oninput", this, "_updateLogLevel");
    MochiKit.Signal.connect(this.ui.logClear, "onclick", this, "_clearLogs");
    MochiKit.Signal.connect(this.ui.logReload, "onclick", this, "_showLogs");
    MochiKit.Signal.connect(this.ui.logTable, "onselect", this, "_showLogDetails");
    this._updateLogLevel(false);

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
};

/**
 * Stops the app.
 */
AdminApp.prototype.stop = function () {
    for (var name in this.proc) {
        MochiKit.Signal.disconnectAll(this.proc[name]);
    }
};

/**
 * Updates the type cache.
 */
AdminApp.prototype._updateTypeCache = function (res) {
    var type;
    this._types = {};
    for (var i = 0; i < res.length; i++) {
        type = res[i];
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
        type = this._types[id];
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
};

/**
 * Validates all connections. The connection list is updated before
 * the validation starts.
 */
AdminApp.prototype._validateConnections = function () {
    function validate(con) {
        return RapidContext.App.callProc("system/connection/validate", [con.id]);
    }
    this.ui.overlay.setAttrs({ message: "Validating..." });
    this.ui.overlay.show();
    this.proc.cxnList()
        .then(function (data) {
            return Promise.allSettled(data.map(validate));
        })
        .catch(function () {})
        .then(() => this.proc.cxnList.recall())
        .finally(() => this.ui.overlay.hide());
};

/**
 * Shows the specified connection in the Admin UI. This method will
 * show the connection pane (if not already displaying), validate
 * the connection specified, update the connection list and finally
 * display the connection details.
 *
 * @param {string} id the connection identifier
 */
AdminApp.prototype.showConnection = function (id) {
    this.ui.tabContainer.selectChild(this.ui.cxnTab);
    RapidContext.App.callProc("system/connection/validate", [id])
        .catch(function () {})
        .then(() => this.proc.cxnList.recall())
        .then(() => this.ui.cxnTable.setSelectedIds(id));
};

/**
 * Shows detailed connection information.
 */
AdminApp.prototype._showConnection = function () {
    let data = this.ui.cxnTable.getSelectedData();
    data = Object.assign({}, data);
    if (/^@\d+$/.test(data._lastUsedTime)) {
        let dttm = new Date(+data._lastUsedTime.substr(1));
        data.lastAccess = MochiKit.DateTime.toISOTimestamp(dttm);
    }
    this.ui.cxnForm.reset();
    this.ui.cxnForm.update(data);
    this.ui.cxnRemove.setAttrs({ hidden: data.plugin != "local" });
    if (data.plugin && data.id) {
        this.ui.cxnEdit.show();
        let url = "rapidcontext/storage/connection/" + data.id;
        this.ui.cxnLink.setAttribute("href", url);
        this.ui.cxnLink.classList.remove("hidden");
    } else {
        this.ui.cxnEdit.hide();
        this.ui.cxnLink.classList.add("hidden");
    }
    let clones = this.ui.cxnTemplate.parentNode.querySelectorAll(".clone");
    RapidContext.Widget.destroyWidget(clones);
    let props = ["id", "type", "plugin", "description", "lastAccess", "maxOpen"];
    for (let k in data) {
        let v = data[k];
        let hidden = k.startsWith("_") || props.includes(k);
        if (!hidden || (/error$/i.test(k) && v)) {
            let title = RapidContext.Util.toTitleCase(k);
            if (v == null) {
                v = "";
            }
            if (/error$/i.test(k)) {
                v = MochiKit.DOM.SPAN({ "class": "important" }, v);
            } else if (/password$/i.test(k)) {
                v = RapidContext.Widget.Field({ name: k, value: v, mask: true });
            }
            let tr = this.ui.cxnTemplate.cloneNode(true);
            tr.className = "clone";
            tr.firstChild.append(title + ":");
            tr.lastChild.append(v);
            this.ui.cxnTemplate.before(tr);
        }
    }
};

/**
 * Opens the connection editing dialog for a new connection.
 */
AdminApp.prototype._addConnection = function () {
    this._initConnectionEdit({});
};

/**
 * Removes a connection (after user confirmation).
 */
AdminApp.prototype._removeConnection = function () {
    let data = this.ui.cxnTable.getSelectedData();
    RapidContext.UI.Msg.warning.remove("connection", data.id)
        .then(() => {
            var path = RapidContext.Storage.path(data);
            RapidContext.App.callProc("system/storage/delete", [path])
                .then(() => this.proc.cxnList.recall())
                .catch(RapidContext.UI.showError);
        })
        .catch(() => true);
};

/**
 * Opens the connection editing dialog for an existing connection.
 */
AdminApp.prototype._editConnection = function () {
    var data = this.ui.cxnTable.getSelectedData();
    this._initConnectionEdit(data);
};

/**
 * Initializes the connection editing dialog.
 */
AdminApp.prototype._initConnectionEdit = function (data) {
    this.ui.cxnEditDialog.data = data;
    this.ui.cxnEditForm.reset();
    this.ui.cxnEditForm.update(data);
    var self = this;
    this.proc.typeList()
        .then(function () {
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
                    select.append(MochiKit.DOM.OPTION(attrs, k));
                }
            }
        })
        .then(() => this._updateConnectionEdit())
        .then(() => this.ui.cxnEditDialog.show())
        .catch(RapidContext.UI.showError);
};

/**
 * Updates the connection editing dialog.
 */
AdminApp.prototype._updateConnectionEdit = function () {
    function buildRow(tr, p) {
        var title = p.title || RapidContext.Util.toTitleCase(p.name);
        var value = (data[p.name] != null) ? "" + data[p.name] : "";
        var defaultValue = (data["_" + p.name] != null) ? "" + data["_" + p.name] : "";
        var valueLines = AdminApp.splitLines(value, 58);
        tr.className = "clone";
        tr.firstChild.append(title + ":");
        var attrs = { name: p.name, size: 60 };
        if (p.required && p.format != "password") {
            attrs.required = true;
        }
        if (defaultValue) {
            attrs.placeholder = defaultValue;
        }
        var input = null;
        if (p.format == "text" || valueLines.length > 1) {
            attrs.cols = 58;
            attrs.rows = Math.min(Math.max(2, valueLines.length), 20);
            input = RapidContext.Widget.TextArea(attrs);
        } else if (p.format == "password") {
            attrs.type = "password";
            input = RapidContext.Widget.TextField(attrs);
        } else {
            input = RapidContext.Widget.TextField(attrs);
        }
        tr.lastChild.append(input);
        if (p.custom) {
            input.size = 55;
            var btn = { icon: "fa fa-lg fa-minus", "class": "font-smaller ml-1", "data-action": "remove" };
            tr.lastChild.append(RapidContext.Widget.Button(btn));
        }
        if (p.required && p.format != "password") {
            var validator = RapidContext.Widget.FormValidator({ name: p.name });
            tr.lastChild.append(validator);
        }
        if (p.description) {
            var help = MochiKit.DOM.DIV({ "class": "helptext text-pre-wrap" }, p.description);
            tr.lastChild.append(help);
        }
        if (!showAll && !p.required && !p.custom && !value) {
            tr.classList.add("hidden");
        }
        return tr;
    }
    var data = this.ui.cxnEditForm.valueMap();
    var showAll = (data._showAll == "yes");
    this.ui.cxnEditForm.reset();
    MochiKit.Base.setdefault(data, this.ui.cxnEditDialog.data);
    var clones = this.ui.cxnEditTemplate.parentNode.querySelectorAll(".clone");
    RapidContext.Widget.destroyWidget(clones);
    var hiddenProps = { id: true, type: true, plugin: true };
    var props = {};
    if (this._types[data.type]) {
        var type = this._types[data.type];
        Object.assign(props, type.properties);
        this.ui.cxnEditTypeDescr.innerText = type.description;
    } else {
        this.ui.cxnEditTypeDescr.innerText = "";
    }
    for (var name in data) {
        var val = String(data[name]).trim();
        if (!name.startsWith("_") && !(name in props) && !(name in hiddenProps) && val) {
            props[name] = {
                name: name,
                title: name,
                custom: true,
                description: "User-specified parameter."
            };
            if (/password$/i.test(name)) {
                props[name].format = "password";
            }
        }
    }
    for (var k in props) {
        var tr = buildRow(this.ui.cxnEditTemplate.cloneNode(true), props[k]);
        this.ui.cxnEditTemplate.before(tr);
    }
    this.ui.cxnEditForm.update(data);
    this.ui.cxnEditDialog.moveToCenter();
};

/**
 * Handles addition and removal of custom connection parameters.
 */
AdminApp.prototype._addRemoveConnectionProps = function (evt) {
    var $el = $(evt.target()).closest("[data-action]");
    if ($el.data("action") === "add") {
        var name = this.ui.cxnEditAddParam.value.trim();
        if (/^[a-z0-9_-]+$/i.test(name)) {
            this.ui.cxnEditAddParam.setAttrs({ name: name, value: "value" });
            this._updateConnectionEdit();
            this.ui.cxnEditAddParam.setAttrs({ name: "_add", value: "" });
        } else {
            RapidContext.UI.Msg.error("Invalid parameter name.");
        }
    } else if ($el.data("action") === "remove") {
        RapidContext.Widget.destroyWidget($el.closest("tr").get(0));
    }
};

/**
 * Stores the edited or new connection to the RapidContext storage.
 */
AdminApp.prototype._storeConnection = function () {
    this.ui.cxnEditForm.validateReset();
    if (this.ui.cxnEditForm.validate()) {
        var prev = this.ui.cxnEditDialog.data;
        var data = this.ui.cxnEditForm.valueMap();
        for (var k in data) {
            var v = data[k].trim();
            if (v && !k.startsWith("_")) {
                // Store updated value
            } else if (prev.id && !k.startsWith("_") && !k.startsWith(".")) {
                data[k] = null; // Remove old value
            } else {
                delete data[k]; // Omit or keep unmodified
            }
        }
        var oldPath = prev.id ? RapidContext.Storage.path(prev) : null;
        var newPath = RapidContext.Storage.path(data);
        var opts = { path: newPath + ".yaml" };
        if (oldPath && oldPath !== newPath) {
            opts = { path: oldPath, updateTo: opts.path };
        } else if (oldPath) {
            opts.update = true;
        }
        RapidContext.App.callProc("system/storage/write", [opts, data])
            .then(() => this.ui.cxnEditDialog.hide())
            .then(() => this.showConnection(data.id))
            .catch(RapidContext.UI.showError);
    }
};

/**
 * Loads the app list.
 */
AdminApp.prototype.loadApps = function () {
    this.proc.appList();
};

/**
 * Callback for the app list loading.
 */
AdminApp.prototype._callbackLoadApps = function () {
    // Procedure call triggered an update of the cached app list,
    // so the procedure results can be ignored here
    this.ui.appTable.setData(RapidContext.App.apps());
    this._showApp();
};

/**
 * Shows detailed app information.
 */
AdminApp.prototype._showApp = function () {
    var data = this.ui.appTable.getSelectedData();
    this.ui.appForm.reset();
    if (data) {
        this.ui.appForm.update(data);
        this.ui.appIcon.innerHTML = "";
        if (data.icon) {
            this.ui.appIcon.append(data.icon.cloneNode(true));
        }
        var url = "rapidcontext/storage/app/" + data.id;
        this.ui.appLink.setAttribute("href", url);
        this.ui.appLink.classList.remove("hidden");
        this.ui.appResourceTable.show();
        this.ui.appResourceTable.setData(data.resources);
    } else {
        this.ui.appIcon.innerHTML = "";
        this.ui.appLink.classList.add("hidden");
        this.ui.appResourceTable.hide();
    }
    this.ui.appLaunch.setAttrs({ disabled: !data });
    this.ui.appLaunchWindow.setAttrs({ disabled: !data });
};

/**
 * Launches the currently selected app.
 */
AdminApp.prototype._launchApp = function () {
    var data = this.ui.appTable.getSelectedData();
    RapidContext.App.startApp(data.id);
};

/**
 * Launches the currently selected app in separate window.
 */
AdminApp.prototype._launchAppWindow = function () {
    var data = this.ui.appTable.getSelectedData();
    RapidContext.App.startApp(data.id, window.open());
};

/**
 * Loads the table of available plug-ins.
 */
AdminApp.prototype.loadPlugins = function () {
    return this.proc.plugInList();
};

/**
 * Shows the details for the currently selected plug-in.
 */
AdminApp.prototype._showPlugin = function () {
    var data = this.ui.pluginTable.getSelectedData();
    this.ui.pluginForm.reset();
    if (data) {
        this.ui.pluginForm.update(data);
        this.ui.pluginLink.classList.remove("hidden");
        var path = "/storage/plugin/" + data.id + "/";
        var url = "rapidcontext/storage" + path;
        this.ui.pluginLink.setAttribute("href", url);
        if (data._loaded) {
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
        this.ui.pluginLink.classList.add("hidden");
        this.ui.pluginLoad.hide();
        this.ui.pluginUnload.hide();
    }
};

/**
 * Loads or unloads the currently selected plug-in.
 */
AdminApp.prototype._togglePlugin = function () {
    function showRestartMesssage() {
        let msg = "Unloading Java resources requires a full server restart.";
        return RapidContext.UI.Msg.info(msg);
    }
    this.ui.pluginReload.hide();
    this.ui.pluginLoading.show();
    let data = this.ui.pluginTable.getSelectedData();
    let isJava = data._loaded && /\blib\b/.test(data._content);
    let proc = data._loaded ? "system/plugin/unload" : "system/plugin/load";
    RapidContext.App.callProc(proc, [data.id])
        .catch(RapidContext.UI.showError)
        .then(() => this.resetServer())
        .then(() => this.loadPlugins())
        .then(isJava ? showRestartMesssage : () => true);
};

/**
 * Handles the plug-in file upload and installation.
 */
AdminApp.prototype._pluginUpload = function () {
    function initProgress(widget, size) {
        var text;
        if (size > 1000000) {
            text = MochiKit.Format.roundToFixed(size / 1048576, 1) + " MiB";
        } else if (size > 2000) {
            text = MochiKit.Format.roundToFixed(size / 1024, 1) + " KiB";
        } else {
            text = size + " bytes";
        }
        widget.setAttrs({ min: 0, max: size, value: 0, text: text });
    }
    function updateProgress(evt) {
        this.ui.pluginProgress.setAttrs({ value: evt.loaded });
    }
    function install() {
        return RapidContext.App.callProc("system/plugin/install", ["plugin"]);
    }
    function select() {
        this.ui.pluginTable.setSelectedIds(pluginId);
        this._showPlugin();
    }
    function done() {
        $(this.ui.pluginInstall).removeClass("widgetHidden");
        this.ui.pluginReset.show();
        this.ui.pluginProgress.hide();
    }
    $(this.ui.pluginInstall).addClass("widgetHidden");
    this.ui.pluginReset.hide();
    this.ui.pluginProgress.show();
    var pluginId;
    var file = this.ui.pluginFile.files[0];
    initProgress(this.ui.pluginProgress, file.size);
    RapidContext.App.uploadFile("plugin", file, updateProgress.bind(this))
        .then(install)
        .then(function (res) {
            pluginId = res;
        })
        .then(() => this.resetServer())
        .then(() => this.loadPlugins())
        .then(select.bind(this))
        .catch(RapidContext.UI.showError)
        .finally(done.bind(this));
};

/**
 * Sends a server reset (restart) request.
 */
AdminApp.prototype.resetServer = function (e) {
    function showMessage() {
        RapidContext.UI.Msg.success("Server reset complete");
    }
    var promise = RapidContext.App.callProc("system/reset");
    if (e instanceof MochiKit.Signal.Event) {
        return promise.then(showMessage).catch(RapidContext.UI.showError);
    } else {
        return promise;
    }
};

/**
 * Loads the procedures for the procedure tree.
 */
AdminApp.prototype.loadProcedures = function () {
    this.proc.procList();
};

/**
 * Callback function for loading the procedure tree.
 *
 * @param {Object} res the result object
 */
AdminApp.prototype._callbackProcedures = function (res) {
    this.ui.procTree.markAll();
    for (var i = 0; i < res.length; i++) {
        var path = res[i].split(/[./]/g);
        var node = this.ui.procTree.addPath(path);
        node.data = res[i];
    }
    this.ui.procTree.removeAllMarked();
    this._showProcedure();
};

/**
 * Shows the specified procedure in the procedure tree and in the
 * details area.
 *
 * @param {string} name the procedure name
 */
AdminApp.prototype.showProcedure = function (name) {
    var node = this.ui.procTree.findByPath(name.split(/[./]/g));
    if (node == null) {
        throw new Error("failed to find procedure '" + name + "'");
    } else if (node.isSelected()) {
        this._showProcedure();
    } else {
        node.select();
    }
};

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
    this.ui.procAlias.classList.add("hidden");
    this.ui.procDeprecated.classList.add("hidden");
    this.ui.procArgTable.innerHTML = "";
    this.ui.procExec.disable();
    this.ui.procBatch.disable();
    this.ui.procExecResult.removeAll();
    RapidContext.Util.resizeElements(this.ui.procExecResult);
    if (node != null && node.data != null) {
        var cb = (res) => this._callbackShowProcedure(node.data, res);
        this.proc.procRead(node.data).then(cb, cb);
        this.ui.procLoading.show();
    }
};

/**
 * Callback function for showing detailed procedure information.
 *
 * @param {string} procName the procedure name
 * @param {Object} res the result object or error
 */
AdminApp.prototype._callbackShowProcedure = function (procName, res) {
    this.ui.procReload.show();
    this.ui.procLoading.hide();
    if (res instanceof Error) {
        RapidContext.UI.showError(res);
    } else {
        this._currentProc = res;
        if (res.local) {
            this.ui.procRemove.show();
        }
        if (res.type == "built-in" || res.type == "procedure") {
            res.type = "Java built-in";
        } else {
            this.ui.procEdit.show();
        }
        this.ui.procAlias.classList.toggle("hidden", !res.alias);
        this.ui.procDeprecated.classList.toggle("hidden", !res.deprecated);
        this.ui.procForm.update(res);
        this.ui.procArgTable.innerHTML = "";
        var count = 0;
        for (var i = 0; i < res.bindings.length; i++) {
            var b = res.bindings[i];
            if (b.type == "argument") {
                var attrs = { "class": "-form-layout-label" };
                var text = RapidContext.Util.toTitleCase(b.name) + ":";
                var col1 = MochiKit.DOM.TH(attrs, text);
                var name = "arg" + count;
                var value = this._defaults[b.name] || "";
                var field = RapidContext.Widget.TextField({ name: name, value: value });
                var btn = RapidContext.Widget.Button({
                    "class": "font-smaller ml-1",
                    "icon": "fa fa-lg fa-pencil"
                });
                btn.onclick = this._editProcArg.bind(this, count);
                var col2 = MochiKit.DOM.TD({ "class": "text-nowrap pr-2" }, field, btn);
                var col3 = MochiKit.DOM.TD({ "class": "text-pre-wrap w-100 pt-1" }, b.description);
                var tr = MochiKit.DOM.TR({}, col1, col2, col3);
                this.ui.procArgTable.append(tr);
                count++;
            }
        }
        this.ui.procExec.enable();
        this.ui.procBatch.enable();
        this.ui.procExecResult.removeAll();
        RapidContext.Util.resizeElements(this.ui.procExecResult);
    }
};

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
};

/**
 * Opens the procedure argument editor for a specific argument.
 *
 * @param {number} idx the argument index
 */
AdminApp.prototype._editProcArg = function (idx) {
    var args = this._getProcArgs();
    this.ui.procArgForm.update(args[idx]);
    this.ui.procArgForm.argumentIndex = idx;
    this.ui.procArgDialog.show();
};

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
    var text;
    if (form.type === "string" && lines.length <= 1) {
        field.disabled = false;
        field.dataValue = undefined;
        field.setAttrs({ value: form.value });
    } else if (form.type === "json") {
        field.disabled = true;
        field.dataValue = form.value;
        text = "JSON object";
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
        text = lines.length + " lines, " + form.value.length + " chars";
        field.setAttrs({ value: text });
    }
    this.ui.procArgDialog.hide();
};

/**
 * Opens the procedure editing dialog for a new procedure.
 */
AdminApp.prototype._addProcedure = function () {
    var data = {
        name: "",
        type: "procedure/javascript",
        description: "",
        bindings: {},
        defaults: {}
    };
    this._initProcEdit(data).then(() => this._updateProcEdit());
};

/**
 * Removes the currently shown procedure (if in the local plug-in).
 */
AdminApp.prototype._removeProcedure = function () {
    let proc = this._currentProc.name;
    RapidContext.UI.Msg.warning.remove("procedure", proc)
        .then(() => {
            this.ui.overlay.setAttrs({ message: "Deleting..." });
            this.proc.procDelete(proc);
        })
        .catch(() => true);
};

/**
 * Opens the procedure editing dialog for an existing procedure.
 */
AdminApp.prototype._editProcedure = function () {
    var p = this._currentProc;
    var data = {
        id: p.id,
        name: p.name,
        type: p.type,
        alias: p.alias,
        description: p.description,
        bindings: {},
        defaults: {}
    };
    for (var i = 0; i < p.bindings.length; i++) {
        var b = Object.assign({}, p.bindings[i]);
        if (b.type === "argument") {
            b.value = b.description;
            b.description = "";
        }
        data.bindings[b.name] = b;
    }
    this._initProcEdit(data);
};

/**
 * Initializes the procedure editing dialog.
 */
AdminApp.prototype._initProcEdit = function (data) {
    this.ui.procEditDialog.data = data;
    var select = this.ui.procEditType;
    return this.proc.procTypes()
        .then(function (res) {
            select.innerHTML = "";
            Object.keys(res).sort().forEach(function (k) {
                var name = k.replace("procedure/", "");
                select.append(MochiKit.DOM.OPTION({ value: k }, name));
                var values = res[k].bindings;
                var keys = MochiKit.Base.map(MochiKit.Base.itemgetter("name"), values);
                data.defaults[k] = RapidContext.Util.dict(keys, values);
            });
        })
        .then(() => this._renderProcEdit())
        .then(() => this.ui.procEditDialog.show())
        .catch(RapidContext.UI.showError);
};

/**
 * Renders the procedure editing form from the data object stored in
 * the dialog.
 */
AdminApp.prototype._renderProcEdit = function () {
    function buildIcon(cls, action) {
        return RapidContext.Widget.Icon({ "class": cls, "data-action": action });
    }
    function buildBinding(b, def) {
        var name = MochiKit.DOM.STRONG({}, b.name + ": ");
        var icon = def ? null : buildIcon("fa fa-minus-square widget-red", "remove");
        var up = def ? null : buildIcon("fa fa-arrow-circle-up widget-grey", "up");
        var desc = def ? def.description : b.description || "";
        var div = MochiKit.DOM.DIV({ "class": "text-pre-wrap py-1" }, name, icon, up, desc);
        var attrs = {
            name: "binding." + b.name,
            value: b.value,
            "class": "w-100 mb-2"
        };
        var field;
        if (!b.value.includes("\n")) {
            field = RapidContext.Widget.TextField(attrs);
        } else {
            attrs.rows = Math.min(20, b.value.match(/\n/g).length + 2);
            attrs.wrap = "off";
            field = RapidContext.Widget.TextArea(attrs);
        }
        return MochiKit.DOM.DIV({ "class": "binding" }, div, field);
    }
    var data = this.ui.procEditDialog.data;
    var elems = this.ui.procEditForm.querySelectorAll(".binding");
    RapidContext.Widget.destroyWidget(elems);
    var parents = {
        connection: this.ui.procEditConns,
        data: this.ui.procEditData,
        procedure: this.ui.procEditProcs,
        argument: this.ui.procEditArgs
    };
    for (var k in data.bindings) {
        var b = data.bindings[k];
        var def = data.defaults[data.type][b.name];
        parents[b.type].append(buildBinding(b, def));
    }
    Object.values(parents).forEach(function (node) {
        if (node.childNodes.length == 0) {
            var div = MochiKit.DOM.DIV({ "class": "py-1 widget-grey binding" }, "\u2014");
            node.append(div);
        }
    });
    var isTypeJs = data.type == "procedure/javascript";
    this.ui.procEditProcs.parentNode.className = isTypeJs ? "" : "hidden";
    var opts = this.ui.procEditAddType.getElementsByTagName("option");
    opts[0].disabled = !isTypeJs;
    opts[1].disabled = !isTypeJs;
    opts[2].disabled = !isTypeJs;
    if (!isTypeJs) {
        this.ui.procEditAddType.selectedIndex = 3;
    }
    this.ui.procEditForm.update(data);
    this.ui.procEditDialog.moveToCenter();
};

/**
 * Handles addition and removal of procedure bindings.
 */
AdminApp.prototype._addRemoveProcBinding = function (evt) {
    var $el = $(evt.target()).closest("[data-action]");
    if ($el.data("action") === "add") {
        var data = this.ui.procEditDialog.data;
        var type = this.ui.procEditAddType.value;
        var name = this.ui.procEditAddName.value.trim();
        var value = (type == "data") ? "\n" : "";
        if (/[a-z0-9_-]+/i.test(name) && !data.bindings[name]) {
            data.bindings[name] = { name: name, type: type, value: value };
            this.ui.procEditAddName.setAttrs({ name: "binding." + name, value: value });
            this._updateProcEdit();
            this.ui.procEditAddName.setAttrs({ name: null, value: "" });
        } else {
            RapidContext.UI.showError("Name is invalid or already in use.");
            this.ui.procEditAddName.focus();
        }
    } else if ($el.data("action") === "remove") {
        RapidContext.Widget.destroyWidget($el.closest(".binding").get(0));
        this._updateProcEdit();
    } else if ($el.data("action") === "up") {
        var prev = $el.closest(".binding").get(0).previousSibling;
        prev && prev.parentNode.insertBefore(prev.nextSibling, prev);
    }
};

/**
 * Updates the procedure editing data from the form values. This
 * method will automatically handle removed bindings, added bindings
 * or modification of the procedure type. It will also trigger
 * re-rendering of the form.
 */
AdminApp.prototype._updateProcEdit = function () {
    var data = this.ui.procEditDialog.data;
    var values = this.ui.procEditForm.valueMap();
    var bindings = {};
    var k, b;
    for (k in values) {
        var name = /^binding\./.test(k) && k.replace(/^binding\./, "");
        b = name && data.bindings[name];
        if (b) {
            b.value = values[k];
            bindings[name] = b;
        }
    }
    var defaults = data.defaults[values.type];
    for (k in defaults) {
        if (!bindings[k]) {
            b = Object.assign({}, defaults[k]);
            b.value = (b.type == "data") ? "\n" : "";
            bindings[k] = b;
        }
    }
    for (k in bindings) {
        b = bindings[k];
        if (!defaults[k] && !b.value.trim() && b.description) {
            delete bindings[k]; // Remove previous type defaults
        }
    }
    data.name = values.name;
    data.type = values.type;
    data.description = values.description;
    data.bindings = bindings;
    this._renderProcEdit();
};

/**
 * Saves the procedure from the procedure edit dialog.
 */
AdminApp.prototype._saveProcedure = function () {
    this._updateProcEdit();
    var prev = this.ui.procEditDialog.data;
    var data = {
        id: prev.name,
        type: prev.type,
        alias: prev.alias,
        description: prev.description,
        binding: []
    };
    if (!data.alias && data.id === data.alias) {
        delete data.alias;
    }
    for (var k in prev.bindings) {
        var b = prev.bindings[k];
        var res = { name: b.name, type: b.type };
        if (b.type == "argument") {
            res.description = b.value;
        } else {
            res.value = b.value;
        }
        data.binding.push(res);
    }
    var oldPath = prev.id ? RapidContext.Storage.path(prev) : null;
    var newPath = RapidContext.Storage.path(data);
    var opts = { path: newPath + ".yaml" };
    if (oldPath && oldPath !== newPath) {
        opts = { path: oldPath, updateTo: opts.path };
    } else if (oldPath) {
        opts.update = true;
    }
    RapidContext.App.callProc("system/storage/write", [opts, data])
        .then(() => this.ui.procEditDialog.hide())
        .then(() => this.proc.procList.recall())
        .then(() => this.showProcedure(data.id))
        .catch(RapidContext.UI.showError);
};

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
    var cb = (res) => this._callbackExecute(res);
    RapidContext.App.callProc(proc.name, args).then(cb, cb);
};

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
};

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
                if (Object.prototype.hasOwnProperty.call(value, "toString")) {
                    var s = value.toString();
                    return truncate ? MochiKit.Text.truncate(s, 50, "...") : s;
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
                v = data[parseInt(k, 10)];
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
        var nodeAttrs = {
            name: dataLabel(type, data, true),
            tooltip: dataLabel(type, data, false)
        };
        node.addAll(RapidContext.Widget.TreeNode(nodeAttrs));
    }
};

/**
 * Expands a procedure result tree node by expanding a node.
 *
 * @param {Event} evt the tree node expand event
 */
AdminApp.prototype._showExecDataExpand = function (evt) {
    var node = evt.event().detail.node;
    this._showExecData(node, node.data);
};

/**
 * Creates a new set of batch calls for the current procedure.
 */
AdminApp.prototype._createBatch = function () {
    var proc = this._currentProc;
    var args = [];
    var count = null;
    var values = this._getProcArgs();
    var i, j;
    for (i = 0; i < proc.bindings.length; i++) {
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
                value = value.split("\n").map(function (s) {
                    return s.trim();
                });
                if (value.length <= 1) {
                    value = value[0];
                }
            }
            if (value instanceof Array) {
                if (count == null) {
                    count = value.length;
                } else if (value.length != count) {
                    var msg = (
                        "Mismatching line count for field " + name +
                        ": expected " + count + ", but found " +
                        value.length
                    );
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
    for (i = 0; i < count; i++) {
        var callArgs = [];
        for (j = 0; j < args.length; j++) {
            var arg = args[j];
            if (arg instanceof Array) {
                callArgs.push(arg[i]);
            } else {
                callArgs.push(arg);
            }
        }
        this._batch.queue.push({ proc: proc.name, args: callArgs });
    }
    this.ui.tabContainer.selectChild(this.ui.batchTab);
    this._startBatch();
};

/**
 * Configure the batch delay.
 */
AdminApp.prototype._configBatchDelay = function () {
    var msg = "Enter order processing delay (in seconds):";
    var value = prompt(msg, "" + this._batch.delay);
    if (value != null) {
        value = parseInt(value, 10);
        if (!isNaN(value)) {
            this._batch.delay = Math.max(value, 0);
        }
    }
};

/**
 * Starts the batch processing if not already running.
 */
AdminApp.prototype._startBatch = function () {
    if (!this._batch.running) {
        this._batch.running = true;
        this._batch.stat.success = 0;
        this._batch.stat.failed = 0;
        this.ui.batchProgress.setAttrs({ min: 0, max: this._batch.queue.length, text: null });
        this.ui.batchPlay.hide();
        this.ui.batchPause.show();
        this.ui.batchLoading.show();
        this._processBatch();
    }
};

/**
 * Stops the batch processing if running.
 */
AdminApp.prototype._stopBatch = function () {
    this._batch.running = false;
    this.ui.batchPlay.show();
    this.ui.batchPause.hide();
    this.ui.batchLoading.hide();
};

/**
 * Toggles the batch processing.
 */
AdminApp.prototype._toggleBatch = function () {
    if (this._batch.running) {
        this._stopBatch();
    } else {
        this._startBatch();
    }
};

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
};

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
            var cb = (res) => this._callbackBatch(res);
            RapidContext.App.callProc(item.proc, item.args).then(cb, cb);
        }
    }
};

/**
 * Callback function for batch processing.
 *
 * @param {Object} res the call result data or error
 */
AdminApp.prototype._callbackBatch = function (res) {
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
};

/**
 * Loads the list of users.
 */
AdminApp.prototype.loadUsers = function () {
    this.proc.userList();
};

/**
 * Modifies the user form for adding a new user.
 */
AdminApp.prototype._addUser = function () {
    this.ui.userForm.reset();
    this.ui.userForm.update({
        enabled: true,
        passwordHint: "Minimum 5 characters"
    });
    this.ui.userId.setAttrs({ readonly: null });
};

/**
 * Modifies the user form for editing an existing user.
 */
AdminApp.prototype._editUser = function () {
    var data = this.ui.userTable.getSelectedData();
    this.ui.userForm.reset();
    var extra = {
        roles: (data.role) ? data.role.join(" ") : "",
        passwordHint: "Leave blank for unmodified"
    };
    this.ui.userForm.update(Object.assign(extra, data));
    this.ui.userId.setAttrs({ readonly: true });
};

/**
 * Saves the user modification form.
 */
AdminApp.prototype._saveUser = function () {
    var data = this.ui.userForm.valueMap();
    if (this.ui.userForm.validate()) {
        var desc = data.description;
        var enabled = data.enabled ? "1" : "0";
        var pwd = data.password;
        this.proc.userChange(data.id, data.name, data.email, desc, enabled, pwd, data.roles);
    }
};

/**
 * Updates the log level.
 */
AdminApp.prototype._updateLogLevel = function (evt) {
    if (evt) {
        RapidContext.Log.level(this.ui.logForm.valueMap().level);
    }
    this.ui.logForm.update({ level: RapidContext.Log.level() });
};

/**
 * Clears the log.
 */
AdminApp.prototype._clearLogs = function () {
    RapidContext.Log.clear();
    this.ui.logTable.setData([]);
    this.ui.logData.innerText = "";
};

/**
 * Refreshes the log levels and table.
 */
AdminApp.prototype._showLogs = function () {
    this.ui.logForm.update({ level: RapidContext.Log.level() });
    this.ui.logTable.setData(RapidContext.Log.history());
};

/**
 * Show log details for the selected log data.
 */
AdminApp.prototype._showLogDetails = function () {
    var data = this.ui.logTable.getSelectedData();
    var text;
    if (data == null || data.data == null) {
        text = "<no additional data>";
    } else {
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
        text = list.join("\n");
    }
    this.ui.logData.innerText = text;
};

/**
 * Splits a text string into multiple lines. Existing line breaks
 * will be preserved (recognizing "\n", "\r" or "\r\n"), but
 * additional line breaks may be added by specifying a maximum line
 * length. If the keepspace flag is set, the newlines and space
 * characters at the end of each line will be preserved.
 *
 * @param {string} str the string to split
 * @param {number} [maxlen] the maximum line length, or zero for
 *            unlimited (defaults to 0)
 * @param {boolean} [keepspace] the boolean flag to keep
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
                m = /[^\s).:,;=-]*$/.exec(line);
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
    var pos = 0;
    while (re.exec(str) != null) {
        splitPush(str.substring(pos, re.lastIndex));
        pos = re.lastIndex;
    }
    splitPush(str.substring(pos));
    return lines;
};
