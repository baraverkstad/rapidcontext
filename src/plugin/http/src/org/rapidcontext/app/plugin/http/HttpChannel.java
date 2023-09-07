/*
 * RapidContext HTTP plug-in <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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

package org.rapidcontext.app.plugin.http;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Channel;

/**
 * An HTTP connection channel. This is not a real HTTP connection,
 * but rather just a utility for accessing HTTP connection parameters
 * stored in the environment.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class HttpChannel extends Channel {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(HttpProcedure.class.getName());

    /**
     * Creates a new HTTP connection channel.
     *
     * @param parent         the parent HTTP connection
     */
    HttpChannel(HttpConnection parent) {
        super(parent);
    }

    /**
     * Checks if this channel can be pooled (i.e. reused). This
     * method should return the same value for all instances of a
     * specific channel subclass.
     *
     * @return true if the channel can be pooled and reused, or
     *         false otherwise
     */
    protected boolean isPoolable() {
        return false;
    }

    /**
     * Reserves and activates the channel. This method is called just
     * before a channel is to be used, i.e. when a new channel has
     * been created or fetched from a resource pool.
     */
    protected void reserve() {
        // Nothing to do here
    }

    /**
     * Releases and passivates the channel. This method is called
     * just after a channel has been used and returned. This should
     * clear or reset the channel, so that it can safely be used
     * again without affecting previous results or operations (if
     * the channel is pooled).
     */
    protected void release() {
        // Nothing to do here
    }

    /**
     * Checks if the channel connection is still valid. This method
     * is called before using a channel and regularly when it is idle
     * in the pool. It can be used to trigger a "ping" for a channel.
     * This method can only mark a valid channel as invalid, never
     * the other way around.
     */
    public void validate() {
        String method = ((HttpConnection) connection).validateMethod();
        if (method != null && method.trim().length() > 0) {
            try {
                HttpURLConnection con = HttpProcedure.setup(getUrl(), false);
                try {
                    HttpProcedure.setRequestHeaders(con, getHeaders());
                    HttpProcedure.setRequestMethod(con, method);
                    HttpProcedure.logRequest(null, con, null);
                    con.connect();
                    int code = con.getResponseCode();
                    HttpProcedure.logResponse(null, con, HttpProcedure.responseText(con));
                    if (code / 100 != 2) {
                        LOG.warning("validation failure, invalid response: HTTP " + code);
                        invalidate();
                    }
                } finally {
                    con.disconnect();
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "validation failure", e);
                invalidate();
            }
        }
    }

    /**
     * Commits any pending changes. This method is called after each
     * successful procedure tree execution that included this channel.
     * This method may be implemented as a no-op, if no support for
     * commit and rollback semantics is available.<p>
     *
     * In case of error, a subclass should log the message and
     * invalidate the channel.
     */
    public void commit() {
        // Not supported
    }

    /**
     * Rolls any pending changes back. This method is called after an
     * unsuccessful procedure tree execution that included this
     * channel. This method may be implemented as a no-op, if no
     * support for commit and rollback semantics is available.<p>
     *
     * In case of error, a subclass should log the message and
     * invalidate the channel.
     */
    public void rollback() {
        // Not supported
    }

    /**
     * Returns the base URL for the connection.
     *
     * @return the base URL for the connection
     *
     * @throws ProcedureException if the base URL was malformed
     */
    public URL getUrl() throws ProcedureException {
        String url = ((HttpConnection) connection).url();
        try {
            return new URI(url).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new ProcedureException("invalid URL: " + url);
        }
    }

    /**
     * Returns the default HTTP headers for the connection.
     *
     * @return the default HTTP headers for the connection, or
     *         an empty string if not set
     */
    public String getHeaders() {
        return ((HttpConnection) connection).headers();
    }
}
