class ExampleApp {

    constructor() {
        this.interval = null;
        this.progress = 0;
    }

    // Starts the app and initializes the UI
    start() {
        // Create procedure callers
        this.proc = RapidContext.Procedure.mapAll({
            appList: "system/app/list"
        });

        // Attach signal handlers
        this.ui.zooPane.on("click", "[data-action]", (evt) => this.action(evt));
        this.ui.progressForm.on("click", () => this.progressConfig());
        this.ui.appReload.on("click", () => this.loadApps());
        RapidContext.UI.Event.on(this.ui.iconShowAll, "change", () => this.toggleIcons());

        // Initialize data
        this.loadApps();
        this.initIcons();
        this.interval = setInterval(() => this.progressUpdate(), 500);
    }

    // Stops the app
    stop() {
        // Cleanup any resources or handlers
        clearInterval(this.interval);
    }

    async loadApps() {
        this.ui.appLoading.show();
        this.ui.appReload.hide();
        try {
            this.ui.appTable.clear();
            const data = await this.proc.appList();
            this.ui.appTable.setData(data);
        } catch (e) {
            RapidContext.UI.showError(e);
        }
        this.ui.appLoading.hide();
        this.ui.appReload.show();
    }

    action(evt) {
        switch (evt.delegateTarget.dataset.action) {
        case "dlg-show":
            this.ui.dialog.show();
            break;
        case "msg-error":
            RapidContext.UI.Msg.error("Test error");
            break;
        case "msg-warning":
            RapidContext.UI.Msg.warning("Test warning");
            break;
        case "msg-success":
            RapidContext.UI.Msg.success("Test success");
            break;
        case "msg-info":
            RapidContext.UI.Msg.info("Test info");
            break;
        case "show-overlay":
            this.ui.overlay.setAttrs({ message: "Test overlay..." });
            this.ui.overlay.show();
            setTimeout(() => this.ui.overlay.hide(), 3000);
            break;
        case "hide-overlay":
            this.ui.overlay.hide();
            break;
        }
    }

    // Modify the progress bar style
    progressConfig() {
        const cfg = this.ui.progressForm.valueMap();
        const attrs = {};
        attrs.text = cfg.text ? "Doing Random Stuff" : null;
        attrs.noratio = !cfg.ratio;
        attrs.novalue = !cfg.value;
        attrs.notime = !cfg.time;
        this.ui.progressBar.setAttrs(attrs);
    }

    // Updates the progress bar value
    progressUpdate() {
        this.progress += 0.5;
        if (this.progress >= 110) {
            this.progress = 0;
            this.ui.progressBar.setAttrs({ min: 0, max: 100 });
        }
        this.ui.progressBar.setAttrs({ value: Math.floor(this.progress) });
    }

    // Creates the icon table content dynamically
    initIcons() {
        const TD = RapidContext.UI.TD;
        const Icon = RapidContext.Widget.Icon;
        const rows = [];
        for (const k in Icon) {
            const v = Icon[k];
            if (typeof(v) == "object" && k == k.toUpperCase() && k != "DEFAULT") {
                const cells = [];
                cells.push(TD({ "class": "label" }, k));
                cells.push(TD({ "class": "white" }, Icon({ ref: k })));
                cells.push(TD({ "class": "grey extra hidden" }, Icon({ ref: k })));
                cells.push(TD({ "class": "black extra hidden" }, Icon({ ref: k })));
                cells.push(TD({ "class": "white extra hidden" }, Icon({ ref: k, disabled: true })));
                cells.push(TD({ "class": "grey extra hidden" }, Icon({ ref: k, disabled: true })));
                cells.push(TD({ "class": "black extra hidden" }, Icon({ ref: k, disabled: true })));
                rows.push(cells);
            }
        }
        const len = Math.ceil(rows.length / 3);
        const col1 = rows.slice(0, len);
        const col2 = rows.slice(len, len + len);
        const col3 = rows.slice(len + len);
        for (let i = 0; i < len; i++) {
            const tr = RapidContext.UI.TR({}, ...col1[i]);
            if (col2[i]) {
                tr.append(TD({ "class": "space" }), ...col2[i]);
            }
            if (col3[i]) {
                tr.append(TD({ "class": "space" }), ...col3[i]);
            }
            tr.append(TD({ "class": "end" }));
            this.ui.iconTable.append(tr);
        }
    }

    // Handles show all icon backgrounds checkbox
    toggleIcons() {
        this.ui.iconTable.querySelectorAll(".extra").forEach((el) => el.classList.toggle("hidden"));
    }
}

// FIXME: Switch to module and export class instead
window.ExampleApp = ExampleApp;
