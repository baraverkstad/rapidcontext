/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2022-2024 Per Cederberg. All rights reserved.
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

(function (window) {

    function isFunction(value) {
        return typeof(value) === "function";
    }

    /**
     * Checks if an object conforms to the `Promise` API.
     *
     * @memberOf RapidContext.Async
     * @param {Object} value the object to check
     *
     * @return {boolean} `true` if the object is a promise, or `false` otherwise
     */
    function isPromise(value) {
        return !!value && isFunction(value.then) && isFunction(value.catch);
    }

    function isDeferred(value) {
        return !isPromise(value) && value instanceof MochiKit.Async.Deferred;
    }

    function isCancellable(value) {
        return isPromise(value) && isFunction(value.cancel);
    }

    /**
     * Creates a new cancellable promise. The cancellation callback must be a
     * no-op if the action is already performed.
     *
     * @constructor
     * @param {Promise|Deferred|Function|Error|Object} promise the promise to wrap
     * @param {function} [onCancelled] the cancellation callback function
     *
     * @name RapidContext.Async
     * @class A cancellable Promise that is backwards-compatible with
     * `MochiKit.Async.Deferred`. These promises can be used either as a deferred
     * or as a normal promise (recommended).
     *
     * Instances of this class are `instanceof MochiKit.Async.Deferred` (and
     * NOT `instanceof Promise`) due to backwards compatibility. Use
     * `RapidContext.Async.isPromise` to check for a promise-compatible API.
     */
    function Async(promise, onCancelled) {
        if (isPromise(promise)) {
            this._promise = promise;
        } else if (isDeferred(promise)) {
            this._promise = new Promise(function (resolve, reject) {
                promise.addBoth(function (res) {
                    var cb = (res instanceof Error) ? reject : resolve;
                    cb(res);
                });
            });
        } else if (isFunction(promise)) {
            this._promise = new Promise(promise);
        } else if (promise instanceof Error) {
            this._promise = Promise.reject(promise);
        } else {
            this._promise = Promise.resolve(promise);
        }
        this._cancel = onCancelled;
    }

    // Setup prototype chain (for instanceof)
    // FIXME: Change to Promise.prototype once MochiKit is removed
    Async.prototype = Object.create(MochiKit.Async.Deferred.prototype);

    Object.assign(Async.prototype, {
        constructor: Async,

        /**
         * Registers one or two callback functions to this promise.
         *
         * @memberof RapidContext.Async.prototype
         * @param {function} [onFulfilled] a callback if promise fulfilled
         * @param {function} [onRejected] a callback if promise rejected
         * @returns {Async} a new promise that resolves to whatever value the
         *     callback functions return
         */
        then: function (onFulfilled, onRejected) {
            var promise = wrapPromise(this, onFulfilled, onRejected);
            return new Async(promise, () => this.cancel());
        },

        /**
         * Registers a reject callback function to this promise.
         *
         * @memberof RapidContext.Async.prototype
         * @param {function} onRejected a callback if promise rejected
         * @returns {Async} a new promise that resolves to whatever value the
         *     callback function returned
         */
        catch: function (onRejected) {
            return this.then(undefined, onRejected);
        },

        /**
         * Registers a finalizer callback function to this promise. Note that
         * the finalizer MAY NOT BE CALLED if the promise is cancelled.
         *
         * @memberof RapidContext.Async.prototype
         * @param {function} onFinally a callback for promise resolved
         * @returns {Async} a new promise
         */
        finally: function (onFinally) {
            var promise = this._promise.finally(onFinally);
            return new Async(promise, () => this.cancel());
        },

        /**
         * Cancels this promise and calls the registered cancellation handler.
         * No other callbacks will be triggered after the promise has been
         * cancelled (except finalizer).
         *
         * @memberof RapidContext.Async.prototype
         */
        cancel: function () {
            this._cancelled = true;
            isFunction(this._cancel) && this._cancel();
            isCancellable(this._result) && this._result.cancel();
        },

        /**
         * Registers one or two callback functions to this promise.
         *
         * @memberof RapidContext.Async.prototype
         * @param {function} [onFulfilled] a callback if promise fulfilled
         * @param {function} [onRejected] a callback if promise rejected
         * @returns {Async} this same promise
         * @deprecated Provided for `MochiKit.Async.Deferred` compatibility
         */
        addCallbacks: function (callback, errback) {
            console.warn("deprecated: call to RapidContext.Async.addCallbacks(), use then() instead.");
            this._promise = wrapPromise(this, callback, errback);
            return this;
        },

        /**
         * Registers a fulfilled callback function to this promise.
         *
         * @memberof RapidContext.Async.prototype
         * @param {function} [callback] a callback if promise fulfilled
         * @returns {Async} this same promise
         * @deprecated Provided for `MochiKit.Async.Deferred` compatibility
         */
        addCallback: function (callback) {
            console.warn("deprecated: call to RapidContext.Async.addCallback(), use then() instead.");
            if (arguments.length > 1) {
                var args = Array.from(arguments);
                args.splice(1, 0, undefined);
                callback = callback.bind.apply(callback, args);
            }
            this._promise = wrapPromise(this, callback, undefined);
            return this;
        },

        /**
         * Registers a reject callback function to this promise.
         *
         * @memberof RapidContext.Async.prototype
         * @param {function} errback a callback if promise rejected
         * @returns {Async} this same promise
         * @deprecated Provided for `MochiKit.Async.Deferred` compatibility
         */
        addErrback: function (errback) {
            console.warn("deprecated: call to RapidContext.Async.addErrback(), use catch() instead.");
            if (arguments.length > 1) {
                var args = Array.from(arguments);
                args.splice(1, 0, undefined);
                errback = errback.bind.apply(errback, args);
            }
            this._promise = wrapPromise(this, undefined, errback);
            return this;
        },

        /**
         * Registers a callback function to this promise.
         *
         * @memberof RapidContext.Async.prototype
         * @param {function} [callback] a callback if promise either fulfilled
         *        or rejected
         * @returns {Async} this same promise
         * @deprecated Provided for `MochiKit.Async.Deferred` compatibility
         */
        addBoth: function (callback) {
            console.warn("deprecated: call to RapidContext.Async.addBoth(), use then() instead.");
            if (arguments.length > 1) {
                var args = Array.from(arguments);
                args.splice(1, 0, undefined);
                callback = callback.bind.apply(callback, args);
            }
            this._promise = wrapPromise(this, callback, callback);
            return this;
        }
    });

    function wrapPromise(self, callback, errback) {
        var onSuccess = isFunction(callback) ? wrapCallback(self, callback) : callback;
        var onError = isFunction(errback) ? wrapCallback(self, errback) : errback;
        return self._promise.then(onSuccess, onError);
    }

    function wrapCallback(self, callback) {
        return function (val) {
            var res = self._cancelled ? undefined : callback(val);
            return self._result = (isDeferred(res) ? new Async(res) : res);
        };
    }

    /**
     * Returns a delayed value.
     *
     * @memberOf RapidContext.Async
     * @param {number} millis the number of milliseconds to wait
     * @param {Object} [value] the value to resolve with
     * @return {Async} a new promise that resolves with the value
     */
    function wait(millis, value) {
        var timer = null;
        function callLater(resolve) {
            timer = setTimeout(() => resolve(value), millis);
        }
        return new Async(callLater, () => clearTimeout(timer));
    }

    /**
     * Loads an image from a URL.
     *
     * @memberOf RapidContext.Async
     * @param {string} url the image URL to load
     * @return {Async} a promise that resolves with the DOM `<img>` element
     */
    function img(url) {
        return create("img", { src: url });
    }

    /**
     * Injects a CSS stylesheet to the current page.
     *
     * @memberOf RapidContext.Async
     * @param {string} url the stylesheet URL to load
     * @return {Async} a promise that resolves with the DOM `<link>` element
     */
    function css(url) {
        var attrs = { rel: "stylesheet", type: "text/css", href: url };
        return create("link", attrs, document.head);
    }

    /**
     * Injects a JavaScript to the current page.
     *
     * @memberOf RapidContext.Async
     * @param {string} url the script URL to load
     * @return {Async} a promise that resolves with the DOM `<script>` element
     */
    function script(url) {
        return create("script", { src: url, async: false }, document.head);
    }

    function create(tag, attrs, parent) {
        return new Async(function (resolve, reject) {
            var el = document.createElement(tag);
            el.onload = function () {
                el = el.onload = el.onerror = null;
                resolve(el);
            };
            el.onerror = function (err) {
                var url = el.src || el.href;
                el = el.onload = el.onerror = null;
                reject(new URIError("failed to load: " + url, url));
            };
            Object.assign(el, attrs);
            parent && parent.append(el);
        });
    }

    /**
     * Performs an XmlHttpRequest to a URL.
     *
     * @memberOf RapidContext.Async
     * @param {string} url the URL to request
     * @param {Object} [opts] the request options
     * @param {string} [opts.method] the HTTP method (e.g. `GET`, `POST`...)
     * @param {Object} [opts.headers] the HTTP headers to send
     * @param {number} [opts.timeout] the timeout in milliseconds (default is 30s)
     * @param {string} [opts.log] the logging prefix (defaults to `null` for no logs)
     * @param {string} [opts.responseType] the expected HTTP response (e.g. `json`)
     * @param {string} [opts.body] the HTTP request body to send
     *
     * @return {Async} a new promise that resolves with either the XHR object,
     *     or an error if the request failed
     */
    function xhr(url, opts) {
        opts = { method: "GET", headers: {}, timeout: 30000, log: null, ...opts };
        if (opts.responseType === "json" && !opts.headers["Accept"]) {
            opts.headers["Accept"] = "application/json";
        }
        var xhr = new XMLHttpRequest();
        var promise = new Promise(function (resolve, reject) {
            xhr.open(opts.method, url, true);
            for (var key in opts.headers) {
                xhr.setRequestHeader(key, opts.headers[key]);
            }
            xhr.responseType = opts.responseType || "text";
            xhr.timeout = opts.timeout;
            if (xhr.upload && isFunction(opts.progress)) {
                xhr.upload.addEventListener("progress", opts.progress);
            }
            xhr.onreadystatechange = function () {
                var err;
                if (xhr && xhr.readyState === 4) {
                    if (xhr.status >= 200 && xhr.status <= 299 && xhr.response != null) {
                        resolve(xhr);
                    } else if (xhr.status === 0) {
                        err = "communication error or timeout";
                        reject(new AsyncError(opts.method, url, xhr, err, opts.log));
                    } else {
                        err = "unexpected response code/data";
                        reject(new AsyncError(opts.method, url, xhr, err, opts.log));
                    }
                    xhr = null; // Stop duplicate events
                }
            };
            xhr.send(opts.body);
        });
        var cancel = function () {
            xhr && setTimeout(xhr.abort.bind(xhr));
            xhr = null;
        };
        return new Async(promise, cancel);
    }

    function AsyncError(method, url, xhr, detail, log) {
        var parts = [].concat(detail, " [");
        if (xhr && xhr.status > 0) {
            parts.push("HTTP ", xhr.status, ": ");
        }
        parts.push(method, " ", url, "]");
        this.message = parts.filter(Boolean).join("");
        this.method = method;
        this.url = url;
        this.code = xhr && xhr.status;
        this.stack = new Error().stack;
        if (log) {
            let logger = /timeout/i.test(this.message) ? console.info : console.warn;
            logger([log, this.message].join(": "), xhr && xhr.response);
        }
    }

    AsyncError.prototype = Object.create(Error.prototype);
    Object.assign(AsyncError.prototype, {
        constructor: AsyncError,
        name: "AsyncError"
    });

    // Create namespace and export API
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    RapidContext.Async = Async;
    Object.assign(Async, { isPromise, wait, img, css, script, xhr, AsyncError });

})(this);
