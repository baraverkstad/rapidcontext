/**
 * Creates a new start app.
 */
function StartApp() {
    this.appStatus = {};
    this.focused = true;
    this.showingModifiers = false;
    this.inlinePanes = false;
}

/**
 * Starts the app and initializes the UI.
 */
StartApp.prototype.start = function () {
    this.proc = RapidContext.Procedure.mapAll({
        appList: "System.App.List"
    });

    // General events
    MochiKit.Signal.connect(document, "onkeydown", this, "_handleKeyEvent");
    MochiKit.Signal.connect(document, "onkeyup", this, "_handleKeyEvent");
    MochiKit.Signal.connect(this.ui.root, "onenter", this, "_focusGained");
    MochiKit.Signal.connect(this.ui.root, "onexit", this, "_focusLost");

    // App pane
    RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
    MochiKit.Signal.connect(this.proc.appList, "onsuccess", this, "initApps");
    MochiKit.Signal.connect(this.ui.appTable, "onclick", this, "_handleAppLaunch");

    // Tour wizard
    MochiKit.Signal.connect(this.ui.tourButton, "onclick", this, "tourStart");
    MochiKit.Signal.connect(this.ui.tourWizard, "onclose", this, "tourStop");
    MochiKit.Signal.connect(this.ui.tourWizard, "onchange", this, "tourChange");
    MochiKit.Signal.connect(this.ui.tourStartLocate, "onclick", this, "tourLocateStart");
    MochiKit.Signal.connect(this.ui.tourUserLocate, "onclick", this, "tourLocateUser");
    MochiKit.Signal.connect(this.ui.tourHelpLocate, "onclick", this, "tourLocateHelp");
    MochiKit.Signal.connect(this.ui.tourTabsLocate, "onclick", this, "tourLocateTabs");
    MochiKit.Signal.connect(this.ui.tourAdminLocate, "onclick", this, "tourLocateAdmin");
    MochiKit.Signal.connect(this.ui.tourAdminProcLocate, "onclick", this, "tourLocateAdminProcs");
    MochiKit.Signal.connect(this.ui.tourAdminLogLocate, "onclick", this, "tourLocateAdminLogs");
    MochiKit.Signal.connect(this.ui.tourAdminUserLocate, "onclick", this, "tourLocateAdminUsers");
    MochiKit.Signal.connect(this.ui.tourAdminPluginLocate, "onclick", this, "tourLocateAdminPlugins");
    // TODO: Security test should be made on access, not role name
    var user = RapidContext.App.user();
    if (!user || !user.role || MochiKit.Base.findValue(user.role, "admin") < 0) {
        this.ui.tourWizard.removeChildNode(this.ui.tourWizard.lastChild);
        this.ui.tourWizard.removeChildNode(this.ui.tourWizard.lastChild);
    }

    // Init app list
    this.proc.appList();
}

/**
 * Stops the app.
 */
StartApp.prototype.stop = function () {
    for (var name in this.proc) {
        MochiKit.Signal.disconnectAll(this.proc[name]);
    }
}

/**
 * Initializes the app launchers. If all the apps are already known,
 * this method does nothing. Otherwise the app launcher table will be
 * recreated and new start pane apps launched.
 */
StartApp.prototype.initApps = function () {
    var apps = RapidContext.App.apps();
    var launchers = [];
    var help = null;
    var admin = null;

    // Sort the app launcher list
    for (var i = 0; i < apps.length; i++) {
        var app = apps[i];
        if (app.className === "HelpApp") {
            help = app;
        } else if (app.className === "AdminApp") {
            admin = app;
        } else if (app.launch == "manual" || app.launch == "auto") {
            launchers.push(app);
        } else if (app.startPage && !this.appStatus[app.id]) {
            this.initStartupApp(app);
        }
        this.appStatus[app.id] = true;
    }
    if (help) {
        launchers.push(help);
    }
    if (admin) {
        launchers.push(admin);
    }

    // Redraw the app launcher table
    var rows = [];
    for (var i = 0; i < launchers.length; i++) {
        var app = launchers[i];
        // TODO: Should use a template widget...
        var attrs = { style: { "padding": "4px 6px 6px 6px" } };
        var tdIcon = MochiKit.DOM.TD(attrs);
        if (app.icon) {
            var img = MochiKit.DOM.IMG({ src: app.icon });
            MochiKit.DOM.replaceChildNodes(tdIcon, img);
        }
        var style = { marginLeft: "6px" };
        var iconAttrs = { ref: "EXPAND", tooltip: "Open in new window",
                          style: style };
        var expIcon = RapidContext.Widget.Icon(iconAttrs);
        expIcon.hide();
        var style = { margin: "0", lineHeight: "18px", color: "#1E466E" };
        var title = MochiKit.DOM.H3({ style: style }, app.name, expIcon);
        var desc = MochiKit.DOM.SPAN(null, app.description);
        try {
            desc.style.whiteSpace = "pre-line";
        } catch (ignore) {
            // May throw error in MSIE 7 since the style is unrecognized
        }
        var tdName = MochiKit.DOM.TD(attrs, title, desc);
        var attrs = { "class": "clickable", "data-appid": app.id };
        rows.push(MochiKit.DOM.TR(attrs, tdIcon, tdName));
    }
    MochiKit.DOM.replaceChildNodes(this.ui.appTable, rows);
}

