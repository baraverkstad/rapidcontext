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

import java.util.Date;
import java.util.logging.Logger;

import org.rapidcontext.app.ApplicationContext;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.proc.CallContext;
import org.rapidcontext.core.type.Channel;
import org.rapidcontext.core.type.Connection;
import org.rapidcontext.core.type.ConnectionException;
import org.rapidcontext.util.ValueUtil;

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
     * The class logger.
     */
    private static final Logger LOG = Logger.getLogger(HttpConnection.class.getName());

    /**
     * The HTTP base URL configuration parameter name.
     */
    public static final String HTTP_URL = "url";

    /**
     * The HTTP header configuration parameter name.
     */
    public static final String HTTP_HEADERS = "headers";

    /**
     * The HTTP validation method configuration parameter name.
     */
    public static final String HTTP_VALIDATE = "validate";

    /**
     * The HTTP authentication configuration parameter name.
     */
    public static final String HTTP_AUTH = "auth";

    /**
     * The computed authorization header parameter name.
     */
    public static final String AUTH_HEADER = "header";

    /**
     * The computed authorization expiry date parameter name.
     */
    public static final String AUTH_EXPIRES = "expires";

    /**
     * Normalizes an HTTP connection data object if needed. This method
     * will modify legacy data into the proper keys and values.
     *
     * @param id             the object identifier
     * @param dict           the storage data
     *
     * @return the storage data (possibly modified)
     */
    public static Dict normalize(String id, Dict dict) {
        // TODO: Remove this legacy conversion (added 2017-02-01)
        if (dict.containsKey("header")) {
            LOG.warning("deprecated: connection " + id + " data: legacy header");
            String headers = dict.get("header", String.class, "");
            dict.remove("header");
            dict.set(HTTP_HEADERS, headers);
        }
        return dict;
    }

    /**
     * Creates a new HTTP connection from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     */
    public HttpConnection(String id, String type, Dict dict) {
        super(id, type, normalize(id, dict));
    }

    /**
     * Checks if this object is in active use. This method will return
     * true if the object was activated during the last 5 minutes.
     *
     * @return true if the object is considered active, or
     *         false otherwise
     */
    @Override
    protected boolean isActive() {
        long now = System.currentTimeMillis();
        return super.isActive() || authActive(now) != null;
    }

    /**
     * Returns the base URL.
     *
     * @return the base URL, or an empty string
     */
    public String url() {
        return dict.get(dictKey(HTTP_URL), String.class, "");
    }

    /**
     * Returns the default HTTP headers.
     *
     * @return the default HTTP headers, or
     *         an empty string if not set
     */
    public String headers() {
        return dict.get(dictKey(HTTP_HEADERS), String.class, "");
    }

    /**
     * Returns the HTTP method for validating the connection.
     *
     * @return the HTTP method for validation, or
     *         an empty string if disabled
     */
    public String validateMethod() {
        return dict.get(HTTP_VALIDATE, String.class, "");
    }

    /**
     * Returns the computed authorization header if still valid.
     *
     * @param now
     *
     * @return the computed authorization header, or
     *         null if not available or expired
     */
    protected String authActive(long now) {
        Dict conf = dict.getDict(dictKey(HTTP_AUTH));
        if (conf.containsKey(AUTH_HEADER)) {
            Date expires = conf.get(AUTH_EXPIRES, Date.class);
            if (expires == null || now < expires.getTime()) {
                return conf.get(AUTH_HEADER, String.class);
            } else {
                dict.remove(PREFIX_COMPUTED + HTTP_AUTH);
            }
        }
        return null;
    }

    /**
     * Returns or computes an authorization header if configured.
     *
     * @return the computed authorization header, or
     *         null if not configured
     *
     * @throws ConnectionException if the authentication wasn't successful
     */
    protected synchronized String authRefresh() throws ConnectionException {
        long now = System.currentTimeMillis();
        if (!dict.containsKey(dictKey(HTTP_AUTH))) {
            return null;
        } else if (authActive(now) instanceof String res) {
            return res;
        } else {
            try {
                Dict conf = dict.getDict(dictKey(HTTP_AUTH));
                Object val = callContext().execute("http/auth", new Object[] { conf });
                if (val instanceof Dict d) {
                    String header = d.getElse(AUTH_HEADER, String.class, () -> {
                        String token = d.get("access_token", String.class);
                        return (token == null) ? null : "Bearer " + token;
                    });
                    Date expires = d.getElse(AUTH_EXPIRES, Date.class, () -> {
                        long expIn = d.get("expires_in", Long.class, 0L);
                        return (expIn > 0L) ? new Date(now + expIn * 1000L) : null;
                    });
                    if (header != null && expires != null) {
                        Dict act = new Dict()
                            .set(AUTH_HEADER, header)
                            .set(AUTH_EXPIRES, expires);
                        dict.set(PREFIX_COMPUTED + HTTP_AUTH, act);
                    }
                    return header;
                } else {
                    return ValueUtil.convert(val, String.class);
                }
            } catch (Exception e) {
                throw new ConnectionException("auth error: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Creates a new connection channel.
     *
     * @return the channel created
     *
     * @throws ConnectionException if the channel couldn't be created
     *             properly
     */
    @Override
    protected Channel createChannel() throws ConnectionException {
        return new HttpChannel(this);
    }

    /**
     * Destroys a connection channel, freeing any resources used
     * (such as database connections, networking sockets, etc).
     *
     * @param channel        the channel to destroy
     */
    @Override
    protected void destroyChannel(Channel channel) {
        // Nothing to do, HTTP channels close automatically
    }

    /**
     * Returns the call context for the active thread.
     *
     * @return the current call context
     */
    @SuppressWarnings("removal")
    protected CallContext callContext() {
        return ApplicationContext.getInstance().findContext(Thread.currentThread());
    }
}
