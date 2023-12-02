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
        MochiKit.Signal.connect(this.ui.progressForm, "onclick", this, "progressConfig");
        RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
        MochiKit.Signal.connect(this.proc.appList, "oncall", this.ui.appTable, "clear");
        MochiKit.Signal.connect(this.proc.appList, "onsuccess", this.ui.appTable, "setData");
        MochiKit.Signal.connect(this.ui.dialogButton, "onclick", this.ui.dialog, "show");
        MochiKit.Signal.connect(this.ui.dialogClose, "onclick", this.ui.dialog, "hide");
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
        let TD = MochiKit.DOM.TD;
        let TR = MochiKit.DOM.TR;
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
            let tr = TR({}, ...col1[i]);
            if (col2[i]) {
                tr.append(TD({ "class": "space" }), ...col2[i]);
            }
            if (col3[i]) {
                tr.append(TD({ "class": "space" }), ...col3[i]);
            }
            tr.append(TD({ "class": "end" }));
            this.ui.iconTable.append(tr);
        }
        this.ui.iconTable.resizeContent = () => {};
    }

    // Handles show all icon backgrounds checkbox
    toggleIcons() {
        $(this.ui.iconTable).find(".extra").toggleClass("hidden");
    }
};