/**
 * Initializes an inline pane auto-start app.
 */
StartApp.prototype.initStartupApp = function (app) {
    if (!this.inlinePanes) {
        this.inlinePanes = true;
        this.ui.inlinePane.removeAll();
    }
    // TODO: use proper widget and container instead
    var style = { "position": "relative", "float": app.startPage,
                  "min-height": "200px", "border": "1px solid #BBBBBB",
                  "padding": "5px" };
    var attrs = { pageTitle: app.name, pageCloseable: true, style: style };
    var pane = new RapidContext.Widget.Pane(attrs);
    this.ui.inlinePane.addAll(pane);
    RapidContext.Util.registerSizeConstraints(pane, "50%-15");
    RapidContext.Util.resizeElements(pane);
    this.startApp(app.className, pane);
}

/**
 * Event handler for the focus gain event (onenter).
 */
StartApp.prototype._focusGained = function (evt) {
    this.focused = true;
}

/**
 * Event handler for the focus lost event (onexit).
 */
StartApp.prototype._focusLost = function (evt) {
    this.focused = false;
    this._showAppModifiers(false);
}

/**
 * Handles global key events. This handler only processes events if
 * the app is currently in focus (as far as can be determined). It
 * currently only handles the launcher modifier keys.
 */
StartApp.prototype._handleKeyEvent = function (evt) {
    if (evt.type() == "keydown" && evt.modifier().any && this.focused && !this.showingModifiers) {
        this.showingModifiers = true;
        this._showAppModifiers(true);
    } else if (evt.type() == "keyup" && this.showingModifiers) {
        this.showingModifiers = false;
        this._showAppModifiers(false);
    }
}

/**
 * Shows or hides the application launcher modifier icons.
 *
 * @param {Boolean} visible the visible flag
 */
StartApp.prototype._showAppModifiers = function (visible) {
    var icons = MochiKit.DOM.getElementsByTagAndClassName(null, "widgetIcon", this.ui.appTable);
    for (var i = 0; i < icons.length; i++) {
        if (visible) {
            icons[i].show();
        } else {
            icons[i].hide();
        }
    }
}

/**
 * Handles an app launch click.
 *
 * @param {Event} evt the click event
 */
StartApp.prototype._handleAppLaunch = function (evt) {
    var tr = evt.target();
    if (tr.tagName != "TR") {
        tr = MochiKit.DOM.getFirstParentByTagAndClassName(tr, 'TR');
    }
    if (tr != null) {
        var appId = MochiKit.DOM.getNodeAttribute(tr, "data-appid");
        if (appId) {
            this.startApp(appId, evt.modifier().any ? window.open() : null);
            this._showAppModifiers(false);
        }
    }
    evt.stop();
}

/**
 * Starts an app with the specified class name.
 *
 * @param {String} app the app id or class name
 * @param {Widget} [container] the optional container widget
 */
StartApp.prototype.startApp = function (app, container) {
    try {
        var d = RapidContext.App.startApp(app, container);
        d.addErrback(RapidContext.UI.showError);
    } catch (e) {
        RapidContext.UI.showError(e);
    }
}

