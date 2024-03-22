window.ExampleApp = class {

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
        MochiKit.Signal.connect(this.ui.progressForm, "onclick", this, "progressConfig");
        RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
        MochiKit.Signal.connect(this.proc.appList, "oncall", this.ui.appTable, "clear");
        MochiKit.Signal.connect(this.proc.appList, "onsuccess", this.ui.appTable, "setData");
        MochiKit.Signal.connect(this.ui.iconShowAll, "onchange", this, "toggleIcons");

        // Initialize data
        this.proc.appList();
        this.initIcons();
        this.interval = setInterval(() => this.progressUpdate(), 500);
    }

    // Stops the app
    stop() {
        // Cleanup any resources or handlers
        clearInterval(this.interval);
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
        let cfg = this.ui.progressForm.valueMap();
        let attrs = {};
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
        let TD = RapidContext.UI.TD;
        let Icon = RapidContext.Widget.Icon;
        let rows = [];
        for (let k in Icon) {
            let v = Icon[k];
            if (typeof(v) == "object" && k == k.toUpperCase() && k != "DEFAULT") {
                let cells = [];
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
        let len = Math.ceil(rows.length / 3);
        let col1 = rows.slice(0, len);
        let col2 = rows.slice(len, len + len);
        let col3 = rows.slice(len + len);
        for (let i = 0; i < len; i++) {
            let tr = RapidContext.UI.TR({}, ...col1[i]);
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
        $(this.ui.iconTable).find(".extra").toggleClass("hidden");
    }
};
