/**
 * Creates a new start app.
 */
function StartApp() {
    this.inlinePanes = false;
}

/**
 * Starts the app and initializes the UI.
 */
StartApp.prototype.start = function () {
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
    var a = RapidContext.App.apps();
    var help = null;
    var admin = null;
    var manualLaunch = { manual: true, auto: true };
    for (var i = 0; i < a.length; i++) {
        if (a[i].className === "HelpApp") {
            help = a[i];
        } else if (a[i].className === "AdminApp") {
            admin = a[i];
        } else if (a[i].launch in manualLaunch) {
            this.addApp(a[i]);
        } else if (a[i].startPage) {
            this.startApp(a[i].className,
                             this._createInlinePane(a[i].name, a[i].startPage));
        }
    }
    if (help) {
        this.addApp(help);
    }
    if (admin) {
        this.addApp(admin);
    }
    if (MochiKit.Base.findValue(RapidContext.App.user().role, "Admin") < 0) {
        this.ui.tourWizard.removeChildNode(this.ui.tourWizard.lastChild);
        this.ui.tourWizard.removeChildNode(this.ui.tourWizard.lastChild);
    }
}

/**
 * Stops the app.
 */
StartApp.prototype.stop = function () {
    // Nothing to do here
}

/**
 * Adds the specified app launcher to the list of available
 * apps.
 *
 * @param {Object} app the app launcher to add
 */
StartApp.prototype.addApp = function (app) {
    var launcher = MochiKit.Base.bind("startApp", this, app.className, null);
    if (app.icon) {
        var imgLink = MochiKit.DOM.A({ href: "#" }, MochiKit.DOM.IMG({ src: app.icon }));
        MochiKit.Signal.connect(imgLink, "onclick", launcher);
    }
    var nameLink = MochiKit.DOM.A({ href: "#" }, app.name);
    MochiKit.Signal.connect(nameLink, "onclick", launcher);
    var attrs = { style: { "padding-right": "10px", "padding-bottom": "10px" } };
    var tr = MochiKit.DOM.TR(null,
                             MochiKit.DOM.TD(attrs, imgLink),
                             MochiKit.DOM.TD(attrs, nameLink, " - ", app.description));
    this.ui.appTable.appendChild(tr);
}

/**
 * Starts an app with the specified class name.
 *
 * @param {String} className the app class name
 * @param {Widget} [container] the optional container widget
 * @param {Event} [evt] the optional mouse click event
 */
StartApp.prototype.startApp = function (className, container, evt) {
    try {
        var d = RapidContext.App.startApp(className, container);
        d.addErrback(RapidContext.UI.showError);
    } catch (e) {
        RapidContext.UI.showError(e);
    }
    if (evt) {
        evt.stop();
    }
}

/**
 * Creates a pane widget in the inline area.
 *
 * @param {String} title the widget title
 * @param {String} position the widget position ("left" or "right")
 *
 * @return {Widget} the widget DOM node
 */
StartApp.prototype._createInlinePane = function (name, position) {
    if (!this.inlinePanes) {
        this.inlinePanes = true;
        MochiKit.DOM.replaceChildNodes(this.ui.inlineArea);
    }
    // TODO: use proper widget and container instead
    var style = { "position": "relative", "float": position,
                  "min-height": "200px",
                  "border": "1px solid #bbbbbb", "padding": "5px" };
    var attrs = { pageTitle: name, pageCloseable: true, style: style };
    var pane = new RapidContext.Widget.Pane(attrs);
    this.ui.inlineArea.appendChild(pane);
    RapidContext.Util.registerSizeConstraints(pane, "50%-15");
    RapidContext.Util.resizeElements(pane);
    return pane;
}

StartApp.prototype.tourStart = function () {
    if (this.ui.tourDialog.isHidden()) {
        document.body.appendChild(this.ui.tourDialog);
        var title = this.ui.tourDialog.firstChild;
        MochiKit.Style.setStyle(title, { background: "#70263e" });
        var close = title.nextSibling;
        close.setAttrs({ url: "close-red.gif" });
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
    var res = this.tourGetAdminTab(2);
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
    var res = this.tourGetAdminTab(5);
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
    var res = this.tourGetAdminTab(4);
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
    var res = this.tourGetAdminTab(1);
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