StartApp.prototype.tourStart = function () {
    if (this.ui.tourDialog.isHidden()) {
        document.body.appendChild(this.ui.tourDialog);
        var title = this.ui.tourDialog.firstChild;
        MochiKit.Style.setStyle(title, { background: "#70263e" });
        var div = this.ui.tourDialog.lastChild;
        MochiKit.Style.setStyle(div, { background: "#ffddee" });
        var dim = MochiKit.Style.getViewportDimensions();
        var opts = { effect: "Move", mode: "absolute", duration: 1.5, transition: "spring",
                     x: Math.floor(dim.w * 0.1), y: Math.floor(dim.h - 400) };
        MochiKit.Style.setElementPosition(this.ui.tourDialog, { x: opts.x, y: -200 });
        this.ui.tourDialog.animate(opts);
        this.ui.tourWizard.activatePage(0);
        this.ui.tourDialog.show();
    }
}

/**
 * Starts the guided tour.
 */
StartApp.prototype.tourStop = function () {
    this.ui.tourDialog.hide();
}

/**
 * Stops the guided tour.
 */
StartApp.prototype.tourChange = function () {
    var d = MochiKit.Async.wait(1);
    switch (this.ui.tourWizard.activePageIndex()) {
    case 1:
        d.addBoth(function() {
            return RapidContext.App.callApp("StartApp", "tourLocateStart");
        });
        break;
    case 2:
        d.addBoth(MochiKit.Base.bind("tourLocateUser", this));
        break;
    case 3:
        d.addBoth(function() {
            return RapidContext.App.callApp("HelpApp", "loadTopics");
        });
        d.addBoth(function() {
            return MochiKit.Async.wait(1);
        });
        d.addBoth(MochiKit.Base.bind("tourLocateHelp", this));
        break;
    case 4:
        d.addBoth(MochiKit.Base.bind("tourLocateTabs", this));
        break;
    case 5:
        d.addBoth(function() {
            return RapidContext.App.callApp("AdminApp", "loadProcedures");
        });
        d.addBoth(function() {
            return MochiKit.Async.wait(1);
        });
        d.addBoth(MochiKit.Base.bind("tourLocateAdmin", this));
        break;
    case 6:
        d.addBoth(MochiKit.Base.bind("tourLocateAdminProcs", this));
        break;
    case 7:
        d.addBoth(MochiKit.Base.bind("tourLocateAdminLogs", this));
        break;
    case 8:
        d.addBoth(MochiKit.Base.bind("tourLocateAdminUsers", this));
        break;
    case 9:
        d.addBoth(MochiKit.Base.bind("tourLocateAdminPlugins", this));
        break;
    }
    d.addErrback(RapidContext.UI.showError);
}

/**
 * Locates the start app.
 */
StartApp.prototype.tourLocateStart = function () {
    this.tourLocate(this.ui.appTable);
}

/**
 * Locates the user menu.
 */
StartApp.prototype.tourLocateUser = function () {
    var menu = RapidContext.App._UI.menu;
    if (menu.isHidden()) {
        RapidContext.App._UI.menu.show();
        var func = MochiKit.Base.bind("tourLocateUser", this);
        window.setTimeout(func, 500);
    } else {
        RapidContext.App._UI.menu.show();
        this.tourLocate(menu);
    }
}

/**
 * Locates the help app.
 */
StartApp.prototype.tourLocateHelp = function () {
    var tab = RapidContext.App._UI.container.selectedChild();
    var box = this.getBoundingBox(tab.firstChild.nextSibling);
    var diag = this.getBoundingBox(this.ui.tourDialog);
    box.h = diag.y - box.y - 100;
    this.tourLocate(box);
}

/**
 * Locates the app tabs.
 */
StartApp.prototype.tourLocateTabs = function () {
    var tabs = RapidContext.App._UI.container.firstChild;
    var box = this.getBoundingBox(tabs.firstChild, tabs.lastChild);
    box.h += 30;
    this.tourLocate(box);
}

/**
 * Locates the admin app.
 */
StartApp.prototype.tourLocateAdmin = function () {
    var tab = RapidContext.App._UI.container.selectedChild();
    var box = this.getBoundingBox(tab);
    var diag = this.getBoundingBox(this.ui.tourDialog);
    box.h = diag.y - box.y - 100;
    this.tourLocate(box);
}

/**
 * Locates the admin procedures.
 */
StartApp.prototype.tourLocateAdminProcs = function () {
    var res = this.tourGetAdminTab(3);
    res.container.selectChild(res.tab);
    var tree = res.tab.firstChild.lastChild;
    var node = tree.findByPath("System.Session.Current".split("."));
    node.select();
    var box = this.getBoundingBox(res.tab.firstChild.nextSibling);
    var diag = this.getBoundingBox(this.ui.tourDialog);
    box.h = diag.y - box.y - 100;
    this.tourLocate(box);
}

