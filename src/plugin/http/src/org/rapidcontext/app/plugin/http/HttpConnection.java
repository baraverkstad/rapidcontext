/**
 * RapidContext HTTP plug-in <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE for more details.
 */

package org.rapidcontext.app.plugin.http;

import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.type.Channel;
import org.rapidcontext.core.type.Connection;
import org.rapidcontext.core.type.ConnectionException;

/**
 * A virtual HTTP connection. This class allows storing HTTP
 * connection parameters in the environment instead of in the HTTP
 * procedures. The actual HTTP (TCP/IP) connections are not managed
 * in this class or the corresponding channel, but rather in the HTTP
 * procedures.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class HttpConnection extends Connection {

    /**
     * The HTTP base URL configuration parameter name.
     */
    public static final String HTTP_URL = "url";

    /**
     * The HTTP header configuration parameter name.
     */
    public static final String HTTP_HEADERS = "headers";

    /**
     * Creates a new HTTP connection from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public HttpConnection(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Creates a new connection channel.
     *
     * @return the channel created
     *
     * @throws ConnectionException if the channel couldn't be created
     *             properly
     */
    protected Channel createChannel() throws ConnectionException {
        return new HttpChannel(this, dict);
    }

    /**
     * Destroys a connection channel, freeing any resources used
     * (such as database connections, networking sockets, etc).
     *
     * @param channel        the channel to destroy
     */
    protected void destroyChannel(Channel channel) {
        // Nothing to do, HTTP channels close automatically
    }
}
