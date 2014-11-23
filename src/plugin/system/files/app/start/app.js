/**
 * Creates a new start app.
 */
function StartApp() {
    this.appStatus = {};
    this.gradients = ["red", "green", "blue"];
    this.focused = true;
    this.showingModifiers = false;
}

/**
 * Starts the app and initializes the UI.
 */
StartApp.prototype.start = function () {
    this.proc = RapidContext.Procedure.mapAll({
        appList: "System.App.List",
        changePassword: "System.User.ChangePassword",
        sessionInfo: "System.Session.Current"
    });
    var status = RapidContext.App.status();

    // General events
    MochiKit.Signal.connect(document, "onkeydown", this, "_handleKeyEvent");
    MochiKit.Signal.connect(document, "onkeyup", this, "_handleKeyEvent");
    MochiKit.Signal.connect(this.ui.root, "onenter", this, "_focusGained");
    MochiKit.Signal.connect(this.ui.root, "onexit", this, "_focusLost");

    // Info bar & popup menu
    this._initInfoMenu();
    var env = status.environment;
    env = (env && env.name) ? env.name : "<none>";
    MochiKit.DOM.replaceChildNodes(this.ui.infoEnv, env);
    var show = { effect: "appear", duration: 0.2 };
    var hide = { effect: "fade", duration: 0.2, delay: 0.2 };
    this.ui.menu.setAttrs({ showAnim: show, hideAnim: hide });
    if (/MSIE/.test(navigator.userAgent)) {
        // TODO: MSIE 6.0 sets div width to 100%, so we hack the width
        this.ui.menu.style.width = "250px";
    }
    MochiKit.Signal.connect(this.proc.sessionInfo, "onsuccess", this, "_initInfoMenu");
    MochiKit.Signal.connect(this.ui.infoBar, "onmousemove", this.ui.menu, "show");
    MochiKit.Signal.connect(this.ui.infoBar, "onmouseleave", this.ui.menu, "hide");
    MochiKit.Signal.connect(this.ui.menu, "onmouseleave", this.ui.menu, "hide");
    MochiKit.Signal.connect(this.ui.menu, "onclick", this, "_hideInfoPopup");
    MochiKit.Signal.connect(this.ui.menuAbout, "onclick", this.ui.about, "show");
    var func = MochiKit.Base.partial(RapidContext.App.startApp, "help", null);
    MochiKit.Signal.connect(this.ui.menuHelp, "onclick", func);
    MochiKit.Signal.connect(this.ui.menuLogInOut, "onclick", this, "_loginOut");

    // App pane
    RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
    MochiKit.Signal.connect(this.proc.appList, "onsuccess", this, "_initApps");
    MochiKit.Signal.connect(this.ui.appTable, "onclick", this, "_handleAppLaunch");

    // About dialog
    MochiKit.Signal.connect(this.ui.aboutClose, "onclick", this.ui.about, "hide");
    var version = MochiKit.Text.format("{version} ({date})", status);
    MochiKit.DOM.replaceChildNodes(this.ui.aboutVersion, version);

    // Password dialog
    MochiKit.Signal.connect(this.ui.passwordCancel, "onclick", this.ui.passwordDialog, "hide");
    MochiKit.Signal.connect(this.ui.passwordSave, "onclick", this, "_changePassword");
    MochiKit.Signal.connect(this.proc.changePassword, "onresponse", this, "_changePasswordCallback");

    // Login dialog
    MochiKit.Signal.connect(this.ui.loginCancel, "onclick", this.ui.loginDialog, "hide");
    MochiKit.Signal.connect(this.ui.loginAuth, "onclick", this, "_loginAuth");

    // Tour wizard
    MochiKit.Signal.connect(this.ui.tourButton, "onclick", this, "_tourStart");
    MochiKit.Signal.connect(this.ui.tourWizard, "onclose", this, "_tourStop");
    MochiKit.Signal.connect(this.ui.tourWizard, "onchange", this, "_tourChange");
    MochiKit.Signal.connect(this.ui.tourStartLocate, "onclick", this, "_tourLocateStart");
    MochiKit.Signal.connect(this.ui.tourUserLocate, "onclick", this, "_tourLocateUser");
    MochiKit.Signal.connect(this.ui.tourHelpLocate, "onclick", this, "_tourLocateHelp");
    MochiKit.Signal.connect(this.ui.tourTabsLocate, "onclick", this, "_tourLocateTabs");

    // Init app list
    this.proc.appList();
};

/**
 * Stops the app.
 */
StartApp.prototype.stop = function () {
    for (var name in this.proc) {
        MochiKit.Signal.disconnectAll(this.proc[name]);
    }
};

/**
 * Initializes the info bar and popup menu.
 */
