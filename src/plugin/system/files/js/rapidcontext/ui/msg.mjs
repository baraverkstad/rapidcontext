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
 * Provides common message dialogs.
 * @namespace RapidContext.UI.Msg
 */

/**
 * The message actions (i.e. buttons) to show in the message dialog. Each
 * action corresponds to a result value returned by the message promise.
 * The action values `cancel` and `reload` are handled specially, as they
 * either reject the promise or reload the page altogether.
 *
 * Each action button can either use default styles, or be configured with
 * an options object. By default, the action key is used as both the
 * default button type (e.g. `primary`, `danger`, `info` or `success`), and
 * the returned action value.
 *
 * @param {string|Object} * the action text or options object
 * @param {string} [*.text] the button text to show
 * @param {string} [*.icon] the button icon to show
 * @param {string} [*.css] the button CSS classes or styles, uses action key by default
 * @param {string} [*.action] the action return value, uses action key by default
 * @param {string} [*.href] the URL to follow if selected
 * @param {string} [*.target] the link target (e.g. `_blank`)
 * @typedef {Object} Actions
 * @memberof RapidContext.UI.Msg
 *
 * @example
 * {
 *    // Actions can use either default styles or configuration
 *    danger: "Yes, I know what I'm doing",
 *    custom: { text: "Ok", icon: "fa fa-lg fa-check", css: 'primary' },
 *
 *    // The special 'cancel' action rejects the returned promise
 *    cancel: { text: "Cancel", icon: "fa fa-lg fa-times" },
 *
 *    // The 'reload' action triggers a reload of the current page
 *    reload: { text: "Reload page" }
 * }
 */
const TYPES = {
    'error': {
        title: 'Error',
        css: 'danger',
        text: 'An error message',
        icon: 'fa fa-minus-circle color-danger',
        actions: {
            danger: { icon: 'fa fa-lg fa-bolt', text: 'Ok, I understand' }
        }
    },
    'warning': {
        title: 'Warning',
        css: 'warning',
        text: 'A warning message',
        icon: 'fa fa-exclamation-triangle color-warning',
        actions: {
            warning: { icon: 'fa fa-lg fa-check', text: 'Ok, I understand' }
        }
    },
    'success': {
        title: 'Success',
        css: 'success',
        text: 'A success message',
        icon: 'fa fa-check color-success',
        actions: {
            success: { icon: 'fa fa-lg fa-check', text: 'Ok, got it' }
        }
    },
    'info': {
        title: 'Information',
        css: 'info',
        text: 'An information message',
        icon: 'fa fa-info-circle color-info',
        actions: {
            info: { icon: 'fa fa-lg fa-check', text: 'Ok, got it' }
        }
    }
};

const XML = `
    <Dialog system='true' closeable='false' class='position-relative' style='width: 25rem;'>
      <i class='ui-msg-icon position-absolute top-0 left-0 fa fa-fw fa-3x px-1 py-2'></i>
      <div class='ui-msg-text' style='padding: 0.5em 0 1em 4.5em;'>
      </div>
      <div class='ui-msg-btns text-right mt-1'>
      </div>
    </Dialog>
`.trim();

function isObject(o) {
    return Object.prototype.toString.call(o) === '[object Object]';
}

function show(type, cfg) {
    cfg = { ...TYPES[type], ...(isObject(cfg) ? cfg : { text: String(cfg) }) };
    const dlg = RapidContext.UI.create(XML);
    dlg.setAttrs({ title: cfg.title });
    if (isObject(cfg.css) || /:/.test(cfg.css)) {
        dlg.setAttrs({ style: cfg.css });
    } else {
        dlg.classList.add(...cfg.css.split(/\s+/g));
    }
    dlg.querySelector('.ui-msg-icon').classList.add(...cfg.icon.split(/\s+/g));
    if (cfg.html && cfg.html.nodeType > 0) {
        // FIXME: Use replaceChildren(..) instead
        dlg.querySelector('.ui-msg-text').innerHTML = '';
        dlg.querySelector('.ui-msg-text').append(cfg.html);
    } else if (cfg.html) {
        dlg.querySelector('.ui-msg-text').innerHTML = cfg.html;
    } else {
        dlg.querySelector('.ui-msg-text').innerText = cfg.text;
    }
    for (const key in cfg.actions) {
        const act = normalizeAction(key, cfg.actions[key]);
        const btn = createButton(act);
        if (act.css.includes(':')) {
            btn.style.cssText = act.css;
        } else {
            btn.classList.add(...act.css.split(/\s+/g));
        }
        btn.setAttribute('data-action', act.action);
        if (act.icon || act.href) {
            const icon = document.createElement('i');
            icon.className = act.icon || 'fa fa-arrow-right';
            btn.append(icon);
        }
        if (act.text) {
            const span = document.createElement('span');
            span.innerText = act.text;
            btn.append(span);
        }
        dlg.querySelector('.ui-msg-btns').append(btn);
    }
    const promise = createEventHandler(dlg);
    promise.dialog = dlg;
    promise.content = dlg.querySelector('.ui-msg-text');
    document.body.append(dlg);
    dlg.show();
    return promise;
}

function normalizeAction(key, val) {
    val = isObject(val) ? val : { text: String(val) };
    if (key === 'reload' || val.action === 'reload') {
        const href = 'javascript:window.location.reload();';
        const css = val.css || 'danger';
        Object.assign(val, { href, target: null, css, icon: 'fa fa-refresh' });
    }
    val.css = val.css || key;
    val.action = val.action || key;
    return val;
}

