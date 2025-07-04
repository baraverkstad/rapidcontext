/*
 * RapidContext HTTP plug-in <https://www.rapidcontext.com/>
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

package org.rapidcontext.app.plugin.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.logging.Logger;

import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.proc.ProcedureException;
import org.rapidcontext.core.type.Channel;
import org.rapidcontext.core.type.ConnectionException;
import org.rapidcontext.util.HttpUtil;

/**
 * An HTTP connection channel. This is not a real HTTP connection,
 * but rather just a utility for accessing HTTP connection parameters
 * stored in the environment.
 *
 * @author Per Cederberg
 */
public class HttpChannel extends Channel {

    /**
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(HttpChannel.class.getName());

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
    @Override
    protected boolean isPoolable() {
        return false;
    }

    /**
     * Reserves and activates the channel. This method is called just
     * before a channel is to be used, i.e. when a new channel has
     * been created or fetched from a resource pool.
     */
    @Override
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
    @Override
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
    @Override
    @SuppressWarnings("resource")
    public void validate() {
        String method = ((HttpConnection) connection).validateMethod();
        if (method != null && !method.isBlank()) {
            try {
                CallContext cx = ((HttpConnection) connection).callContext();
                HttpClient client = HttpRequestProcedure.defaultClient();
                HttpRequest req = HttpRequestProcedure.buildRequest(uri(), method, headers(), null);
                HttpLog.logRequest(cx, req, null);
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                HttpLog.logResponse(cx, resp);
                if (resp.statusCode() / 100 != 2) {
                    throw new IOException("invalid response: HTTP " + resp.statusCode());
                }
                report(0, true, null);
            } catch (Exception e) {
                String msg = "validation failure: " + e.getMessage();
                LOG.warning(msg);
                report(0, false, msg);
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
    @Override
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
    @Override
    public void rollback() {
        // Not supported
    }

    /**
     * Returns the base URI for the connection.
     *
     * @return the base URI for the connection
     *
     * @throws ProcedureException if the base URI was malformed
     */
    protected URI uri() throws ProcedureException {
        String url = ((HttpConnection) connection).url();
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new ProcedureException("invalid URL: " + url);
        }
    }

    /**
     * Returns the default HTTP headers for the connection. Note that an
     * authorization header will be included if configured.
     *
     * @return the default HTTP headers for the connection, or
     *         an empty string if not set
     *
     * @throws ProcedureException if an HTTP header line was invalid
     */
    protected Map<String,String> headers() throws ProcedureException {
        String str = ((HttpConnection) connection).headers();
        Map<String,String> headers = HttpRequestProcedure.parseHeaders(str);
        try {
            String auth = ((HttpConnection) connection).authRefresh();
            if (auth != null && !auth.isBlank()) {
                headers.put(HttpUtil.Header.AUTHORIZATION, auth);
            }
        } catch (ConnectionException e) {
            throw new ProcedureException(e.getMessage());
        }
        return headers;
    }
}
