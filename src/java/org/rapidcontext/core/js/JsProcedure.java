/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.js;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.rapidcontext.core.proc.AddOnProcedure;
import org.rapidcontext.core.proc.Bindings;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.Procedure;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Channel;

/**
 * A JavaScript procedure. This procedure will execute generic
 * JavaScript code allowing other procedures to be called.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class JsProcedure extends AddOnProcedure {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(JsProcedure.class.getName());

    /**
     * The binding name for the JavaScript code.
     */
    public static final String BINDING_CODE = "code";

    /**
     * The compiled script code. Set to null upon code changes.
     */
    private Script script = null;

    /**
     * Creates a new JavaScript procedure.
     *
     * @throws ProcedureException if the initialization failed
     */
    public JsProcedure() throws ProcedureException {
        defaults.set(BINDING_CODE, Bindings.DATA, "",
                     "The JavaScript source code to execute on procedure " +
                     "calls. Use a 'return' statement for the procedure result.");
        defaults.seal();
    }

    /**
     * Checks if this script has been compiled.
     *
     * @return true if this script has been compiled, or
     *         false otherwise
     */
    public boolean isCompiled() {
        return script != null;
    }

    /**
     * Executes a call of this procedure in the specified context
     * and with the specified call bindings. The semantics of what
     * the procedure actually does, is up to each implementation.
     * Note that the call bindings are normally inherited from the
     * procedure bindings with arguments bound to their call values.
     *
     * @param cx             the procedure call context
     * @param bindings       the call bindings to use
     *
     * @return the result of the call, or
     *         null if the call produced no result
     *
     * @throws ProcedureException if the call execution caused an
     *             error
     */
    public Object call(CallContext cx, Bindings bindings)
        throws ProcedureException {

        if (script == null) {
            compile();
        }
        Context scriptContext = Context.enter();
        try {
            scriptContext.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = scriptContext.initSafeStandardObjects();
            Object console = Context.javaToJS(new ConsoleObject(getName()), scope);
            ScriptableObject.putProperty(scope, "console", console);
            String[] names = bindings.getNames();
            for (int i = 0; i < names.length; i++) {
                int type = bindings.getType(names[i]);
                Object value = bindings.getValue(names[i], null);
                if (BINDING_CODE.equals(names[i])) {
                    // Do nothing
                } else if (type == Bindings.DATA) {
                    ScriptableObject.putProperty(scope, names[i], value);
                } else if (type == Bindings.PROCEDURE) {
                    value = new ProcedureWrapper(cx, (Procedure) value, scope);
                    ScriptableObject.putProperty(scope, names[i], value);
                } else if (type == Bindings.CONNECTION) {
                    value = new ConnectionWrapper(cx, (Channel) value, scope);
                    ScriptableObject.putProperty(scope, names[i], value);
                } else if (type == Bindings.ARGUMENT) {
                    value = JsSerializer.wrap(value, scope);
                    ScriptableObject.putProperty(scope, names[i], value);
                }
            }
            Object res = script.exec(scriptContext, scope);
            boolean unwrapResult = (cx.getCallStack().height() <= 1);
            return unwrapResult ? JsSerializer.unwrap(res) : res;
        } catch (Exception e) {
            throw createException(e);
        } finally {
            Context.exit();
        }
    }

    /**
     * Compiles the script code. This will be done automatically the
     * first time the procedure is run, but may be practical to do at
     * other times as well in order to detect errors.
     *
     * @throws ProcedureException if the script couldn't be compiled
     *             correctly
     */
    public void compile() throws ProcedureException {
        JsErrorHandler  errorHandler = new JsErrorHandler();
        Context         cx;
        String[]        lines;
        String          str;
        StringBuffer    code = new StringBuffer();
        int             idx;

        cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setErrorReporter(errorHandler);
            str = (String) getBindings().getValue(BINDING_CODE);
            lines = str.split("\n");
            idx = lines.length - 1;
            while (idx >= 0 && lines[idx].trim().length() <= 0) {
                idx--;
            }
            // TODO: remove this backwards-compatibility hack
            if (idx >= 0) {
                str = lines[idx];
                if (!str.contains("return") && !str.contains("throw")) {
                    lines[idx] = "return " + str;
                }
            }
            code.append("(function () {\n");
            for (int i = 0; i < lines.length; i++) {
                code.append(lines[i]);
                code.append("\n");
            }
            code.append("})();");
            script = cx.compileString(code.toString(), getName(), 0, null);
            if (errorHandler.getErrorCount() > 0) {
                throw new ProcedureException(errorHandler.getErrorText());
            }
        } catch (Exception e) {
            script = null;
            if (errorHandler.getErrorCount() > 0) {
                throw new ProcedureException(errorHandler.getErrorText());
            }
            throw createException(e);
        } finally {
            Context.exit();
        }
    }

    /**
     * Creates a procedure exception from any exception type.
     *
     * @param e              the exception to convert
     *
     * @return the procedure exception
     */
    private ProcedureException createException(Throwable e) {
        if (e instanceof ProcedureException) {
            return (ProcedureException) e;
        } else if (e instanceof WrappedException) {
            return createException(((WrappedException) e).getWrappedException());
        } else {
            LOG.log(Level.WARNING, "Caught unhandled exception", e);
            return new ProcedureException("Caught unhandled exception", e);
        }
    }
}