function createButton(act) {
    if (act.href) {
        const el = document.createElement('a');
        el.setAttribute('href', act.href);
        if (act.target != null && act.target !== false) {
            el.setAttribute('target', act.target || '_blank');
        }
        el.classList.add('btn');
        return el;
    } else {
        return document.createElement('button');
    }
}

function createEventHandler(dlg) {
    return new Promise(function (resolve, reject) {
        const handler = (evt) => {
            const action = evt.delegateTarget.dataset.action;
            if (action) {
                dlg.hide();
                RapidContext.Widget.destroyWidget(dlg);
                if (action === 'cancel') {
                    reject(new Error('operation cancelled'));
                } else {
                    resolve(action);
                }
            }
        };
        dlg.once('click', '.ui-msg-btns [data-action]', handler);
    });
}

/**
 * Displays a modal error dialog, similar to `alert()`. By default a
 * single action is shown to close the dialog, but use the `actions`
 * options to configure otherwise. The returned promise resolves with the
 * action selected by the user.
 *
 * @param {string|Object} msg the message text or message configuration options
 * @param {string} [msg.title="Error"] the dialog title
 * @param {string} [msg.text="An error message"] the message text
 * @param {string|Node} [msg.html] the message in HTML format
 * @param {string} [msg.css] the dialog css
 * @param {Actions} [msg.actions] the action buttons to show
 * @return {Promise} a promise that resolves with the selected action
 * @function RapidContext.UI.Msg.error
 *
 * @example
 * try {
 *     ...
 * } catch (e) {
 *     await RapidContext.UI.Msg.error(e.toString());
 * }
 */
export const error = show.bind(null, 'error');

/**
 * Displays a modal warning dialog, similar to `alert()`. By default a
 * single action is shown to close the dialog, but use the `actions`
 * options to configure otherwise. The returned promise resolves with the
 * action selected by the user.
 *
 * @param {string|Object} msg the message text or message configuration options
 * @param {string} [msg.title="Warning"] the dialog title
 * @param {string} [msg.text="A warning message"] the message text
 * @param {string|Node} [msg.html] the message in HTML format
 * @param {string} [msg.css] the dialog css
 * @param {Actions} [msg.actions] the action buttons to show
 * @return {Promise} a promise that resolves with the selected action
 * @function RapidContext.UI.Msg.warning
 *
 * @example
 * let text = "This will take some time to complete. Are you sure?";
 * let actions = {
 *     cancel: "No, I've changed my mind",
 *     warning: "Yes, I understand"
 * }
 * RapidContext.UI.Msg.warning({ text, actions }).then(...);
 */
export const warning = show.bind(null, 'warning');

/**
 * Displays a modal success dialog, similar to `alert()`. By default a
 * single action is shown to close the dialog, but use the `actions`
 * options to configure otherwise. The returned promise resolves with the
 * action selected by the user.
 *
 * @param {string|Object} msg the message text or message configuration options
 * @param {string} [msg.title="Success"] the dialog title
 * @param {string} [msg.text="A success message"] the message text
 * @param {string|Node} [msg.html] the message in HTML format
 * @param {string} [msg.css] the dialog css
 * @param {Actions} [msg.actions] the action buttons to show
 * @return {Promise} a promise that resolves with the selected action
 * @function RapidContext.UI.Msg.success
 *
 * @example
 * RapidContext.UI.Msg.success("Everything is proceeding as planned.");
 */
export const success = show.bind(null, 'success');

/**
 * Displays a modal info dialog, similar to `alert()`. By default a
 * single action is shown to close the dialog, but use the `actions`
 * options to configure otherwise. The returned promise resolves with the
 * action selected by the user.
 *
 * @param {string|Object} msg the message text or message configuration options
 * @param {string} [msg.title="Information"] the dialog title
 * @param {string} [msg.text="An information message"] the message text
 * @param {string|Node} [msg.html] the message in HTML format
 * @param {string} [msg.css] the dialog css
 * @param {Actions} [msg.actions] the action buttons to show
 * @return {Promise} a promise that resolves with the selected action
 * @function RapidContext.UI.Msg.info
 *
 * @example
 * await RapidContext.UI.Msg.info("System will now reboot. Please wait.");
 * ...
 */
export const info = show.bind(null, 'info');

Object.assign(error, {
    loggedOut() {
        return error({
            title: 'Logged out',
            text: 'You\'ve been logged out. Please reload the page to login again.',
            actions: {
                reload: 'Reload page'
            }
        });
    }
});

Object.assign(warning, {
    /**
     * Displays an object removal warning, similar to `confirm()`. This is
     * a simplified preset that uses the generic `warning` function.
     *
     * @param {string} type the object type (e.g. "connection")
     * @param {string} id the object name or id
     * @return {Promise} a promise that resolves when confirmed, or
     *                   rejects if cancelled
     * @function RapidContext.UI.Msg.warning:remove
     *
     * @example
     * try {
     *     await RapidContext.UI.Msg.warning.remove("person", "123");
     *     ...
     * } catch (e) {
     *     // cancelled
     * }
     */
    remove(type, id) {
        return warning({
            title: `Remove ${type}`,
            html: `Do you really want to remove the ${type} <code>${id}</code>?`,
            actions: {
                cancel: 'Cancel',
                warning: {
                    icon: 'fa fa-lg fa-minus-circle',
                    text: `Yes, remove ${type}`
                }
            }
        });
    }
});

Object.assign(info, {
    updateAvailable() {
        return info({
            title: 'Update available',
            text: 'An updated version is available. Reload the page to access the latest version.',
            actions: {
                close: 'Maybe later',
                reload: { text: 'Reload page', css: 'info', action: 'info' }
            }
        });
    }
});

export default { error, warning, success, info };
