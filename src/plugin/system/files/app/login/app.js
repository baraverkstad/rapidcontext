class LoginApp {

    /**
     * Starts the app and initializes the UI.
     */
    start() {
        MochiKit.Signal.connect(this.ui.loginForm, "onsubmit", this, "_loginAuth");
        const user = RapidContext.App.user();
        if (user?.id) {
            $(this.ui.loginName).text(user.name ?? user.id);
            $(this.ui.loginWarning).removeClass("hidden");
        }
        this.ui.loginDialog.show();
        this.ui.loginUser.focus();
    }

    /**
     * Stops the app.
     */
    stop() {
        // Nothing to do here
    }

    /**
     * Shows the login authentication dialog.
     */
    _loginAuth() {
        this.ui.loginAuth.setAttrs({ disabled: true, icon: "LOADING" });
        const data = this.ui.loginForm.valueMap();
        RapidContext.App.login($.trim(data.user), data.password)
            .then(() => window.location.reload())
            .catch((err) => this._loginError(err));
    }

    /**
     * Handles login errors.
     */
    _loginError(err) {
        this.ui.loginAuth.setAttrs({ disabled: false, icon: "OK" });
        let msg = (err?.message) ?? String(err);
        msg = msg.charAt(0).toUpperCase() + msg.substr(1);
        $(this.ui.loginError).removeClass("hidden").text(msg);
        $(this.ui.loginWarning).addClass("hidden");
    }
}

// FIXME: Switch to module and export class instead
window.LoginApp = LoginApp;