StartApp.prototype._initInfoMenu = function () {
    var user = RapidContext.App.user();
    MochiKit.Signal.disconnect(this.ui.menuAdmin, "onclick");
    MochiKit.Signal.disconnect(this.ui.menuPassword, "onclick");
    if (user && user.id) {
        MochiKit.DOM.replaceChildNodes(this.ui.infoUser, user.name || user.id);
        MochiKit.DOM.replaceChildNodes(this.ui.menuTitle, user.longName);
        MochiKit.DOM.replaceChildNodes(this.ui.menuLogInOut, "Logout");
        MochiKit.DOM.removeElementClass(this.ui.menuAdmin, "widgetPopupDisabled");
        MochiKit.DOM.removeElementClass(this.ui.menuPassword, "widgetPopupDisabled");
        var func = MochiKit.Base.partial(RapidContext.App.startApp, "admin", null);
        MochiKit.Signal.connect(this.ui.menuAdmin, "onclick", func);
        MochiKit.Signal.connect(this.ui.menuPassword, "onclick", this, "_showPasswordDialog");
    } else {
        MochiKit.DOM.replaceChildNodes(this.ui.infoUser, "anonymous");
        MochiKit.DOM.replaceChildNodes(this.ui.menuTitle, "Anonymous User");
        MochiKit.DOM.replaceChildNodes(this.ui.menuLogInOut, "Login");
        MochiKit.DOM.addElementClass(this.ui.menuAdmin, "widgetPopupDisabled");
        MochiKit.DOM.addElementClass(this.ui.menuPassword, "widgetPopupDisabled");
    }
};

/**
 * Initializes the app launcher pane and handles app auto-starts.
 * This method is safe to call multiple times, such as when the app
 * list is refreshed.
 */
StartApp.prototype._initApps = function () {
    var apps = RapidContext.App.apps();
    var instances = 0;
    var launchers = [];
    var help = null;
    var admin = null;

    // Find app instances and manual launchers
    for (var i = 0; i < apps.length; i++) {
        var app = apps[i];
        if (app.instances) {
            instances += app.instances.length;
        }
        if (app.id === "help") {
            help = app;
        } else if (app.id === "admin") {
            admin = app;
        } else if (app.launch == "auto" || app.launch == "manual" || app.launch == "window") {
            launchers.push(app);
        }
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
            MochiKit.DOM.replaceChildNodes(tdIcon, app.icon.cloneNode(true));
        }
        var style = { marginLeft: "6px" };
        var iconAttrs = { ref: "EXPAND", tooltip: "Open in new window",
                          style: style };
        var expIcon = RapidContext.Widget.Icon(iconAttrs);
        if (app.launch == "window") {
            expIcon.addClass("launch-window");
        } else {
            expIcon.hide();
        }
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

    // Start auto and inline apps
    for (var i = 0; i < apps.length; i++) {
        var app = apps[i];
        if (this.appStatus[app.id] || instances > 1) {
            // Do nothing, auto-startup disabled
        } else if (app.instances && app.instances.length) {
            // Do nothing, app already running
        } else if (app.startPage) {
            this._initStartupApp(app);
        } else if (app.launch == "auto" || app.launch == "once") {
            RapidContext.App.startApp(app);
        }
        this.appStatus[app.id] = true;
    }
};

/**
 * Initializes an inline pane auto-start app.
 */
StartApp.prototype._initStartupApp = function (app) {
    // TODO: use proper widget and container instead
    var style = {};
    if (app.startPage == "left" || app.startPage == "right") {
        style = { float: app.startPage, clear: app.startPage };
    } else {
        style = { clear: "both" };
    }
    var color = this.gradients.shift() || "grey";
    var attrs = { pageTitle: app.name, pageCloseable: true,
                  "class": "startApp-inline " + color, style: style };
    var pane = new RapidContext.Widget.Pane(attrs);
    this.ui.inlinePane.insertBefore(pane, this.ui.inlinePane.firstChild);
    if (app.startPage == "left" || app.startPage == "right") {
        RapidContext.Util.registerSizeConstraints(pane, "50%-25");
    } else {
        RapidContext.Util.registerSizeConstraints(pane, "100%");
    }
    RapidContext.Util.resizeElements(pane);
    this.startApp(app.className, pane);
};

/**
 * Event handler for the focus gain event (onenter).
 */
StartApp.prototype._focusGained = function (evt) {
    this.focused = true;
};

/**
 * Event handler for the focus lost event (onexit).
 */
StartApp.prototype._focusLost = function (evt) {
    this.focused = false;
    this._showAppModifiers(false);
};

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
};

/**
 * Shows or hides the application launcher modifier icons.
 *
 * @param {Boolean} visible the visible flag
 */