/**
 * Locates the admin logs.
 */
StartApp.prototype.tourLocateAdminLogs = function () {
    var res = this.tourGetAdminTab(6);
    res.container.selectChild(res.tab);
    var box = this.getBoundingBox(res.tab.firstChild.nextSibling);
    var diag = this.getBoundingBox(this.ui.tourDialog);
    box.h = diag.y - box.y - 100;
    this.tourLocate(box);
}

/**
 * Locates the admin users.
 */
StartApp.prototype.tourLocateAdminUsers = function () {
    var res = this.tourGetAdminTab(5);
    res.container.selectChild(res.tab);
    var box = this.getBoundingBox(res.tab);
    var diag = this.getBoundingBox(this.ui.tourDialog);
    box.h = diag.y - box.y - 100;
    this.tourLocate(box);
}

/**
 * Locates the admin plug-ins.
 */
StartApp.prototype.tourLocateAdminPlugins = function () {
    var res = this.tourGetAdminTab(2);
    res.container.selectChild(res.tab);
    var box = this.getBoundingBox(res.tab.firstChild);
    var diag = this.getBoundingBox(this.ui.tourDialog);
    box.h = diag.y - box.y - 100;
    this.tourLocate(box);
}

/**
 * Locates the specified DOM nodes.
 *
 * @param {Node} ... the DOM node elements to locate
 */
StartApp.prototype.tourLocate = function () {
    document.body.appendChild(this.ui.tourLocator);
    this.ui.tourLocator.animate({ effect: "cancel" });
    var box = this.getBoundingBox(this.ui.tourDialog);
    MochiKit.Style.setElementDimensions(this.ui.tourLocator, box);
    MochiKit.Style.setElementPosition(this.ui.tourLocator, box);
    MochiKit.Style.setOpacity(this.ui.tourLocator, 0.3);
    MochiKit.Style.showElement(this.ui.tourLocator);
    var box = this.getBoundingBox.apply(this, arguments);
    var style = { left: box.x + "px", top: box.y + "px",
                  width: box.w + "px", height: box.h + "px" };
    this.ui.tourLocator.animate({ effect: "Morph", duration: 3.0,
                                  transition: "spring", style: style });
    this.ui.tourLocator.animate({ effect: "fade", delay: 2.4, queue: "parallel" });
}

/**
 * Returns a UI tab from the admin app.
 *
 * @param {Number} idx the tab index
 *
 * @return {Object} a result object with 'tab' and 'container'
 *             properties
 */
StartApp.prototype.tourGetAdminTab = function (idx) {
    // TODO: this accesses internal UI from the app...
    var tab = RapidContext.App._UI.container.selectedChild();
    var container = tab.lastChild;
    var children = container.getChildNodes();
    if (children.length < 6 && idx > 4) {
        idx--;
    }
    if (children.length < 6 && idx > 1) {
        idx--;
    }
    tab = children[idx];
    return { container: container, tab: tab };
}

/**
 * Returns the bounding box for the specified DOM nodes.
 *
 * @param {Node} ... the DOM node elements to locate
 *
 * @return {Object} the bounding box
 */
StartApp.prototype.getBoundingBox = function () {
    var box = null;
    for (var i = 0; i < arguments.length; i++) {
        var elem = arguments[i];
        var elemBox = null;
        if (RapidContext.Util.isDOM(elem)) {
            elemBox = MochiKit.Style.getElementPosition(elem);
            MochiKit.Base.update(elemBox, MochiKit.Style.getElementDimensions(elem));
        } else if (elem != null && typeof(elem.x) == "number") {
            elemBox = MochiKit.Base.update({ x: 0, y: 0, w: 0, h: 0 }, elem);
        }
        if (elemBox != null && box == null) {
            box = elemBox;
        } else if (elemBox != null) {
            var xMin = Math.min(box.x, elemBox.x);
            var xMax = Math.max(box.x + box.w, elemBox.x + elemBox.w);
            var yMin = Math.min(box.y, elemBox.y);
            var yMax = Math.max(box.y + box.h, elemBox.y + elemBox.h);
            box.x = xMin;
            box.w = xMax - xMin;
            box.y = yMin;
            box.h = yMax - yMin;
        }
    }
    return box;
}
