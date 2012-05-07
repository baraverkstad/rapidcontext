/**
 * Creates a new app instance.
 */
function ExampleApp() {
    // Set instance variables here:
    // this.abc = 123;
}

/**
 * Starts the app and initializes the UI.
 */
ExampleApp.prototype.start = function() {

    // Create procedure callers
    this.proc = RapidContext.Procedure.mapAll({
        appList: "System.App.List"
    });

    // Attach signal handlers
    MochiKit.Signal.connect(this.ui.iconShowAll, "onchange", this, "toggleIcons");
    RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
    MochiKit.Signal.connect(this.proc.appList, "oncall", this.ui.appTable, "clear");
    MochiKit.Signal.connect(this.proc.appList, "onsuccess", this.ui.appTable, "setData");

    // Initialize data
    this.initIcons();
    this.proc.appList();
}

/**
 * Stops the app.
 */
ExampleApp.prototype.stop = function() {
    // Usually not much to do here
}

ExampleApp.prototype.initIcons = function () {
    var TD = MochiKit.DOM.TD;
    var TR = MochiKit.DOM.TR;
    var Icon = RapidContext.Widget.Icon;
    var rows = [];
    for (var k in Icon) {
        var v = Icon[k];
        if (typeof(v) == "object" && k == k.toUpperCase() && k != "DEFAULT") {
            var cells = [];
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
    var len = Math.ceil(rows.length / 3);
    var col1 = rows.slice(0, len);
    var col2 = rows.slice(len, len + len);
    var col3 = rows.slice(len + len);
    for (var i = 0; i < len; i++) {
        var tr = TR({}, col1[i]);
        if (col2[i]) {
            MochiKit.DOM.appendChildNodes(tr, TD({ "class": "space" }), col2[i]);
        }
        if (col3[i]) {
            MochiKit.DOM.appendChildNodes(tr, TD({ "class": "space" }), col3[i]);
        }
        MochiKit.DOM.appendChildNodes(tr, TD({ "class": "end" }));
        this.ui.iconTable.appendChild(tr);
    }
    this.ui.iconTable.resizeContent = MochiKit.Base.noop;
}

ExampleApp.prototype.toggleIcons = function () {
    $(this.ui.iconTable).find(".extra").toggleClass("hidden");
}
