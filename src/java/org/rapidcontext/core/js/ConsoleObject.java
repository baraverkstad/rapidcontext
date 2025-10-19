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

package org.rapidcontext.core.js;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * A JavaScript console object. This class provides a subset of the standard
 * JavaScript console object for logging.
 *
 * @author Per Cederberg
 */
public class ConsoleObject extends ScriptableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(ConsoleObject.class.getName());

    /**
     * The log prefix.
     */
    private String prefix;

    /**
     * The console methods (lazy creation on request).
     */
    private HashMap<String, Function> methods = new HashMap<>();

    /**
     * Creates a new console object with a specific prefix.
     *
     * @param prefix         the logging prefix text
     * @param parentScope    the object parent scope
     */
    @SuppressWarnings("this-escape")
    public ConsoleObject(String prefix, Scriptable parentScope) {
        super(parentScope, getObjectPrototype(parentScope));
        this.prefix = prefix;
        setAttributes("error", READONLY | PERMANENT);
        setAttributes("warn", READONLY | PERMANENT);
        setAttributes("info", READONLY | PERMANENT);
        setAttributes("log", READONLY | PERMANENT);
    }

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    @Override
    public String getClassName() {
        return "Console";
    }

    /**
     * Returns a named property from this object.
     *
     * @param name           the name of the property
     * @param start          the object in which the lookup began
     *
     * @return the value of the property, or
     *         NOT_FOUND if not found
     */
    @Override
    public Object get(String name, Scriptable start) {
        return switch(name) {
            case "error", "warn", "info", "log" -> getMethod(name);
            default -> super.get(name, start);
        };
    }

    /**
     * Gets or creates the corresponding method.
     *
     * @param name           the method name
     *
     * @return the JS method object
     */
    private Function getMethod(String name) {
        if (!methods.containsKey(name)) {
            for (Method m : getClass().getMethods()) {
                boolean isPublic = (m.getModifiers() & Modifier.PUBLIC) > 0;
                if (isPublic && m.getName().equals(name)) {
                    NativeJavaMethod method = new NativeJavaMethod(m, name);
                    method.setParentScope(this);
                    method.setPrototype(getFunctionPrototype(this));
                    methods.put(name, method);
                    break;
                }
            }
        }
        return methods.get(name);
    }

    /**
     * Logs an error message.
     *
     * @param args           the log message arguments
     */
    public void error(Object ...args) {
        logInternal(Level.SEVERE, args);
    }

    /**
     * Logs a warning message.
     *
     * @param args           the log message arguments
     */
    public void warn(Object ...args) {
        logInternal(Level.WARNING, args);
    }

    /**
     * Logs an info message.
     *
     * @param args           the log message arguments
     */
    public void info(Object ...args) {
        logInternal(Level.INFO, args);
    }

    /**
     * Logs a debug message.
     *
     * @param args           the log message arguments
     */
    public void log(Object ...args) {
        logInternal(Level.FINE, args);
    }

    /**
     * Logs a message.
     *
     * @param level          the log level
     * @param args           the log message arguments
     */
    private void logInternal(Level level, Object ...args) {
        for (int i = 0; i < args.length; i++) {
            args[i] = JsRuntime.unwrap(args[i]);
        }
        LOG.log(level, this.prefix + ": " + StringUtils.join(args, " "));
    }
}
