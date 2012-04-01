/**
 * @namespace Provides functions for event handling via signals.
 */
Signal = {};

/**
 * Connects a slot function to a signal function. Note that any
 * function can be used either as a signal, a slot or both. The slot
 * function will only be called on successful execution of the signal
 * function.
 *
 * @param src                the signal source object
 * @param signal             the signal function name
 * @param destOrFunc         the slot object or slot function
 * @param funcOrStr          the slot function or function name
 */
Signal.connect = function(src, signal, destOrFunc, funcOrStr) {
    var slot;

    if (typeof(signal) != "string") {
        throw new Error("signal name must be a string");
    }
    slot = Signal._createSlot(destOrFunc, funcOrStr);
    this._connectDispatcher(src, signal);
    src[signal].postFuncs.push(slot);
}

/**
 * Connects a slot function to a signal function. Note that any
 * function can be used either as a signal, a slot or both. The slot
 * function will always be called before the signal function.
 *
 * @param src                the signal source object
 * @param signal             the signal function name
 * @param destOrFunc         the slot object or slot function
 * @param funcOrStr          the slot function or function name
 */
Signal.connectBefore = function(src, signal, destOrFunc, funcOrStr) {
    var slot;

    if (typeof(signal) != "string") {
        throw new Error("signal name must be a string");
    }
    slot = Signal._createSlot(destOrFunc, funcOrStr);
    this._connectDispatcher(src, signal);
    src[signal].preFuncs.push(slot);
}

/**
 * Disconnects all slot functions from a signal function or from an
 * object.
 *
 * @param src                the signal source object
 * @param signal             the optional signal function name
 */
Signal.disconnectAll = function(src, signal) {
    if (src == null) {
        throw new Error("signal object cannot be null");
    }
    if (signal != null) {
        if (typeof(signal) != "string") {
            throw new Error("signal name must be a string");
        }
        this._disconnectDispatcher(src, signal);
    } else {
        for (var n in src) {
            if (typeof(src[n]) == "function") {
                this._disconnectDispatcher(src, n);
            }
        }
    }
}

/**
 * Creates a new signal slot function. If the slot function couldn't
 * be properly created, an exception will be thrown.
 *
 * @param destOrFunc         the slot object or slot function
 * @param funcOrStr          the slot function or function name
 *
 * @return the slot function object
 *
 * @private
 */
Signal._createSlot = function(destOrFunc, funcOrStr) {
    if (typeof(funcOrStr) == "string") {
        if (typeof(destOrFunc[funcOrStr]) != "function") {
            throw new Error("signal slot '" + funcOrStr +
                            "' is not a function");
        }
        return FunctionUtil.bind(funcOrStr, destOrFunc);
    } else if (typeof(funcOrStr) == "function") {
        return FunctionUtil.bind(funcOrStr, destOrFunc);
    } else if (typeof(destOrFunc) == "function") {
        return destOrFunc;
    } else {
        throw new Error("signal slot is not a function");
    }
}

/**
 * Connects a dispatcher function to a signal if not already done.
 *
 * @param src                the signal source object
 * @param signal             the signal function name
 *
 * @private
 */
Signal._connectDispatcher = function(src, signal) {
    var value;

    if (src == null) {
        throw new Error("signal object cannot be null");
    } else if (!(signal in src)) {
        throw new Error("signal '" + signal + "' doesn't exist in object");
    }
    value = src[signal];
    if (value != null && typeof(value.signalFunc) == "function") {
        return;
    }
    src[signal] = function() {
        var self = arguments.callee;
        var res;
        Signal._callSlots(self.dispatchName, self.preFuncs, this, arguments);
        if (typeof(self.signalFunc) == "function") {
            res = self.signalFunc.apply(this, arguments);
        }
        Signal._callSlots(self.dispatchName, self.postFuncs, this, arguments);
        return res;
    }
    src[signal].signalFunc = value;
    src[signal].preFuncs = [];
    src[signal].postFuncs = [];
    src[signal].dispatchName = ReTracer.Util.functionName(value) || signal;
    src[signal].dispatchName += ".dispatch";
    ReTracer.Util.registerFunctionNames(src[signal], null);
}

/**
 * Disconnects a dispatcher function from a signal if not already done.
 *
 * @param src                the signal source object
 * @param signal             the signal function name
 *
 * @private
 */
Signal._disconnectDispatcher = function(src, signal) {
    var value;

    if (src == null) {
        throw new Error("signal object cannot be null");
    } else if (!(signal in src)) {
        throw new Error("signal '" + signal + "' doesn't exist in object");
    }
    value = src[signal];
    if (value == null || typeof(value.signalFunc) != "function") {
        return;
    }
    src[signal] = value.signalFunc;
    value.signalFunc = null;
    value.preFuncs = null;
    value.postFuncs = null;
}

/**
 * Calls an array of slot functions. Any return values will be
 * discarded and exceptions will be caught and logged.
 *
 * @param signalName         the signal function name
 * @param slots              the array of slot functions
 * @param obj                the object to apply on
 * @param args               the call arguments
 *
 * @private
 */
Signal._callSlots = function(signalName, slots, obj, args) {
    for (var i = 0; i < slots.length; i++) {
        try {
            ReTracer.Util.injectStackTrace([signalName]);
            slots[i].apply(obj, args);
        } catch (e) {
            var name = ReTracer.Util.functionName(slots[i]);
            if (name) {
                ReTracer.Util.injectStackTrace([name]);
            }
            LOG.error("Uncaught exception in call from " + signalName, e.message);
        }
    }
}