StartApp.prototype._showAppModifiers = function (visible) {
    var icons = $(this.ui.appTable).find(".widgetIcon").not(".launch-window");
    for (var i = 0; i < icons.length; i++) {
        if (visible) {
            icons[i].show();
        } else {
            icons[i].hide();
        }
    }
};

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
            var app = RapidContext.App.findApp(appId);
            var win = evt.modifier().any || (app && app.launch == "window");
            this.startApp(appId, win ? window.open() : null);
            this._showAppModifiers(false);
        }
    }
    evt.stop();
};

/**
 * Initializes (and optionally creates) a new app pane.
 *
 * @param {Widget} [pane] the optional container pane (null to create)
 * @param {Object} [opts] the options for the pane to create
 * @param {String} [opts.title] the pane title in the app switcher
 * @param {Boolean} [opts.closeable] the closeable flag (defaults to true)
 * @param {Boolean} [opts.background] the background flag (defaults to false)
 * @return {Object} a UI object with "root" and "overlay" properties
 */
StartApp.prototype.initAppPane = function (pane, opts) {
    opts = opts || {};
    if (pane == null) {
        var style = { position: "relative" };
        var attrs = { pageTitle: opts.title, pageCloseable: opts.closeable, style: style };
        pane = new RapidContext.Widget.Pane(attrs);
        RapidContext.Util.registerSizeConstraints(pane, "100%", "100%");
        this.ui.tabContainer.addAll(pane);
        if (!opts.background) {
            this.ui.tabContainer.selectChild(pane);
        }
    }
    var overlay = new RapidContext.Widget.Overlay({ message: "Loading app..." });
    MochiKit.DOM.replaceChildNodes(pane, overlay);
    return { root: pane, overlay: overlay };
};

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
};

/**
 * Hides the info popup menu instantly.
 */
StartApp.prototype._hideInfoPopup = function() {
    this.ui.menu.setAttrs({ hideAnim: { effect: "fade", duration: 0 } });
    this.ui.menu.hide();
    this.ui.menu.setAttrs({ hideAnim: { effect: "fade", duration: 0.2, delay: 0.2 } });
};

/**
 * Shows the password change dialog.
 */
StartApp.prototype._showPasswordDialog = function () {
    this.ui.passwordForm.reset();
    this.ui.passwordDialog.show();
    this.ui.passwordDialog.resizeToContent();
    this.ui.passwordCurrent.focus();
};

/**
 * Changes the user password (from the dialog).
 */
StartApp.prototype._changePassword = function () {
    var data = this.ui.passwordForm.valueMap();
    if (data.password != data.passwordcheck) {
        data.passwordcheck = "";
    }
    this.ui.passwordForm.update(data);
    if (this.ui.passwordForm.validate()) {
        var user = RapidContext.App.user();
        var prefix = user.id + ":" + user.realm + ":";
        var oldHash = CryptoJS.MD5(prefix + data.current).toString();
        var newHash = CryptoJS.MD5(prefix + data.password).toString();
        this.proc.changePassword(oldHash, newHash);
        this.ui.passwordSave.setAttrs({ disabled: true, icon: "LOADING" });
    }
    this.ui.passwordDialog.resizeToContent();
};

/**
 * Callback for the password change dialog.
 */
StartApp.prototype._changePasswordCallback = function (res) {
    this.ui.passwordSave.setAttrs({ disabled: false, icon: "OK" });
    if (res instanceof Error) {
        this.ui.passwordError.addError(this.ui.passwordCurrent, res.message);
        this.ui.passwordDialog.resizeToContent();
    } else {
        this.ui.passwordDialog.hide();
        this.ui.passwordForm.reset();
    }
};

/**
 * Shows either the login or the logout dialog. In the latter case, the
 * session is also terminated.
 */
StartApp.prototype._loginOut = function () {
    var user = RapidContext.App.user();
    if (user && user.id) {
        RapidContext.App.logout(false);
        this.ui.logoutDialog.show();
        this.ui.logoutDialog.resizeToContent();
    } else {
        this.ui.loginForm.reset();
        this.ui.loginDialog.show();
        this.ui.loginDialog.resizeToContent();
        this.ui.loginUser.focus();
        this.proc.sessionInfo();
    }
};

/**
 * Shows the login authentication dialog.
 */
StartApp.prototype._loginAuth = function () {
    if (this.ui.loginForm.validate()) {
        this.ui.loginAuth.setAttrs({ disabled: true, icon: "LOADING" });
        var data = this.ui.loginForm.valueMap();
        var d = RapidContext.App.login($.trim(data.user), data.password);
        d.addBoth(MochiKit.Base.bind("_loginAuthCallback", this));
    }
    this.ui.loginDialog.resizeToContent();
};

/**
 * Callback for the login authentication.
 */
