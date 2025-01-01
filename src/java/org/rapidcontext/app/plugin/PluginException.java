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

package org.rapidcontext.app.plugin;

/**
 * A plug-in exception. This class encapsulates all plug-in loading
 * and unloading errors.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class PluginException extends Exception {

    /**
     * Creates a new plug-in exception.
     *
     * @param message        the detailed error message
     */
    public PluginException(String message) {
        super(message);
    }
}
