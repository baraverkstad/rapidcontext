/**
 * Creates a new login app.
 */
function LoginApp() {
}

/**
 * Starts the app and initializes the UI.
 */
LoginApp.prototype.start = function () {
    MochiKit.Signal.connect(this.ui.loginForm, "onsubmit", this, "_loginAuth");
    var user = RapidContext.App.user();
    if (user && user.id) {
        $(this.ui.loginName).text(user.name || user.id);
        $(this.ui.loginWarning).removeClass("hidden");
    }
    this.ui.loginDialog.show();
    this.ui.loginUser.focus();
};

/**
 * Stops the app.
 */
LoginApp.prototype.stop = function () {
    // Nothing to do here
};

/**
 * Shows the login authentication dialog.
 */
LoginApp.prototype._loginAuth = function () {
    if (this.ui.loginAuth.disabled) {
        // Call already in progress
    } else if (this.ui.loginForm.validate()) {
        this.ui.loginAuth.setAttrs({ disabled: true, icon: "LOADING" });
        var data = this.ui.loginForm.valueMap();
        var d = RapidContext.App.login($.trim(data.user), data.password);
        d.addBoth(MochiKit.Base.bind("_loginAuthCb", this));
    } else {
        this.ui.loginUser.focus();
    }
    this.ui.loginDialog.resizeToContent();
};

/**
 * Handles the login authentication callback.
 */
LoginApp.prototype._loginAuthCb = function (res) {
    this.ui.loginAuth.setAttrs({ disabled: false, icon: "OK" });
    if (res instanceof Error) {
        var msg = res.message;
        msg = msg.charAt(0).toUpperCase() + msg.substr(1);
        $(this.ui.loginError).removeClass("hidden").text(msg);
        $(this.ui.loginWarning).addClass("hidden");
        this.ui.loginDialog.resizeToContent();
    } else {
        window.location.reload();
    }
};
