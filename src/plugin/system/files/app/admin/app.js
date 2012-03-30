/**
 * Creates a new admin app.
 */
function AdminApp() {
    this._defaults = { operatorId: RapidContext.App.user().id };
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
        appList: "System.App.List",
        plugInList: "System.PlugIn.List",
        procList: "System.Procedure.List",
        userList: "System.User.List",
        userChange: "System.User.Change"
    });
    // Initialize event signals
    MochiKit.Signal.connect(this.ui.root, "onenter", MochiKit.Base.bind("selectChild", this.ui.tabContainer, null));
    RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
    MochiKit.Signal.connect(this.proc.appList, "onsuccess", this.ui.appTable, "setData");
    MochiKit.Signal.connect(this.ui.appTable, "onselect", this, "_showApp");
    MochiKit.Signal.connect(this.ui.appLaunch, "onclick", this, "_launchApp");
    MochiKit.Signal.connect(this.ui.pluginTab, "onenter", this, "_pluginUploadInit");
    MochiKit.Signal.connectOnce(this.ui.pluginTab, "onenter", this, "loadPlugins");
    MochiKit.Signal.connect(this.ui.pluginFile, "onselect", this, "_pluginUploadStart");
    MochiKit.Signal.connect(this.ui.pluginInstall, "onclick", this, "_pluginInstall");
    MochiKit.Signal.connect(this.ui.pluginReset, "onclick", this, "resetServer");
    MochiKit.Signal.connect(this.ui.pluginFileDelete, "onclick", this, "_pluginUploadInit");
    RapidContext.UI.connectProc(this.proc.plugInList, this.ui.pluginLoading, this.ui.pluginReload);
    MochiKit.Signal.connect(this.proc.plugInList, "onsuccess", this.ui.pluginTable, "setData");
    MochiKit.Signal.connect(this.ui.pluginTable, "onselect", this, "_showPlugin");
    MochiKit.Signal.connect(this.ui.pluginLoad, "onclick", this, "_togglePlugin");
    MochiKit.Signal.connect(this.ui.pluginUnload, "onclick", this, "_togglePlugin");
    MochiKit.Signal.connectOnce(this.ui.procTab, "onenter", this.proc.procList, "call");
    RapidContext.UI.connectProc(this.proc.procList, this.ui.procTreeLoading, this.ui.procTreeReload);
    MochiKit.Signal.connect(this.proc.procList, "onsuccess", this, "_callbackProcedures");
    MochiKit.Signal.connect(this.ui.procTree, "onselect", this, "_showProcedure");
    MochiKit.Signal.connect(this.ui.procAdd, "onclick", this, "_addProcedure");
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
    MochiKit.Signal.connect(this.ui.batchDelete, "onclick", this, "_clearBatch");
    MochiKit.Signal.connect(this.ui.batchDelay, "onclick", this, "_configBatchDelay");
    MochiKit.Signal.connect(this.ui.batchResume, "onclick", this, "_toggleBatch");
    MochiKit.Signal.connectOnce(this.ui.userTab, "onenter", this, "loadUsers");
    RapidContext.UI.connectProc(this.proc.userList, this.ui.userLoading, this.ui.userReload);
    MochiKit.Signal.connect(this.proc.userList, "onsuccess", this.ui.userTable, "setData");
    MochiKit.Signal.connect(this.ui.userTable, "onselect", this, "_editUser");
    MochiKit.Signal.connect(this.ui.userAdd, "onclick", this, "_addUser");
    MochiKit.Signal.connect(this.ui.userSave, "onclick", this, "_saveUser");
    RapidContext.UI.connectProc(this.proc.userChange);
    MochiKit.Signal.connect(this.proc.userChange, "onsuccess", this.proc.userList, "recall");
    MochiKit.Signal.connect(this.ui.logTab, "onenter", this, "_showLogs");
    MochiKit.Signal.connect(this.ui.logError, "onclick", MochiKit.Base.bind("setLogLevel", this, LOG.ERROR));
    MochiKit.Signal.connect(this.ui.logWarning, "onclick", MochiKit.Base.bind("setLogLevel", this, LOG.WARNING));
    MochiKit.Signal.connect(this.ui.logInfo, "onclick", MochiKit.Base.bind("setLogLevel", this, LOG.INFO));
    MochiKit.Signal.connect(this.ui.logTrace, "onclick", MochiKit.Base.bind("setLogLevel", this, LOG.TRACE));
    MochiKit.Signal.connect(this.ui.logClear, "onclick", this, "_clearLogs");
    MochiKit.Signal.connect(this.ui.logReload, "onclick", this, "_showLogs");
    MochiKit.Signal.connect(this.ui.logTable, "onselect", this, "_showLogDetails");
    var func = function (td, data) {
        if (data) {
            td.appendChild(RapidContext.Widget.Icon({ ref: "OK", tooltip: "Loaded" }));
        } else {
            td.appendChild(RapidContext.Widget.Icon({ ref: "ERROR", tooltip: "Not loaded" }));
        }
    };
    this.ui.pluginTable.getChildNodes()[0].setAttrs({ renderer: func });
    this.ui.procExecLoading.hide();
    // TODO: Security test should be made on access, not role name
    if (MochiKit.Base.findValue(RapidContext.App.user().role, "admin") < 0) {
        this.ui.procAdd.hide();
        this.ui.tabContainer.removeChildNode(this.ui.pluginTab);
        this.ui.tabContainer.removeChildNode(this.ui.userTab);
        RapidContext.Widget.destroyWidget(this.ui.pluginTab);
        RapidContext.Widget.destroyWidget(this.ui.userTab);
        this.ui.pluginTab = null;
        this.ui.userTab = null;
    }
    // Special resize procedure to adjust to available height
    this.ui.procExecResult.resizeContent = function () {
        var pos = MochiKit.Style.getElementPosition(this, this.parentNode);
        var dim = MochiKit.Style.getElementDimensions(this.parentNode);
        MochiKit.Style.setElementDimensions(this, { h: dim.h - pos.y });
    }

    // Initialize data
    this.proc.appList();
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
 * Shows detailed app information.
 */
