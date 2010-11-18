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
    MochiKit.Signal.connect(this.ui.reload, "onclick", this, "loadProcList");
    MochiKit.Signal.connect(this.ui.table, "onclick", this, "showProc");
    this.loadProcList();
}

/**
 * Stops the app.
 */
ExampleApp.prototype.stop = function() {
    // Usually not much to do here
}

/**
 * Loads the list of procedures.
 */
ExampleApp.prototype.loadProcList = function() {
    this.ui.loading.show();
    this.ui.table.clear();
    var args = [];
    var d = RapidContext.App.callProc("System.Procedure.List", args);
    d.addBoth(MochiKit.Base.bind("callbackProcList", this));
}

/**
 * Callback function when the procedure list is loaded.
 *
 * @param {Object/Error} res the procedure result or error
 */
ExampleApp.prototype.callbackProcList = function(res) {
    this.ui.loading.hide();
    if (res instanceof Error) {
        RapidContext.UI.showError(res);
    } else {
        var func = function(e) { return { name: e }; }
        var data = MochiKit.Base.map(func, res);
        this.ui.table.setData(data);
    }
}

/**
 * Shows details for the selected procedure.
 */
ExampleApp.prototype.showProc = function() {
    // TODO: implement this method
}
