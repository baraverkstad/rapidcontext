/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg.
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

package org.rapidcontext.core.data;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * A data selector representation. This class encapsulates a data
 * query or search path, providing simple access to information about
 * the query itself.
 *
 * @author Per Cederberg
 * @version 1.0
 */
public class DataSelector {

    /**
     * The index flag. This flag is set if the selector corresponds
     * to an index or list of data objects.
     */
    public boolean isIndex = false;

    /**
     * The path components. The last element in the path array is the
     * object id, and any previous elements correspond to parent
     * indices (data type or path). The root index will have a zero
     * length path.
     */
    public String[] path = null;

    /**
     * Creates a new data selector from a query string (similar to
     * an file system path for example).
     *
     * @param query          the query string to parse
     */
    public DataSelector(String query) {
        query = StringUtils.stripStart(query, "/");
        this.isIndex = query.equals("") || query.endsWith("/");
        query = StringUtils.stripEnd(query, "/");
        if (query.equals("")) {
            this.path = ArrayUtils.EMPTY_STRING_ARRAY;
        } else {
            this.path = query.split("/");
        }
    }

    /**
     * Checks if this selector corresponds to the root index.
     *
     * @return true if the selector is for the root index, or
     *         false otherwise
     */
    public boolean isRoot() {
        return isIndex && path.length == 0;
    }

    /**
     * Returns the selector depth. The root index, and any objects
     * accessible there, have depth zero (0). For each additional
     * index (directory) traversed, the depth increases by one (1).
     * Non-index leafs in the data tree will not affect the depth.
     *
     * @return the selector depth
     */
    public int depth() {
        return path.length - (isIndex ? 0 : 1);
    }
}
