/*
 * RapidContext <https://www.rapidcontext.com/>
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

package org.rapidcontext.core.type;

/**
 * A communications channel for a connection. A channel provides
 * data transport to an external system, such as a database, a
 * message bus or similar. Depending on the channel subclass, it
 * might be possible to reuse a channel multiple times (resource
 * pooling) or share it between several parallel tasks.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class Channel {

    /**
     * The valid channel flag. Should only be changed to false, never
     * back to true.
     */
    protected boolean valid = true;

    /**
     * The recent error count. Will be set to zero (0) on success.
     * If count reaches 3, the channel is invalidated.
     */
    protected int errors = 0;

    /**
     * The parent connection for this channel.
     */
    protected Connection connection;

    /**
     * Creates a new communications channel for the specified connection.
     *
     * @param con            the parent connection
     */
    protected Channel(Connection con) {
        this.connection = con;
    }

    /**
     * Checks if this channel is considered valid.
     *
     * @return true if this channel is valid, or
     *         false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Checks if this channel can be pooled (i.e. reused). This
     * method should return the same value for all instances of a
     * specific channel subclass.
     *
     * @return true if the channel can be pooled and reused, or
     *         false otherwise
     */
    protected abstract boolean isPoolable();

    /**
     * Returns the connection that this channel belongs to.
     *
     * @return the connection for this channel
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * Reserves and activates the channel. This method is called just
     * before a channel is to be used, i.e. when a new channel has
     * been created or fetched from a resource pool.
     *
     * @throws ConnectionException if the channel couldn't be
     *             reserved (channel will be destroyed)
     */
    protected abstract void reserve() throws ConnectionException;

    /**
     * Releases and passivates the channel. This method is called
     * just after a channel has been used and returned. This should
     * clear or reset the channel, so that it can safely be used
     * again without affecting previous results or operations (if
     * the channel is pooled).
     *
     * @throws ConnectionException if the channel couldn't be
     *             released (channel will be destroyed)
     */
    protected abstract void release() throws ConnectionException;

    /**
     * Checks if the channel connection is still valid. This method
     * is called before using a channel and regularly when it is idle
     * in the pool. It can be used to trigger a "ping" for a channel.
     * This method can only mark a valid channel as invalid, never
     * the other way around.
     *
     * @see #isValid()
     * @see #invalidate()
     */
    public abstract void validate();

    /**
     * Marks the channel as invalid, meaning that it should no longer
     * be used and is scheduled for destruction when returned to the
     * parent connection. During normal operations, this method
     * should not be called, since it defeats the channel pooling
     * capabilities used by some connections.
     *
     * @see #isValid()
     * @see #validate()
     */
    public void invalidate() {
        valid = false;
    }

    /**
     * Reports on successful or failed usage of the channel.
     *
     * @param success        the success flag
     * @param message        the error message, or null
     */
    public void report(boolean success, String message) {
        errors = success ? 0 : errors + 1;
        if (errors >= 3) {
            invalidate();
        }
        if (!success && message != null) {
            connection.lastError = message;
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
    public abstract void commit();

    /**
     * Rolls any pending changes back. This method is called after an
     * unsuccessful procedure tree execution that included this
     * channel. This method may be implemented as a no-op, if no
     * support for commit and rollback semantics is available.<p>
     *
     * In case of error, a subclass should log the message and
     * invalidate the channel.
     */
    public abstract void rollback();
}
