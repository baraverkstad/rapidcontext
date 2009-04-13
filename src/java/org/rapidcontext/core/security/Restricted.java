/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2009 Per Cederberg & Dynabyte AB.
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

package org.rapidcontext.core.security;

/**
 * A restricted resource interface. Most built-in objects should
 * implement this interface to provide default access mappings, as
 * it is otherwise too easy for users to reconfigure the access
 * mappings for the built-in procedures in an unsafe or incorrect
 * way. By implementing this interface the security settings are
 * locked and cannot be modified by the user in the role access
 * mappings. 
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public interface Restricted {

    /**
     * Checks if the currently authenticated user has access to this
     * object.
     *
     * @return true if the current user has access, or
     *         false otherwise
     */
    public boolean hasAccess();
}
