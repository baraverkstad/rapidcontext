/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2009 Per Cederberg.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

// Create default RapidContext object
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}

/**
 * Creates a new procedure caller. This class provides a simplified way of
 * calling a procedure and connecting with UI elements through signals. This
 * function can be called either as a plain function or as a constructor with
 * identical results.
 *
 * @constructor
 * @param {String} name the procedure name
 * @property {String} name The procedure name.
 * @property {Array} arguments The arguments used in the last call.
 */
RapidContext.Procedure = function (name) {
    if (!(this instanceof arguments.callee)) {
        return new arguments.callee(name);
    }
    this.name = name;
    this.arguments = null;
    this._deferred = null;
}

/**
 * Creates a new procedure caller for each key-value-pair in the specified
 * object.
 *
 * @param {Object} obj an object mapping keys to procedure names
 *
 * @return {Object} an object mapping keys to procedure instances
 */
RapidContext.Procedure.mapAll = function (obj) {
    var res = {};
    for (var k in obj) {
        res[k] = new RapidContext.Procedure(obj[k]);
    }
    return res;
}

/**
 * Emitted when the procedure is called. Each call corresponds to exactly one
 * "oncall" and one "onresponse" event (even if the call was cancelled). No
 * event data will be sent.
 *
 * @name RapidContext.Procedure#oncall
 * @event
 */

/**
 * Emitted when the procedure response has been received. Each call corresponds
 * to exactly one "oncall" and one "onresponse" event (even if the call was
 * cancelled). The call response or error object will be sent as the event
 * data.
 *
 * @name RapidContext.Procedure#onresponse
 * @event
 */

/**
 * Emitted when a procedure call returned a result. This event is emitted after
 * the "onresponse" event, but only if the procedure call actually succeeded.
 * Use the "onerror" or "oncancel" signals for other result statuses. The call
 * response object will be sent as the event data.
 *
 * @name RapidContext.Procedure#onsuccess
 * @event
 */

/**
 * Emitted when a procedure call failed. This event is emitted after the
 * "onresponse" event, but only if the procedure call returned an error. Use
 * the "onsuccess" or "oncancel" for other result statuses. The call error
 * object will be sent as the event data.
 *
 * @name RapidContext.Procedure#onerror
 * @event
 */

/**
 * Emitted when a procedure call was cancelled. This event is emitted after the
 * "onresponse" event, but only if the procedure call was cancelled. Use the
 * "onsuccess" or "onerror" for other result statuses. No event data will be
 * sent.
 *
 * @name RapidContext.Procedure#oncancel
 * @event
 */

/**
 * Calls the procedure with the specified arguments. The call is asynchronous,
 * so results will not be returned by this method. Instead the results will be
 * available through the "onsuccess" signal, for example. Note that any
 * previously running call will automatically be cancelled, since only a single
 * call can be processed at any time.
 *
 * @param {Object} [...] the procedure call arguments
 *
 * @return {Deferred} a MochiKit.Async.Deferred object that will
 *         callback with the response data or error
 */
RapidContext.Procedure.prototype.call = function (/*...*/) {
    this.arguments = MochiKit.Base.extend(null, arguments);
    return this.recall();
}

/**
 * Calls the procedure with the same arguments as used in the last call. The
 * call is asynchronous, so results will not be returned by this method.
 * Instead the results will be available through the "onsuccess" signal, for
 * example. Note that any previously running call will automatically be
 * cancelled, since only a single call can be processed at any time.
 *
 * @return {Deferred} a MochiKit.Async.Deferred object that will
 *         callback with the response data or error
 */
RapidContext.Procedure.prototype.recall = function () {
    if (this.arguments === null) {
        throw new Error("No arguments supplied for procedure call to " + this.name);
    }
    this.cancel();
    MochiKit.Signal.signal(this, "oncall");
    this._deferred = RapidContext.App.callProc(this.name, this.arguments);
    this._deferred.addBoth(MochiKit.Base.bind("_callback", this));
    return this._deferred;
}

/**
 * The procedure deferred callback handler. Dispatches the appropriate signals
 * depending on the result. 
 *
 * @param {Object/Error} res the procedure result object
 */
RapidContext.Procedure.prototype._callback = function (res) {
    this._deferred = null;
    MochiKit.Signal.signal(this, "onresponse", res);
    if (res instanceof MochiKit.Async.CancelledError) {
        MochiKit.Signal.signal(this, "oncancel");
    } else if (res instanceof Error) {
        MochiKit.Signal.signal(this, "onerror", res);
    } else {
        MochiKit.Signal.signal(this, "onsuccess", res);
    }
    return res;
}

/**
 * Cancels any current execution of this procedure. This method does nothing if
 * no procedure call was currently executing. 
 */
RapidContext.Procedure.prototype.cancel = function () {
    if (this._deferred !== null) {
        this._deferred.cancel();
        return true;
    } else {
        return false;
    }
}

/**
 * Cancels any current execution and removes the reference to the arguments of
 * this procedure. 
 */
RapidContext.Procedure.prototype.reset = function () {
    this.cancel();
    this.arguments = null;
}
