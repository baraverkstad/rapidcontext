/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2025 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE for more details.
 */

/**
 * Provides functions for managing the app user interface.
 * @namespace RapidContext.UI
 */
(function (window) {

    // The global error dialog
    let errorDialog = null;

    /**
     * Displays an error message for the user. This operation may or may
     * not block the user interface, while the message is being
     * displayed (depending on implementation). All arguments will be
     * concatenated and displayed.
     *
     * @param {...(String|Error)} [args] the messages or errors to display
     *
     * @return {(String|Error|Array)} the call argument or arguments array
     *
     * @memberof RapidContext.UI
     */
    function showError(...args) {
        const msg = args.map((arg) => arg instanceof Error && arg.message || arg).join(", ");
        console.warn(msg, ...args);
        if (!errorDialog) {
            const xml = [
                "<Dialog title='Error' system='true' style='width: 25rem;'>",
                "  <i class='fa fa-exclamation-circle fa-3x color-danger mr-3'></i>",
                "  <div class='inline-block vertical-top' style='width: calc(100% - 4em);'>",
                "    <h4>Error message:</h4>",
                "    <div class='text-pre-wrap' data-message='error'></div>",
                "  </div>",
                "  <div class='text-right mt-1'>",
                "    <Button icon='fa fa-lg fa-times' data-dialog='close'>",
                "      Close",
                "    </Button>",
                "  </div>",
                "</Dialog>"
            ].join("");
            errorDialog = RapidContext.UI.create(xml);
            window.document.body.append(errorDialog);
        }
        const el = errorDialog.querySelector("[data-message]");
        if (errorDialog.isHidden()) {
            el.innerText = msg;
            errorDialog.show();
        } else if (!el.innerText.includes(msg)) {
            el.innerText += `\n\n${msg}`;
        }
        return (args.length == 1) ? args[0] : args;
    }

    /**
     * Connects the default UI signals for a procedure. This includes a default
     * error handler, a loading icon with cancellation handler and a reload icon
     * with the appropriate click handler.
     *
     * @param {Procedure} proc the `RapidContext.Procedure` instance
     * @param {Icon} [loadingIcon] the loading icon, or `null`
     * @param {Icon} [reloadIcon] the reload icon, or `null`
     *
     * @see RapidContext.Procedure
     *
     * @memberof RapidContext.UI
     */
    function connectProc(proc, loadingIcon, reloadIcon) {
        // TODO: error signal not automatically cleaned up on stop()...
        MochiKit.Signal.connect(proc, "onerror", showError);
        if (loadingIcon) {
            MochiKit.Signal.connect(proc, "oncall", loadingIcon, "show");
            MochiKit.Signal.connect(proc, "onresponse", loadingIcon, "hide");
            MochiKit.Signal.connect(loadingIcon, "onclick", proc, "cancel");
        }
        if (reloadIcon) {
            MochiKit.Signal.connect(proc, "oncall", reloadIcon, "hide");
            MochiKit.Signal.connect(proc, "onresponse", reloadIcon, "show");
            MochiKit.Signal.connect(reloadIcon, "onclick", proc, "recall");
        }
    }

    // Export module API
    const RapidContext = window.RapidContext ?? (window.RapidContext = {});
    const module = RapidContext.UI ?? (RapidContext.UI = {});
    Object.assign(module, { showError, connectProc });

})(globalThis);
