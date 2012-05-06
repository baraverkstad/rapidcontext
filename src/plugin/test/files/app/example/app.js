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
    RapidContext.UI.connectProc(this.proc.appList, this.ui.appLoading, this.ui.appReload);
    MochiKit.Signal.connect(this.proc.appList, "oncall", this.ui.appTable, "clear");
    MochiKit.Signal.connect(this.proc.appList, "onsuccess", this.ui.appTable, "setData");

    // Initialize data
    this.proc.appList();
}

/**
 * Stops the app.
 */
ExampleApp.prototype.stop = function() {
    // Usually not much to do here
}
