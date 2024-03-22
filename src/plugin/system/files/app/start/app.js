/**
 * Creates a new start app.
 */
function StartApp() {
    this.appStatus = {};
    this.gradients = ["yellow", "red", "green", "blue"];
    this.focused = true;
    this.showingModifiers = false;
    this.showingInlinePanes = false;
}

/**
 * Starts the app and initializes the UI.
 */
StartApp.prototype.start = function () {
    this.proc = RapidContext.Procedure.mapAll({
        appList: "system/app/list",
        changePassword: "system/user/changepassword",
        sessionInfo: "system/session/current"
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
    this.ui.infoEnv.innerText = env;
    MochiKit.Signal.connect(this.proc.sessionInfo, "onsuccess", this, "_initInfoMenu");
    MochiKit.Signal.connect(this.ui.infoBar, "onmousemove", this.ui.menu, "show");
    MochiKit.Signal.connect(this.ui.infoBar, "onmouseleave", this.ui.menu, "hide");
    MochiKit.Signal.connect(this.ui.menu, "onmouseleave", this.ui.menu, "hide");
    MochiKit.Signal.connect(this.ui.menu, "onmenuselect", this, "_popupSelect");

    // App pane
    RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
    MochiKit.Signal.connect(this.proc.appList, "onsuccess", this, "_initApps");
    MochiKit.Signal.connect(this.ui.appTable, "onclick", this, "_handleAppLaunch");

    // About dialog
    var version = MochiKit.Text.format("{version} ({date})", status);
    this.ui.aboutVersion.innerText = version;

    // Password dialog
    MochiKit.Signal.connect(this.ui.passwordForm, "onsubmit", this, "_changePassword");
    MochiKit.Signal.connect(this.proc.changePassword, "onresponse", this, "_changePasswordCallback");
    this.ui.passwordForm.addValidator("passwordcheck", function (value, field, form) {
        return value === form.elements["password"].value;
    });

    // Login dialog
    MochiKit.Signal.connect(this.ui.loginForm, "onsubmit", this, "_loginAuth");

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
    if (user && user.id) {
        this.ui.infoUser.innerText = user.name || user.id;
        this.ui.menuTitle.innerText = user.longName;
        this.ui.menuLogInOut.innerText = "Logout";
    } else {
        this.ui.infoUser.innerText = "anonymous";
        this.ui.menuTitle.innerText = "Anonymous User";
        this.ui.menuLogInOut.innerText = "Login";
    }
    this.ui.menuPassword.classList.toggle("disabled", !user || user.type != "user");
};

/**
 * Initializes the app launcher pane and handles app auto-starts.
 * This method is safe to call multiple times, such as when the app
 * list is refreshed.
 */
StartApp.prototype._initApps = function () {
    let apps = RapidContext.App.apps();

    // Hide help and admin apps menu items if not available
    let launchers = RapidContext.Data.object("id", apps);
    this.ui.menuHelp.classList.toggle("disabled", !launchers.help);
    this.ui.menuAdmin.classList.toggle("disabled", !launchers.admin);

    // Redraw the app launcher table
    let $appTable = $(this.ui.appTable).empty();
    let sortKey = (a) => (a.sort || a.id).toLowerCase();
    let isListed = (a) => a.launch == "auto" || a.launch == "manual" || a.launch == "window";
    RapidContext.Data.sort(sortKey, apps).filter(isListed).forEach(function (app) {
        let $tr = $("<tr>").addClass("clickable").attr("data-appid", app.id).appendTo($appTable);
        let icon = app.icon && app.icon.cloneNode(true);
        $("<td class='p-2'>").append(icon).appendTo($tr);
        let ext = RapidContext.Widget.Icon("fa fa-external-link-square ml-1");
        ext.addClass((app.launch == "window") ? "launch-window" : "hidden");
        let $title = $("<a href='#' class='h4 m-0'>").text(app.name).append(ext);
        let $desc = $("<span class='text-pre-wrap'>").text(app.description);
        $("<td class='pl-1 pr-2 py-2'>").append($title, $desc).appendTo($tr);
    });

    // Start auto and inline apps
    let instances = apps.reduce((c, a) => c + a.instances.length, 0);
    for (let app of apps) {
        if (this.appStatus[app.id] || instances > 1) {
            // Do nothing, auto-startup disabled
        } else if (app.instances && app.instances.length) {
            // Do nothing, app already running
        } else if (app.startPage) {
            this._initDashboardApp(app);
        } else if (app.launch == "auto" || app.launch == "once") {
            RapidContext.App.startApp(app);
        }
        this.appStatus[app.id] = true;
    }
};

/**
 * Initializes a dashboard app.
 */
StartApp.prototype._initDashboardApp = function (app) {
    if (!this.showingInlinePanes) {
        this.showingInlinePanes = true;
        this.ui.inlinePane.removeAll();
    }
    var cls = ["dash-item", "box", this.gradients.shift() || "grey"];
    if (app.startPage == "left" || app.startPage == "right") {
        cls.push("float-" + app.startPage, "clear-" + app.startPage);
    } else {
        cls.push("clear-both");
    }
    var attrs = {
        pageTitle: app.name,
        pageCloseable: true,
        "class": cls.join(" ")
    };
    var pane = new RapidContext.Widget.Pane(attrs);
    this.ui.inlinePane.insertBefore(pane, this.ui.inlinePane.firstChild);
    if (app.startPage == "left" || app.startPage == "right") {
        pane.style.width = "calc(50% - 3rem - 3px)";
    } else {
        pane.style.width = "calc(100% - 3rem - 3px)";
    }
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
 * @param {boolean} visible the visible flag
 */
StartApp.prototype._showAppModifiers = function (visible) {
    var icons = $(this.ui.appTable).find("a > i").not(".launch-window");
    icons.toggleClass("hidden", !visible);
};

/**
 * Handles an app launch click.
 *
 * @param {Event} evt the click event
 */
StartApp.prototype._handleAppLaunch = function (evt) {
    var $tr = $(evt.target()).closest("tr");
    var appId = $tr.data("appid");
    if (appId) {
        var app = RapidContext.App.findApp(appId);
        var win = evt.modifier().any || (app && app.launch == "window");
        this.startApp(appId, win ? window.open() : null);
        this._showAppModifiers(false);
    }
    evt.stop();
};

/**
 * Initializes (and optionally creates) a new app pane.
 *
 * @param {Widget} [pane] the optional container pane (null to create)
 * @param {Object} [opts] the options for the pane to create
 * @param {string} [opts.title] the pane title in the app switcher
 * @param {boolean} [opts.closeable] the closeable flag (defaults to true)
 * @param {boolean} [opts.background] the background flag (defaults to false)
 * @return {Object} a UI object with "root" and "overlay" properties
 */
StartApp.prototype.initAppPane = function (pane, opts) {
    opts = opts || {};
    if (pane == null) {
        var attrs = {
            pageTitle: opts.title,
            pageCloseable: opts.closeable,
            "class": "position-relative"
        };
        pane = new RapidContext.Widget.Pane(attrs);
        pane.style.width = pane.style.height = "100%";
        this.ui.tabContainer.addAll(pane);
        if (!opts.background) {
            this.ui.tabContainer.selectChild(pane);
        }
    }
    pane.innerHTML = "";
    var overlay = new RapidContext.Widget.Overlay({ message: "Loading app..." });
    pane.append(overlay);
    return { root: pane, overlay: overlay };
};

/**
 * Starts an app with the specified class name.
 *
 * @param {string} app the app id or class name
 * @param {Widget} [container] the optional container widget
 */
StartApp.prototype.startApp = function (app, container) {
    RapidContext.App.startApp(app, container).catch(RapidContext.UI.showError);
};

/**
 * Handles a popup menu item selection.
 */
StartApp.prototype._popupSelect = function (evt) {
    this.ui.menu.hide();
    switch (evt.event().detail.item.dataset.action) {
    case "about":
        this.ui.about.show();
        break;
    case "help":
        RapidContext.App.startApp("help");
        break;
    case "admin":
        RapidContext.App.startApp("admin");
        break;
    case "password":
        this.ui.passwordForm.reset();
        this.ui.passwordDialog.show();
        this.ui.passwordCurrent.focus();
        break;
    case "login":
        this._loginOut();
        break;
    }
};

/**
 * Changes the user password (from the dialog).
 */
StartApp.prototype._changePassword = function () {
    var data = this.ui.passwordForm.valueMap();
    var user = RapidContext.App.user();
    var prefix = user.id + ":" + user.realm + ":";
    var oldHash = CryptoJS.MD5(prefix + data.current).toString();
    var newHash = CryptoJS.MD5(prefix + data.password).toString();
    this.proc.changePassword(oldHash, newHash);
    this.ui.passwordSave.setAttrs({ disabled: true, icon: "fa fa-spin fa-refresh" });
};

/**
 * Callback for the password change dialog.
 */
StartApp.prototype._changePasswordCallback = function (res) {
    this.ui.passwordSave.setAttrs({ disabled: false, icon: "fa fa-lg fa-check" });
    if (res instanceof Error) {
        this.ui.passwordError.addError(this.ui.passwordCurrent, res.message);
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
        RapidContext.App.logout();
    } else {
        this.ui.loginForm.reset();
        this.ui.loginDialog.show();
        this.ui.loginUser.focus();
        this.proc.sessionInfo();
    }
};

/**
 * Shows the login authentication dialog.
 */
StartApp.prototype._loginAuth = function () {
    this.ui.loginAuth.setAttrs({ disabled: true, icon: "fa fa-spin fa-refresh" });
    var data = this.ui.loginForm.valueMap();
    var cb = (res) => this._loginAuthCallback(res);
    RapidContext.App.login($.trim(data.user), data.password).then(cb, cb);
};

/**
 * Callback for the login authentication.
 */
StartApp.prototype._loginAuthCallback = function (res) {
    this.ui.loginAuth.setAttrs({ disabled: false, icon: "fa fa-lg fa-check" });
    if (res instanceof Error) {
        this.ui.loginPasswordError.addError(this.ui.loginPassword, res.message);
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
        document.body.append(this.ui.tourDialog);
        this.ui.tourDialog.classList.add("bounce-down");
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
    var promise = RapidContext.Async.wait(0);
    switch (this.ui.tourWizard.activePageIndex()) {
    case 1:
        this.ui.tabContainer.selectChild(0);
        promise = promise.then(() => this._tourLocateStart());
        break;
    case 2:
        promise = RapidContext.App.callApp("help", "clearContent")
            .then(() => RapidContext.Async.wait(1))
            .then(() => this._tourLocateHelp());
        break;
    case 3:
        promise = promise.then(() => this._tourLocateTabs());
        break;
    case 4:
        promise = promise.then(() => this._tourLocateUser());
        break;
    }
    return promise.catch(RapidContext.UI.showError);
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
    box.x -= 15;
    box.y -= 5;
    box.h = 350;
    this._tourLocate(box);
};

/**
 * Locates the app tabs.
 */
StartApp.prototype._tourLocateTabs = function () {
    var tabs = this.ui.tabContainer.firstChild;
    var box = this._getBoundingBox(tabs.firstChild, tabs.lastChild);
    box.y -= 5;
    box.h += 10;
    box.x -= 10;
    box.w += 100;
    this._tourLocate(box);
};

/**
 * Locates the user menu.
 */
StartApp.prototype._tourLocateUser = function () {
    if (this.ui.menu.isHidden()) {
        this.ui.menu.show();
        setTimeout(() => this._tourLocateUser(), 500);
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
    var box = this._getBoundingBox(...arguments);
    var style = {
        left: box.x + "px",
        top: box.y + "px",
        width: box.w + "px",
        height: box.h + "px"
    };
    Object.assign(this.ui.tourLocator.style, style);
    this.ui.tourLocator.classList.remove("hidden");
    this.ui.tourLocator.classList.add("locate");
    document.body.append(this.ui.tourLocator);
    setTimeout(() => this.ui.tourLocator.classList.add("hidden"), 1500);
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
        if (elem && elem.nodeType > 0) {
            elemBox = MochiKit.Style.getElementPosition(elem);
            Object.assign(elemBox, MochiKit.Style.getElementDimensions(elem));
        } else if (elem && typeof(elem.x) == "number") {
            elemBox = Object.assign({ x: 0, y: 0, w: 0, h: 0 }, elem);
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
