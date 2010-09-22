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
 * A data query representation. This class handles parsing of data
 * queries and provides simple meta-data about them.
 *
 * @author Per Cederberg
 * @version 1.0
 */
public class DataQuery {

    /**
     * The index query flag. Will be set if the query requests an
     * index of data objects.
     */
    public boolean isIndex = false;

    /**
     * The path components of the query.
     */
    public String[] path = null;

    /**
     * Parses a query string into a data query.
     *
     * @param query          the query string to parse
     */
    public DataQuery(String query) {
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
     * Checks if this query corresponds to the root index.
     *
     * @return true if the query is for the root index, or
     *         false otherwise
     */
    public boolean isRoot() {
        return isIndex && path.length == 0;
    }

    /**
     * Returns the query depth. The root index is on depth 0 and any
     * additional indices traversed, will add 1 to the depth.
     * Non-index leafs in the query tree will not affect the depth.
     *
     * @return the (hierarchical) query depth
     */
    public int depth() {
        return path.length - (isIndex ? 0 : 1);
    }
}