AdminApp.prototype._showApp = function () {
    var data = this.ui.appTable.getSelectedData();
    this.ui.appForm.reset();
    this.ui.appForm.update(data);
    MochiKit.DOM.replaceChildNodes(this.ui.appResources);
    for (var i = 0; i < data.resources.length; i++) {
        var res = data.resources[i];
        MochiKit.DOM.appendChildNodes(this.ui.appResources, res.url, MochiKit.DOM.BR());
    }
}

/**
 * Launches the currently selected app.
 */
AdminApp.prototype._launchApp = function () {
    var data = this.ui.appTable.getSelectedData();
    RapidContext.App.startApp(data.className);
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
    this.ui.pluginForm.update(data);
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
    d.addBoth(MochiKit.Base.bind("_showPlugin", this))
}

/**
 * Initializes the plug-in file upload and installation interface.
 */
AdminApp.prototype._pluginUploadInit = function () {
    this.ui.pluginFile.show();
    this.ui.pluginProgress.hide();
    this.ui.pluginFileInfo.hide();
    this.ui.pluginInstall.disabled = true;
}

/**
 * Handles the plug-in file upload init.
 */
AdminApp.prototype._pluginUploadStart = function () {
    this.ui.pluginFile.hide();
    this.ui.pluginProgress.show();
    this.ui.pluginProgress.setAttrs({ min: 0, max: 100 });
    this.ui.pluginProgress.setRatio(0);
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
    if (res == null) {
        MochiKit.Async.callLater(1, pluginLoadStatus);
    } else if (res.files.progress) {
        this.ui.pluginProgress.setRatio(res.files.progress);
        MochiKit.Async.callLater(1, pluginLoadStatus);
    } else {
        this._pluginUploadInfo(res.files.plugin);
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
    this.ui.pluginInstall.disabled = false;
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
    this.ui.procEdit.hide();
    this.ui.procReload.hide();
    this.ui.procLoading.hide();
    MochiKit.DOM.replaceChildNodes(this.ui.procArgTable);
    this.ui.procExec.disabled = true;
    this.ui.procBatch.disabled = true;
    this.ui.procExecResult.removeAll();
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
                var attrs = { name: "arg" + count, value: value, style: "margin-right: 3px;" };
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
        this.ui.procExec.disabled = false;
        this.ui.procBatch.disabled = false;
        this.ui.procExecResult.removeAll();
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
    var d = RapidContext.App.callProc("System.Procedure.Types");
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
    this.ui.procExec.disabled = true;
    this.ui.procBatch.disabled = true;
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
    this.ui.procExec.disabled = false;
    this.ui.procBatch.disabled = false;
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
        this.ui.batchProgress.setAttrs({ min: 0, max: this._batch.queue.length });
        this.ui.batchProgress.setValue(0);
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
    this.ui.batchProgress.setText("Stopped");
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
    if (this.ui.batchProgress.maxValue != total) {
        this.ui.batchProgress.setAttrs({ min: 0, max: total });
    }
    this.ui.batchProgress.setValue(done);
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
    this.ui.userId.disabled = false;
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
    this.ui.userId.disabled = true;
}

/**
 * Saves the user modification form.
 */
AdminApp.prototype._saveUser = function () {
    var data = this.ui.userForm.valueMap();
    if (this.ui.userForm.validate()) {
        this.proc.userChange(data.id,
                             data.name,
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
