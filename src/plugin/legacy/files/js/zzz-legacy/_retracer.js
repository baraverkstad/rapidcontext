/**
 * @namespace Provides legacy ReTracer API functions.
 * @deprecated
 */
retracer = {}

/**
 * Calls a remote procedure. The call is asynchronous and the
 * response will be sent to the callback method specified, first
 * argument being the response data and the second being an error
 * string.
 *
 * @param name               the procedure name
 * @param args               the array of arguments, or null
 * @param method             the callback function, method or null
 * @param obj                the callback object, or null
 * @param trackId            the call tracking id, or null
 *
 * @deprecated Use ReTracer.App.callProc instead.
 */
retracer.call = function(name, args, method, obj, trackId) {
    var trace = ReTracer.Util.stackTrace();
    var d = ReTracer.App.callProc(name, args);
    if (method != null) {
        d.addCallback(function(res) {
            ReTracer.Util.injectStackTrace(trace);
            method.call(obj, res, null, trackId);
        });
        d.addErrback(function(err) {
            ReTracer.Util.injectStackTrace(trace);
            method.call(obj, null, err.message, trackId);
        });
    }
}

// Alias for "ReTracer"
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
var ReTracer = RapidContext;
