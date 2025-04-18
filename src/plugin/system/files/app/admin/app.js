/* eslint no-alert: "off", no-unmodified-loop-condition: "off", no-var: "off" */
/* eslint prefer-template: "off" */

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
    const statusRenderer = function (td, value, data) {
        const usedAt = data && data._lastUsedTime || "@0";
        const since = Date.now() - parseInt(usedAt.substr(1), 10);
        let cls = "fa fa-check";
        if (data._error || data._lastError) {
            cls = "fa fa-exclamation-circle color-danger";
        } else if (!data._openChannels && since > 5 * 60 * 1000) {
            cls = "fa fa-exclamation-triangle color-warning";
        }
        td.append(RapidContext.Widget.Icon({ "class": cls }));
    };
    const typeRenderer = function (td, value, data) {
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
    const urlRenderer = function (td, value, data) {
        if (value) {
            const ico = RapidContext.Widget.Icon({ "class": "fa fa-external-link-square ml-1" });
            const link = RapidContext.UI.A({ href: value, target: "_blank" }, value, ico);
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
    const user = RapidContext.App.user();
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
    for (const name in this.proc) {
        MochiKit.Signal.disconnectAll(this.proc[name]);
    }
};

/**
 * Updates the type cache.
 */
AdminApp.prototype._updateTypeCache = function (res) {
    let type;
    this._types = {};
    for (let i = 0; i < res.length; i++) {
        type = res[i];
        this._types[type.id] = type;
        type.property = type.property || [];
        type.properties = {};
        for (let j = 0; j < type.property.length; j++) {
            const prop = type.property[j];
            type.properties[prop.name] = prop;
        }
        delete type.property;
    }
    for (const id in this._types) {
        type = this._types[id];
        const parts = id.split("/");
        while (parts.length > 1) {
            parts.pop();
            const baseId = parts.join("/");
            const baseType = this._types[baseId];
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
    data = { ...data };
    if (/^@\d+$/.test(data._lastUsedTime)) {
        const dttm = new Date(+data._lastUsedTime.substr(1));
        data.lastAccess = MochiKit.DateTime.toISOTimestamp(dttm);
    }
    this.ui.cxnForm.reset();
    this.ui.cxnForm.update(data);
    this.ui.cxnRemove.setAttrs({ hidden: data._plugin != "local" });
    if (data._plugin && data.id) {
        this.ui.cxnEdit.show();
        const url = "rapidcontext/storage/connection/" + data.id;
        this.ui.cxnLink.setAttribute("href", url);
        this.ui.cxnLink.classList.remove("hidden");
    } else {
        this.ui.cxnEdit.hide();
        this.ui.cxnLink.classList.add("hidden");
    }
    const clones = this.ui.cxnTemplate.parentNode.querySelectorAll(".clone");
    RapidContext.Widget.destroyWidget(clones);
    const props = ["id", "type", "_plugin", "description", "lastAccess", "maxOpen"];
    for (const k in data) {
        let v = data[k];
        const hidden = k.startsWith("_") || props.includes(k);
        if (!hidden || (/error$/i.test(k) && v)) {
            const title = RapidContext.Util.toTitleCase(k);
            if (v == null) {
                v = "";
            }
            if (/error$/i.test(k)) {
                v = RapidContext.UI.SPAN({ "class": "important" }, v);
            } else if (/password$/i.test(k)) {
                v = RapidContext.Widget.Field({ name: k, value: v, mask: true });
            }
            const tr = this.ui.cxnTemplate.cloneNode(true);
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
    const data = this.ui.cxnTable.getSelectedData();
    RapidContext.UI.Msg.warning.remove("connection", data.id)
        .then(() => {
            const path = RapidContext.Storage.path(data);
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
    const data = this.ui.cxnTable.getSelectedData();
    this._initConnectionEdit(data);
};

/**
 * Initializes the connection editing dialog.
 */
AdminApp.prototype._initConnectionEdit = function (data) {
    this.ui.cxnEditDialog.data = data;
    this.ui.cxnEditForm.reset();
    this.ui.cxnEditForm.update(data);
    const self = this;
    this.proc.typeList()
        .then(function () {
            const select = self.ui.cxnEditType;
            while (select.firstChild.nextSibling) {
                RapidContext.Widget.destroyWidget(select.firstChild.nextSibling);
            }
            for (const k in self._types) {
                if (/connection\//.test(k)) {
                    const attrs = { value: k };
                    if (data.type === k) {
                        attrs.selected = true;
                    }
                    select.append(RapidContext.UI.OPTION(attrs, k));
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
        const title = p.title || RapidContext.Util.toTitleCase(p.name);
        const value = (data[p.name] != null) ? "" + data[p.name] : "";
        const defaultValue = (data["_" + p.name] != null) ? "" + data["_" + p.name] : "";
        const valueLines = AdminApp.splitLines(value, 58);
        tr.className = "clone";
        tr.firstChild.append(title + ":");
        const attrs = { name: p.name, size: 60 };
        if (p.required && p.format != "password") {
            attrs.required = true;
        }
        if (defaultValue) {
            attrs.placeholder = defaultValue;
        }
        let input;
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
            const btn = { icon: "fa fa-lg fa-minus", "class": "font-smaller ml-1", "data-action": "remove" };
            tr.lastChild.append(RapidContext.Widget.Button(btn));
        }
        if (p.required && p.format != "password") {
            const validator = RapidContext.Widget.FormValidator({ name: p.name });
            tr.lastChild.append(validator);
        }
        if (p.description) {
            const help = RapidContext.UI.DIV({ "class": "helptext text-pre-wrap" }, p.description);
            tr.lastChild.append(help);
        }
        const showAll = (data._showAll == "yes");
        if (!showAll && !p.required && !p.custom && !value) {
            tr.classList.add("hidden");
        }
        return tr;
    }
    var data = this.ui.cxnEditForm.valueMap();
    this.ui.cxnEditForm.reset();
    MochiKit.Base.setdefault(data, this.ui.cxnEditDialog.data);
    const clones = this.ui.cxnEditTemplate.parentNode.querySelectorAll(".clone");
    RapidContext.Widget.destroyWidget(clones);
    const hiddenProps = { id: true, type: true, _plugin: true };
    const props = {};
    if (this._types[data.type]) {
        const type = this._types[data.type];
        Object.assign(props, type.properties);
        this.ui.cxnEditTypeDescr.innerText = type.description;
    } else {
        this.ui.cxnEditTypeDescr.innerText = "";
    }
    for (const name in data) {
        const val = String(data[name]).trim();
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
    for (const k in props) {
        const tr = buildRow(this.ui.cxnEditTemplate.cloneNode(true), props[k]);
        this.ui.cxnEditTemplate.before(tr);
    }
    this.ui.cxnEditForm.update(data);
    this.ui.cxnEditDialog.moveToCenter();
};

/**
 * Handles addition and removal of custom connection parameters.
 */
AdminApp.prototype._addRemoveConnectionProps = function (evt) {
    const $el = $(evt.target()).closest("[data-action]");
    if ($el.data("action") === "add") {
        const name = this.ui.cxnEditAddParam.value.trim();
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
        const prev = this.ui.cxnEditDialog.data;
        const data = this.ui.cxnEditForm.valueMap();
        for (const k in data) {
            const v = data[k].trim();
            if (v && !k.startsWith("_")) {
                // Store updated value
            } else if (prev.id && !k.startsWith("_") && !k.startsWith(".")) {
                data[k] = null; // Remove old value
            } else {
                delete data[k]; // Omit or keep unmodified
            }
        }
        const oldPath = prev.id ? RapidContext.Storage.path(prev) : null;
        const newPath = RapidContext.Storage.path(data);
        let opts = { path: newPath + ".yaml" };
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
    const data = this.ui.appTable.getSelectedData();
    this.ui.appForm.reset();
    if (data) {
        this.ui.appForm.update(data);
        this.ui.appIcon.innerHTML = "";
        if (data.icon) {
            this.ui.appIcon.append(data.icon.cloneNode(true));
        }
        const url = "rapidcontext/storage/app/" + data.id;
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
    const data = this.ui.appTable.getSelectedData();
    RapidContext.App.startApp(data.id);
};

/**
 * Launches the currently selected app in separate window.
 */
AdminApp.prototype._launchAppWindow = function () {
    const data = this.ui.appTable.getSelectedData();
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
    const data = this.ui.pluginTable.getSelectedData();
    this.ui.pluginForm.reset();
    if (data) {
        this.ui.pluginForm.update(data);
        this.ui.pluginLink.classList.remove("hidden");
        const path = "/.storage/plugin/" + data.id + "/";
        const url = "rapidcontext/storage" + path;
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
        const msg = "Unloading Java resources requires a full server restart.";
        return RapidContext.UI.Msg.info(msg);
    }
    this.ui.pluginReload.hide();
    this.ui.pluginLoading.show();
    const data = this.ui.pluginTable.getSelectedData();
    const isJava = data._loaded && /\blib\b/.test(data._content);
    const proc = data._loaded ? "system/plugin/unload" : "system/plugin/load";
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
        let text;
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
    let pluginId;
    const file = this.ui.pluginFile.files[0];
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
    const promise = RapidContext.App.callProc("system/reset");
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
    for (let i = 0; i < res.length; i++) {
        const path = res[i].split(/[./]/g);
        const node = this.ui.procTree.addPath(path);
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
    const node = this.ui.procTree.findByPath(name.split(/[./]/g));
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
    const node = this.ui.procTree.selectedChild();
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
    if (node != null && node.data != null) {
        const cb = (res) => this._callbackShowProcedure(node.data, res);
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
        let count = 0;
        for (let i = 0; i < res.bindings.length; i++) {
            const b = res.bindings[i];
            if (b.type == "argument") {
                const attrs = { "class": "-form-layout-label" };
                const text = RapidContext.Util.toTitleCase(b.name) + ":";
                const col1 = RapidContext.UI.TH(attrs, text);
                const name = "arg" + count;
                const value = this._defaults[b.name] || "";
                const field = RapidContext.Widget.TextField({ name: name, value: value });
                const btn = RapidContext.Widget.Button({
                    "class": "font-smaller ml-1",
                    "icon": "fa fa-lg fa-pencil"
                });
                btn.onclick = this._editProcArg.bind(this, count);
                const col2 = RapidContext.UI.TD({ "class": "text-nowrap pr-2" }, field, btn);
                const col3 = RapidContext.UI.TD({ "class": "text-pre-wrap w-100 pt-1" }, b.description);
                const tr = RapidContext.UI.TR({}, col1, col2, col3);
                this.ui.procArgTable.append(tr);
                count++;
            }
        }
        this.ui.procExec.enable();
        this.ui.procBatch.enable();
        this.ui.procExecResult.removeAll();
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
    const args = [];
    const rows = this.ui.procArgTable.childNodes;
    for (let i = 0; i < rows.length; i++) {
        const td = rows[i].childNodes[1];
        const field = td.firstChild;
        const type = field.dataType || "string";
        const value = field.dataValue || field.getValue();
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
    const args = this._getProcArgs();
    this.ui.procArgForm.update(args[idx]);
    this.ui.procArgForm.argumentIndex = idx;
    this.ui.procArgDialog.show();
};

/**
 * Updates a procedure argument after editing in the dialog.
 */
AdminApp.prototype._updateProcArg = function () {
    const form = this.ui.procArgForm.valueMap();
    const idx = this.ui.procArgForm.argumentIndex;
    const tr = this.ui.procArgTable.childNodes[idx];
    const td = tr.childNodes[1];
    const field = td.firstChild;
    field.dataType = form.type;
    const lines = form.value.split("\n");
    let text;
    if (form.type === "string" && lines.length <= 1) {
        field.disabled = false;
        field.dataValue = undefined;
        field.setAttrs({ value: form.value });
    } else if (form.type === "json") {
        field.disabled = true;
        field.dataValue = form.value;
        text = "JSON object";
        try {
            const o = MochiKit.Base.evalJSON(form.value);
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
    const data = {
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
    const proc = this._currentProc.name;
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
    const p = this._currentProc;
    const data = {
        id: p.id,
        name: p.name,
        type: p.type,
        alias: p.alias,
        description: p.description,
        bindings: {},
        defaults: {}
    };
    for (let i = 0; i < p.bindings.length; i++) {
        const b = { ...p.bindings[i] };
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
    const select = this.ui.procEditType;
    return this.proc.procTypes()
        .then(function (res) {
            select.innerHTML = "";
            Object.keys(res).sort().forEach(function (k) {
                const name = k.replace("procedure/", "");
                select.append(RapidContext.UI.OPTION({ value: k }, name));
                const values = res[k].bindings;
                const keys = MochiKit.Base.map(MochiKit.Base.itemgetter("name"), values);
                data.defaults[k] = RapidContext.Data.object(keys, values);
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
        const name = RapidContext.UI.STRONG({}, b.name + ": ");
        const icon = def ? null : buildIcon("fa fa-minus-square color-danger", "remove");
        const up = def ? null : buildIcon("fa fa-arrow-circle-up widget-grey", "up");
        const desc = def ? def.description : b.description || "";
        const div = RapidContext.UI.DIV({ "class": "text-pre-wrap py-1" }, name, icon, up, desc);
        const attrs = {
            name: "binding." + b.name,
            value: b.value,
            "class": "w-100 mb-2"
        };
        let field;
        if (!b.value.includes("\n")) {
            field = RapidContext.Widget.TextField(attrs);
        } else {
            attrs.rows = Math.min(20, b.value.match(/\n/g).length + 2);
            attrs.wrap = "off";
            field = RapidContext.Widget.TextArea(attrs);
        }
        return RapidContext.UI.DIV({ "class": "binding" }, div, field);
    }
    const data = this.ui.procEditDialog.data;
    const elems = this.ui.procEditForm.querySelectorAll(".binding");
    RapidContext.Widget.destroyWidget(elems);
    const parents = {
        connection: this.ui.procEditConns,
        data: this.ui.procEditData,
        procedure: this.ui.procEditProcs,
        argument: this.ui.procEditArgs
    };
    for (const k in data.bindings) {
        const b = data.bindings[k];
        const def = data.defaults[data.type][b.name];
        parents[b.type].append(buildBinding(b, def));
    }
    Object.values(parents).forEach(function (node) {
        if (node.childNodes.length == 0) {
            const div = RapidContext.UI.DIV({ "class": "py-1 widget-grey binding" }, "\u2014");
            node.append(div);
        }
    });
    const isTypeJs = data.type == "procedure/javascript";
    this.ui.procEditProcs.parentNode.className = isTypeJs ? "" : "hidden";
    const opts = this.ui.procEditAddType.getElementsByTagName("option");
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
    const $el = $(evt.target()).closest("[data-action]");
    if ($el.data("action") === "add") {
        const data = this.ui.procEditDialog.data;
        const type = this.ui.procEditAddType.value;
        const name = this.ui.procEditAddName.value.trim();
        const value = (type == "data") ? "\n" : "";
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
        const prev = $el.closest(".binding").get(0).previousSibling;
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
    const data = this.ui.procEditDialog.data;
    const values = this.ui.procEditForm.valueMap();
    const bindings = {};
    let k, b;
    for (k in values) {
        const name = /^binding\./.test(k) && k.replace(/^binding\./, "");
        b = name && data.bindings[name];
        if (b) {
            b.value = values[k];
            bindings[name] = b;
        }
    }
    const defaults = data.defaults[values.type];
    for (k in defaults) {
        if (!bindings[k]) {
            b = { ...defaults[k] };
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
    const prev = this.ui.procEditDialog.data;
    const data = {
        id: prev.name,
        type: prev.type,
        alias: prev.alias,
        description: prev.description,
        binding: []
    };
    if (!data.alias && data.id === data.alias) {
        delete data.alias;
    }
    for (const k in prev.bindings) {
        const b = prev.bindings[k];
        const res = { name: b.name, type: b.type };
        if (b.type == "argument") {
            res.description = b.value;
        } else {
            res.value = b.value;
        }
        data.binding.push(res);
    }
    const oldPath = prev.id ? RapidContext.Storage.path(prev) : null;
    const newPath = RapidContext.Storage.path(data);
    let opts = { path: newPath + ".yaml" };
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
    const proc = this._currentProc;
    const args = [];
    const values = this._getProcArgs();
    for (let i = 0; i < proc.bindings.length; i++) {
        const b = proc.bindings[i];
        if (b.type == "argument") {
            let value = values[args.length].value;
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
    const cb = (res) => this._callbackExecute(res);
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
                const str = "[" + type + "] " + value.toString();
                return truncate ? MochiKit.Text.truncate(str, 50, "...") : str;
            } catch (e) {
                return "[" + type + "]";
            }
        } else {
            try {
                if (Object.prototype.hasOwnProperty.call(value, "toString")) {
                    const s = value.toString();
                    return truncate ? MochiKit.Text.truncate(s, 50, "...") : s;
                }
            } catch (ignore) {
                // Fallback to key enumeration
            }
            const keys = MochiKit.Base.keys(value);
            return "[Object, " + keys.length + " properties]";
        }
    }
    data = (typeof(data) !== "undefined") ? data : node.data;
    const type = typeName(data);
    if (node.getChildNodes().length > 0) {
        // Do nothing
    } else if (type === "Array" || type === "Object") {
        for (const k in data) {
            let v = data[k];
            if (typeof(v) === "undefined") {
                v = data[parseInt(k, 10)];
            }
            const vt = typeName(v);
            const attrs = { name: k + ": " + dataLabel(vt, v, true) };
            if (vt === "Array" || vt === "Object") {
                attrs.folder = true;
            } else if (v != null) {
                attrs.tooltip = dataLabel(vt, v, false);
            }
            const child = RapidContext.Widget.TreeNode(attrs);
            if (vt === "Array" || vt === "Object") {
                child.data = v;
            }
            node.addAll(child);
        }
    } else {
        const nodeAttrs = {
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
    const node = evt.event().detail.node;
    this._showExecData(node, node.data);
};

/**
 * Creates a new set of batch calls for the current procedure.
 */
AdminApp.prototype._createBatch = function () {
    const proc = this._currentProc;
    const args = [];
    let count = null;
    const values = this._getProcArgs();
    let i, j;
    for (i = 0; i < proc.bindings.length; i++) {
        const b = proc.bindings[i];
        if (b.type == "argument") {
            let value = values[args.length].value;
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
                    const msg = (
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
        const callArgs = [];
        for (j = 0; j < args.length; j++) {
            const arg = args[j];
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
    const msg = "Enter order processing delay (in seconds):";
    let value = prompt(msg, "" + this._batch.delay);
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
    let item;

    if (this._batch.running) {
        item = this._batch.queue.shift();
        if (item == null) {
            this._stopBatch();
        } else {
            const cb = (res) => this._callbackBatch(res);
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
    const done = this._batch.stat.success + this._batch.stat.failed;
    const total = this._batch.queue.length + done;
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
    const data = this.ui.userTable.getSelectedData();
    this.ui.userForm.reset();
    const extra = {
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
    const data = this.ui.userForm.valueMap();
    if (this.ui.userForm.validate()) {
        const desc = data.description;
        const enabled = data.enabled ? "1" : "0";
        const pwd = data.password;
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
    const data = this.ui.logTable.getSelectedData();
    let text;
    if (data == null || data.data == null) {
        text = "<no additional data>";
    } else {
        // TODO: Replace this string splitting with something more dynamic
        const list = data.data.split("\n");
        for (let i = 0; i < list.length; i++) {
            let str = list[i];
            let res = "";
            while (str.length > 1000) {
                let pos = str.lastIndexOf(" ", 1000);
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
    const lines = [];
    str = str || "";
    maxlen = maxlen || 0;
    function splitPush(str) {
        if (!keepspace) {
            str = str.replace(/\s+$/, "");
        }
        let line = null;
        while (maxlen > 0 && str.length > maxlen) {
            line = str.substring(0, maxlen);
            let m = /^\s+/.exec(str.substring(maxlen));
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
    const re = /\n|\r\n|\r/g;
    let pos = 0;
    while (re.exec(str) != null) {
        splitPush(str.substring(pos, re.lastIndex));
        pos = re.lastIndex;
    }
    splitPush(str.substring(pos));
    return lines;
};