StartApp.prototype._loginAuthCallback = function (res) {
    this.ui.loginAuth.setAttrs({ disabled: false, icon: "OK" });
    if (res instanceof Error) {
        this.ui.loginPasswordError.addError(this.ui.loginPassword, res.message);
        this.ui.loginDialog.resizeToContent();
        return false;
    } else {
        this.ui.loginDialog.hide();
        this.ui.loginForm.reset();
        this.proc.appList();
        this.proc.sessionInfo();
        return true;
    }
};

/**
 * Starts the guided tour.
 */
StartApp.prototype._tourStart = function () {
    if (this.ui.tourDialog.isHidden()) {
        document.body.appendChild(this.ui.tourDialog);
        var dim = MochiKit.Style.getViewportDimensions();
        var opts = { effect: "Move", mode: "absolute", duration: 1.5, transition: "spring",
                     x: Math.floor(dim.w * 0.1), y: Math.floor(dim.h - 400) };
        MochiKit.Style.setElementPosition(this.ui.tourDialog, { x: opts.x, y: -200 });
        this.ui.tourDialog.animate(opts);
        this.ui.tourWizard.activatePage(0);
        this.ui.tourDialog.show();
    }
};

/**
 * Stops the guided tour.
 */
StartApp.prototype._tourStop = function () {
    this.ui.tourDialog.hide();
};

/**
 * Changes the active page in the guided tour.
 */
StartApp.prototype._tourChange = function () {
    var d = MochiKit.Async.wait(1);
    switch (this.ui.tourWizard.activePageIndex()) {
    case 1:
        d.addBoth(function() {
            return RapidContext.App.callApp("StartApp", "_tourLocateStart");
        });
        break;
    case 2:
        d.addBoth(function() {
            return RapidContext.App.callApp("help", "loadTopics");
        });
        d.addBoth(function() {
            return MochiKit.Async.wait(1);
        });
        d.addBoth(MochiKit.Base.bind("_tourLocateHelp", this));
        break;
    case 3:
        d.addBoth(MochiKit.Base.bind("_tourLocateTabs", this));
        break;
    case 4:
        d.addBoth(MochiKit.Base.bind("_tourLocateUser", this));
        break;
    }
    d.addErrback(RapidContext.UI.showError);
};

/**
 * Locates the start app.
 */
StartApp.prototype._tourLocateStart = function () {
    this._tourLocate(this.ui.appTable);
};

/**
 * Locates the help app.
 */
StartApp.prototype._tourLocateHelp = function () {
    var tab = this.ui.tabContainer.selectedChild();
    var box = this._getBoundingBox(tab.firstChild.nextSibling);
    var diag = this._getBoundingBox(this.ui.tourDialog);
    box.h = 200;
    this._tourLocate(box);
};

/**
 * Locates the app tabs.
 */
StartApp.prototype._tourLocateTabs = function () {
    var tabs = this.ui.tabContainer.firstChild;
    var box = this._getBoundingBox(tabs.firstChild, tabs.lastChild);
    box.h += 30;
    this._tourLocate(box);
};

/**
 * Locates the user menu.
 */
StartApp.prototype._tourLocateUser = function () {
    if (this.ui.menu.isHidden()) {
        this.ui.menu.show();
        var func = MochiKit.Base.bind("_tourLocateUser", this);
        setTimeout(func, 500);
    } else {
        this.ui.menu.show();
        this._tourLocate(this.ui.menu);
    }
};

/**
 * Locates the specified DOM nodes.
 *
 * @param {Node} ... the DOM node elements to locate
 */
StartApp.prototype._tourLocate = function () {
    document.body.appendChild(this.ui.tourLocator);
    this.ui.tourLocator.animate({ effect: "cancel" });
    var box = this._getBoundingBox(this.ui.tourDialog);
    MochiKit.Style.setElementDimensions(this.ui.tourLocator, box);
    MochiKit.Style.setElementPosition(this.ui.tourLocator, box);
    MochiKit.Style.setOpacity(this.ui.tourLocator, 0.3);
    MochiKit.Style.showElement(this.ui.tourLocator);
    var box = this._getBoundingBox.apply(this, arguments);
    var style = { left: box.x + "px", top: box.y + "px",
                  width: box.w + "px", height: box.h + "px" };
    this.ui.tourLocator.animate({ effect: "Morph", duration: 3.0,
                                  transition: "spring", style: style });
    this.ui.tourLocator.animate({ effect: "fade", delay: 2.4, queue: "parallel" });
};

/**
 * Returns the bounding box for the specified DOM nodes.
 *
 * @param {Node} ... the DOM node elements to locate
 *
 * @return {Object} the bounding box
 */
StartApp.prototype._getBoundingBox = function () {
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
};
