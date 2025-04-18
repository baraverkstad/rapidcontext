class StartApp {

    constructor() {
        this.appStatus = {};
        this.gradients = ["yellow", "red", "green", "blue"];
        this.showingModifiers = false;
        this.showingInlinePanes = false;
    }

    /**
     * Starts the app and initializes the UI.
     */
    start() {
        this.proc = RapidContext.Procedure.mapAll({
            appList: "system/app/list",
            changePassword: "system/user/changepassword",
            sessionInfo: "system/session/current"
        });
        const status = RapidContext.App.status();

        // General events
        RapidContext.UI.Event.on(document, "keydown", (evt) => this._showAppModifiers(evt.ctrlKey || evt.metaKey));
        RapidContext.UI.Event.on(document, "keyup visibilitychange", () => this._showAppModifiers(false));

        // Info bar & popup menu
        this._initInfoMenu();
        let env = status.environment;
        env = (env && env.name) ? env.name : "<none>";
        this.ui.infoEnv.innerText = env;
        MochiKit.Signal.connect(this.proc.sessionInfo, "onsuccess", this, "_initInfoMenu");
        this.ui.infoBar.on("click", () => this.ui.menu.setAttrs({ hidden: !this.ui.menu.isHidden() }));
        this.ui.infoBar.on("click", false);
        this.ui.menu.on("menuselect", (evt) => this._popupSelect(evt.detail.item));
        RapidContext.UI.Event.on(document, "click", () => this.ui.menu.hide());

        // App pane
        RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
        MochiKit.Signal.connect(this.proc.appList, "onsuccess", this, "_initApps");
        RapidContext.UI.Event.on(this.ui.appTable, "click", "[data-appid]", (evt) => this._handleAppLaunch(evt));

        // About dialog
        const version = MochiKit.Text.format("{version} ({date})", status);
        this.ui.aboutVersion.innerText = version;

        // Password dialog
        this.ui.passwordForm.on("submit", () => this._changePassword());
        MochiKit.Signal.connect(this.proc.changePassword, "onresponse", this, "_changePasswordCallback");
        this.ui.passwordForm.addValidator("passwordcheck", function (value, field, form) {
            return value === form.elements["password"].value;
        });

        // Login dialog
        this.ui.loginForm.on("submit", () => this._loginAuth());

        // Tour wizard
        this.ui.tourButton.on("click", () => this._tourStart());
        this.ui.tourWizard.on("close", () => this._tourStop());
        this.ui.tourWizard.on("change", () => this._tourChange());
        this.ui.tourStartLocate.on("click", () => this._tourLocateStart());
        this.ui.tourUserLocate.on("click", () => this._tourLocateUser());
        this.ui.tourHelpLocate.on("click", () => this._tourLocateHelp());
        this.ui.tourTabsLocate.on("click", () => this._tourLocateTabs());

        // Init app list
        this.proc.appList();
    }

    /**
     * Stops the app.
     */
    stop() {
        for (const name in this.proc) {
            MochiKit.Signal.disconnectAll(this.proc[name]);
        }
    }

    /**
     * Initializes the info bar and popup menu.
     */
    _initInfoMenu() {
        const user = RapidContext.App.user();
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
    }

    /**
     * Initializes the app launcher pane and handles app auto-starts.
     * This method is safe to call multiple times, such as when the app
     * list is refreshed.
     */
    _initApps() {
        const apps = RapidContext.App.apps();

        // Hide help and admin apps menu items if not available
        const launchers = RapidContext.Data.object("id", apps);
        this.ui.menuHelp.classList.toggle("disabled", !launchers.help);
        this.ui.menuAdmin.classList.toggle("disabled", !launchers.admin);

        // Redraw the app launcher table
        const $appTable = $(this.ui.appTable).empty();
        const sortKey = (a) => (a.sort || a.id).toLowerCase();
        const isListed = (a) => a.launch == "auto" || a.launch == "manual" || a.launch == "window";
        RapidContext.Data.sort(sortKey, apps).filter(isListed).forEach(function (app) {
            const $tr = $("<tr>").addClass("clickable").attr("data-appid", app.id).appendTo($appTable);
            const icon = app.icon && app.icon.cloneNode(true);
            $("<td class='p-2'>").append(icon).appendTo($tr);
            const ext = RapidContext.Widget.Icon("fa fa-external-link-square ml-1");
            ext.addClass((app.launch == "window") ? "launch-window" : "hidden");
            const $title = $("<a href='#' class='h4 m-0'>").text(app.name).append(ext);
            const $desc = $("<span class='text-pre-wrap'>").text(app.description);
            $("<td class='pl-1 pr-2 py-2'>").append($title, $desc).appendTo($tr);
        });

        // Start auto and inline apps
        const instances = apps.reduce((c, a) => c + a.instances.length, 0);
        for (const app of apps) {
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
    }

    /**
     * Initializes a dashboard app.
     */
    _initDashboardApp(app) {
        if (!this.showingInlinePanes) {
            this.showingInlinePanes = true;
            this.ui.inlinePane.removeAll();
        }
        const cls = ["dash-item", "box", this.gradients.shift() || "grey"];
        if (app.startPage == "left" || app.startPage == "right") {
            cls.push(`float-${app.startPage}`, `clear-${app.startPage}`);
        } else {
            cls.push("clear-both");
        }
        const attrs = {
            pageTitle: app.name,
            pageCloseable: true,
            "class": cls.join(" ")
        };
        const pane = new RapidContext.Widget.Pane(attrs);
        this.ui.inlinePane.insertBefore(pane, this.ui.inlinePane.firstChild);
        if (app.startPage == "left" || app.startPage == "right") {
            pane.style.width = "calc(50% - 3rem - 3px)";
        } else {
            pane.style.width = "calc(100% - 3rem - 3px)";
        }
        this.startApp(app.className, pane);
    }

    /**
    * Shows or hides the application launcher modifier icons.
    *
    * @param {boolean} visible the visible flag
    */
    _showAppModifiers(visible) {
        if (this.showingModifiers !== visible) {
            this.showingModifiers = visible;
            const icons = $(this.ui.appTable).find("a > i").not(".launch-window");
            icons.toggleClass("hidden", !visible);
        }
    }

    /**
     * Handles an app launch click.
     *
     * @param {Event} evt the click event
     */
    _handleAppLaunch(evt) {
        const appId = evt.delegateTarget.dataset.appid;
        if (appId) {
            const app = RapidContext.App.findApp(appId);
            const win = evt.ctrlKey || evt.metaKey || (app && app.launch == "window");
            this.startApp(appId, win ? window.open() : null);
            this._showAppModifiers(false);
        }
    }

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
    initAppPane(pane, opts) {
        opts = opts || {};
        if (pane == null) {
            const attrs = {
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
        const overlay = new RapidContext.Widget.Overlay({ message: "Loading app..." });
        pane.append(overlay);
        return { root: pane, overlay: overlay };
    }

    /**
     * Starts an app with the specified class name.
     *
     * @param {string} app the app id or class name
     * @param {Widget} [container] the optional container widget
     */
    startApp(app, container) {
        RapidContext.App.startApp(app, container).catch(RapidContext.UI.showError);
    }

    /**
     * Handles a popup menu item selection.
     */
    _popupSelect(item) {
        this.ui.menu.hide();
        switch (item.dataset.action) {
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
    }

    /**
     * Changes the user password (from the dialog).
     */
    _changePassword() {
        const data = this.ui.passwordForm.valueMap();
        const user = RapidContext.App.user();
        const prefix = `${user.id}:${user.realm}:`;
        const oldHash = CryptoJS.MD5(prefix + data.current).toString();
        const newHash = CryptoJS.MD5(prefix + data.password).toString();
        this.proc.changePassword(oldHash, newHash);
        this.ui.passwordSave.setAttrs({ disabled: true, icon: "fa fa-spin fa-refresh" });
    }

    /**
     * Callback for the password change dialog.
     */
    _changePasswordCallback(res) {
        this.ui.passwordSave.setAttrs({ disabled: false, icon: "fa fa-lg fa-check" });
        if (res instanceof Error) {
            this.ui.passwordError.addError(this.ui.passwordCurrent, res.message);
        } else {
            this.ui.passwordDialog.hide();
            this.ui.passwordForm.reset();
        }
    }

    /**
     * Shows either the login or the logout dialog. In the latter case, the
     * session is also terminated.
     */
    _loginOut() {
        const user = RapidContext.App.user();
        if (user && user.id) {
            RapidContext.App.logout();
        } else {
            this.ui.loginForm.reset();
            this.ui.loginDialog.show();
            this.ui.loginUser.focus();
            this.proc.sessionInfo();
        }
    }

    /**
     * Shows the login authentication dialog.
     */
    _loginAuth() {
        this.ui.loginAuth.setAttrs({ disabled: true, icon: "fa fa-spin fa-refresh" });
        const data = this.ui.loginForm.valueMap();
        const cb = (res) => this._loginAuthCallback(res);
        RapidContext.App.login($.trim(data.user), data.password).then(cb, cb);
    }

    /**
     * Callback for the login authentication.
     */
    _loginAuthCallback(res) {
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
    }

    /**
     * Starts the guided tour.
     */
    _tourStart() {
        if (this.ui.tourDialog.isHidden()) {
            document.body.append(this.ui.tourDialog);
            this.ui.tourDialog.classList.add("bounce-down");
            this.ui.tourWizard.activatePage(0);
            this.ui.tourDialog.show();
        }
    }

    /**
     * Stops the guided tour.
     */
    _tourStop() {
        this.ui.tourDialog.hide();
    }

    /**
     * Changes the active page in the guided tour.
     */
    _tourChange() {
        let promise = RapidContext.Async.wait(0);
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
    }

    /**
     * Locates the start app.
     */
    _tourLocateStart() {
        this._tourLocate(this.ui.appTable);
    }

    /**
     * Locates the help app.
     */
    _tourLocateHelp() {
        const tab = this.ui.tabContainer.selectedChild();
        const box = this._getBoundingBox(tab.firstChild.nextSibling);
        box.x -= 15;
        box.y -= 5;
        box.h = 350;
        this._tourLocate(box);
    }

    /**
     * Locates the app tabs.
     */
    _tourLocateTabs() {
        const tabs = this.ui.tabContainer.firstChild;
        const box = this._getBoundingBox(tabs.firstChild, tabs.lastChild);
        box.y -= 5;
        box.h += 10;
        box.x -= 10;
        box.w += 100;
        this._tourLocate(box);
    }

    /**
     * Locates the user menu.
     */
    _tourLocateUser() {
        if (this.ui.menu.isHidden()) {
            this.ui.menu.show();
            setTimeout(() => this._tourLocateUser(), 500);
        } else {
            this.ui.menu.show();
            this._tourLocate(this.ui.menu);
        }
    }

    /**
     * Locates the specified DOM nodes.
     *
     * @param {Node} ... the DOM node elements to locate
     */
    _tourLocate() {
        const box = this._getBoundingBox(...arguments);
        const style = {
            left: `${box.x}px`,
            top: `${box.y}px`,
            width: `${box.w}px`,
            height: `${box.h}px`
        };
        Object.assign(this.ui.tourLocator.style, style);
        this.ui.tourLocator.classList.remove("hidden");
        this.ui.tourLocator.classList.add("locate");
        document.body.append(this.ui.tourLocator);
        setTimeout(() => this.ui.tourLocator.classList.add("hidden"), 1500);
    }

    /**
     * Returns the bounding box for the specified DOM nodes.
     *
     * @param {Node} ... the DOM node elements to locate
     *
     * @return {Object} the bounding box
     */
    _getBoundingBox() {
        let box = null;
        for (let i = 0; i < arguments.length; i++) {
            const elem = arguments[i];
            let elemBox = null;
            if (elem && elem.nodeType > 0) {
                elemBox = MochiKit.Style.getElementPosition(elem);
                Object.assign(elemBox, MochiKit.Style.getElementDimensions(elem));
            } else if (elem && typeof(elem.x) == "number") {
                elemBox = { x: 0, y: 0, w: 0, h: 0, ...elem };
            }
            if (elemBox != null && box == null) {
                box = elemBox;
            } else if (elemBox != null) {
                const xMin = Math.min(box.x, elemBox.x);
                const xMax = Math.max(box.x + box.w, elemBox.x + elemBox.w);
                const yMin = Math.min(box.y, elemBox.y);
                const yMax = Math.max(box.y + box.h, elemBox.y + elemBox.h);
                box.x = xMin;
                box.w = xMax - xMin;
                box.y = yMin;
                box.h = yMax - yMin;
            }
        }
        return box;
    }
}

// FIXME: Switch to module and export class instead
window.StartApp = StartApp;
