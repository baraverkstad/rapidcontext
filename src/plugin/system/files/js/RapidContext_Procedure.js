/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2009-2023 Per Cederberg. All rights reserved.
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

    /**
     * Creates a new procedure caller function. This function can be called either
     * as a constructor or as a plain function. In both cases it returns a new
     * JavaScript function with additional methods.
     *
     * @constructor
     * @param {String} procedure the procedure name
     * @property {String} procedure The procedure name.
     * @property {Array} args The arguments used in the last call.
     *
     * @name RapidContext.Procedure
     * @class The procedure wrapper function. Used to provide a simplified way of
     *   calling a procedure and connecting results through signals (instead of
     *   using promise callbacks).
     *
     *   The actual calls are performed with normal function calls, but the results
     *   are asynchronous. When called, the procedure function returns a
     *   `RapidContext.Async` promise (as the normal API call), but the results
     *   will also be signalled through the `onsuccess` signal.
     *
     *   Differing from normal functions, a procedure function will also ensure
     *   that only a single call is in progress at any time, automatically
     *   cancelling any previous call if needed.
     */
    function Procedure(procedure) {
        function self() {
            self.args = Array.from(arguments);
            return self.recall();
        }
        self.procedure = procedure;
        self.args = null;
        self._promise = null;
        for (var k in Procedure.prototype) {
            if (!self[k]) {
                self[k] = Procedure.prototype[k];
            }
        }
        return self;
    }

    /**
     * Emitted when the procedure is called. Each call corresponds to exactly one
     * `oncall` and one `onresponse` event (even if the call was cancelled). No
     * event data will be sent.
     *
     * @name RapidContext.Procedure#oncall
     * @event
     */

    /**
     * Emitted if a partial procedure result is available. This event will only be
     * emitted when performing a multi-call, along with the `oncall` and
     * `onresponse` events (for each call). The partial procedure result will be
     * sent as the event data.
     *
     * @name RapidContext.Procedure#onupdate
     * @event
     */

    /**
     * Emitted when the procedure response has been received. Each call corresponds
     * to exactly one `oncall` and one `onresponse` event (even if the call was
     * cancelled). The call response or error object will be sent as the event
     * data.
     *
     * @name RapidContext.Procedure#onresponse
     * @event
     */

    /**
     * Emitted when a procedure call returned a result. This event is emitted after
     * the `onresponse` event, but only if the procedure call actually succeeded.
     * Use the `onerror` or `oncancel` signals for other result statuses. The call
     * response object will be sent as the event data.
     *
     * @name RapidContext.Procedure#onsuccess
     * @event
     */

    /**
     * Emitted when a procedure call failed. This event is emitted after the
     * `onresponse` event, but only if the procedure call returned an error. Use
     * the `onsuccess` or `oncancel` for other result statuses. The call error
     * object will be sent as the event data.
     *
     * @name RapidContext.Procedure#onerror
     * @event
     */

    /**
     * Emitted when a procedure call was cancelled. This event is emitted after the
     * `onresponse` event, but only if the procedure call was cancelled. Use the
     * `onsuccess` or `onerror` for other result statuses. No event data will be
     * sent.
     *
     * @name RapidContext.Procedure#oncancel
     * @event
     */

    /**
     * Calls the procedure with the same arguments as used in the last call. The
     * call is asynchronous, so results will not be returned by this method.
     * Instead the results will be available through the `onsuccess` signal, for
     * example.
     *
     * Note that any previously running call will automatically be cancelled, since
     * only a single call can be processed at any time.
     *
     * @methodOf RapidContext.Procedure.prototype
     * @return {Promise} a `RapidContext.Async` promise that will resolve with
     *         the response data or error
     */
    function recall() {
        if (this.args === null) {
            throw new Error("No arguments supplied for procedure call to " + this.procedure);
        }
        this.cancel();
        signal(this, "oncall");
        var cb = callback.bind(this);
        this._promise = RapidContext.App.callProc(this.procedure, this.args).then(cb, cb);
        return this._promise;
    }

    // The procedure promise callback handler. Dispatches the appropriate
    // signals depending on the result.
    function callback(res) {
        this._promise = null;
        signal(this, "onresponse", res);
        if (res instanceof Error) {
            signal(this, "onerror", res);
            return Promise.reject(res);
        } else {
            signal(this, "onsuccess", res);
            return res;
        }
    }

    /**
     * Calls the procedure multiple times (in sequence) with different arguments
     * (supplied as an array of argument arrays). The calls are asynchronous, so
     * results will not be returned by this method. Instead an array with the
     * results will be available through the `onupdate` and `onsuccess` signals,
     * for example.
     *
     * Note that any previously running call will automatically be cancelled, since
     * only a single call can be processed at any time. A result `transform`
     * function can be supplied to transform each individual result. If the
     * `transform` function throws an error, that result will be omitted.
     *
     * @methodOf RapidContext.Procedure.prototype
     * @param {Array} args the array of argument arrays
     * @param {Function} [transform] the optional result transform function
     * @return {Promise} a `RapidContext.Async` promise that will resolve with
     *         the response data or error
     */
    function multicall(args, transform) {
        this.cancel();
        this._mapArgs = args;
        this._mapPos = 0;
        this._mapRes = [];
        this._mapTransform = transform;
        nextCall.call(this);
    }

    // The multicall promise callback handler. Dispatches the appropriate
    // signals depending on the result.
    function nextCall(res) {
        this._promise = null;
        if (typeof res != "undefined") {
            signal(this, "onresponse", res);
            if (res instanceof Error) {
                signal(this, "onerror", res);
                return Promise.reject(res);
            } else {
                if (this._mapTransform == null) {
                    this._mapRes.push(res);
                } else {
                    try {
                        res = this._mapTransform(res);
                        this._mapRes.push(res);
                    } catch (ignore) {
                        // Skip results with mapping errors
                    }
                }
                signal(this, "onupdate", this._mapRes);
            }
        }
        if (this._mapPos < this._mapArgs.length) {
            this.args = this._mapArgs[this._mapPos++];
            signal(this, "oncall");
            var cb = nextCall.bind(this);
            this._promise = RapidContext.App.callProc(this.procedure, this.args).then(cb, cb);
            return this._promise;
        } else {
            signal(this, "onsuccess", this._mapRes);
            return this._mapRes;
        }
    }

    /**
     * Cancels any current execution of this procedure. This method does nothing if
     * no procedure call was currently executing.
     *
     * @methodOf RapidContext.Procedure.prototype
     */
    function cancel() {
        if (this._promise !== null) {
            this._promise.cancel();
            this._promise = null;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Cancels any current execution and removes the reference to the arguments of
     * this procedure.
     *
     * @methodOf RapidContext.Procedure.prototype
     */
    function reset() {
        this.cancel();
        this.args = null;
    }

    /**
     * Creates a new procedure caller for each key-value-pair in the specified
     * object.
     *
     * @memberOf RapidContext.Procedure
     * @param {Object} obj an object mapping keys to procedure names
     * @return {Object} an object mapping keys to procedure instances
     */
    function mapAll(obj) {
        var res = {};
        for (var k in obj) {
            res[k] = Procedure(obj[k]);
        }
        return res;
    }

    // Emits a signal via MochiKit.Signal
    function signal(src, sig, value) {
        try {
            if (value === undefined) {
                MochiKit.Signal.signal(src, sig);
            } else {
                MochiKit.Signal.signal(src, sig, value);
            }
        } catch (e) {
            var msg = ["exception in", src.procedure, sig, "handler:"].join(" ");
            (e.errors || [e]).forEach(function (err) {
                console.error(msg, err);
            });
        }
    }

    // Create namespace and export API
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    RapidContext.Procedure = Procedure;
    Object.assign(Procedure.prototype, { recall, multicall, cancel, reset });
    Object.assign(Procedure, { mapAll });

})(this);
