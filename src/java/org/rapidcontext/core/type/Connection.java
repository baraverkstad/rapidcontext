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

import java.time.Duration;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.storage.Path;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.core.storage.Storage;
import org.rapidcontext.core.storage.StorageException;

/**
 * A connection to an external system. This is an abstract base
 * class, providing a number of services:
 *
 * <ul>
 *   <li><strong>On-Demand Creation</strong> -- Both connections and
 *       their managed communication channels are created and
 *       initialized on demand.
 *   <li><strong>Usage Limits</strong> -- Configurable limits for
 *       the maximum number of communication channels are
 *       automatically upheld.
 *   <li><strong>Connection Pooling</strong> -- Communication channel
 *       pooling is built-in and enabled by implementing a few simple
 *       methods.
 *   <li><strong>Connection Sharing</strong> -- Communication channel
 *       sharing (multiple tasks sharing the same channel) is also
 *       built-in and easily enabled.
 *   <li><strong>Validation &amp; Keep-Alive</strong> -- All channels
 *       are validated before usage and kept-alive with regular
 *       validation requests when pooled for reuse.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class Connection extends StorableObject {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Connection.class.getName());

    /**
     * The dictionary key for the maximum number of open channels.
     */
    public static final String KEY_MAX_OPEN = "maxOpen";

    /**
     * The dictionary key for the maximum idle time (in seconds).
     */
    public static final String KEY_MAX_IDLE_SECS = "maxIdleSecs";

    /**
     * The connection object storage path.
     */
    public static final Path PATH = Path.from("/connection/");

    /**
     * The maximum time to wait for acquiring an object from the pool.
     */
    private static long MAX_WAIT_MILLIS = 500L;

    /**
     * The connection channel pool used for managing objects. The
     * pool will be used to create all objects, but only ones
     * supporting it will be returned (others immediately destroyed).
     */
    private GenericObjectPool<Channel> channelPool = null;

    /**
     * The timestamp (in milliseconds) of the last usage time. This
     * will be updated on each connection reservation or release. It
     * is used in the default mechanism for determining if the
     * connection is active.
     */
    protected long lastUsedTime = System.currentTimeMillis();

    /**
     * The error message for the last error. This will set or cleared
     * on each connection reservation.
     */
    protected String lastError = null;

    /**
     * Searches for a specific connection in the storage.
     *
     * @param storage        the storage to search in
     * @param id             the connection identifier
     *
     * @return the connection found, or
     *         null if not found
     */
    public static Connection find(Storage storage, String id) {
        return storage.load(Path.resolve(PATH, id), Connection.class);
    }

    /**
     * Creates a new connection from a serialized representation.
     *
     * @param id             the object identifier
     * @param type           the object type name
     * @param dict           the serialized representation
     *
     * @see #init()
     */
    protected Connection(String id, String type, Dict dict) {
        super(id, type, dict);
    }

    /**
     * Checks if this object is in active use. This method will only
     * return false if the object haven't been used for 60 seconds
     * and no channels remain open.
     *
     * @return true if the object is considered active, or
     *         false otherwise
     */
    protected boolean isActive() {
        return System.currentTimeMillis() - lastUsedTime <= 60000L ||
               openChannels() > 0;
    }

    /**
     * Initializes this connection after loading it from a storage.
     * Any object initialization that may fail or that causes the
     * object to interact with any other part of the system (or
     * external systems) should be implemented here.
     *
     * @throws StorageException if the initialization failed
     */
    protected void init() throws StorageException {
        int open = maxOpen();
        int idle = maxIdleSeconds();

        dict.setInt(PREFIX_COMPUTED + KEY_MAX_OPEN, open);
        dict.setInt(PREFIX_COMPUTED + KEY_MAX_IDLE_SECS, idle);
        channelPool = new GenericObjectPool<>(new ChannelFactory());
        channelPool.setMaxTotal(open);
        channelPool.setMaxIdle(open);
        channelPool.setMinIdle(0);
        channelPool.setMaxWait(Duration.ofMillis(MAX_WAIT_MILLIS));
        channelPool.setMinEvictableIdle(Duration.ofSeconds(idle));
        channelPool.setLifo(false);
        channelPool.setTestOnBorrow(true);
        channelPool.setTestOnReturn(true);
        channelPool.setTestWhileIdle(true);
        channelPool.setBlockWhenExhausted(true);
    }

    /**
     * Destroys this connection. This method is used to free any
     * resources used when this object is no longer used. This method
     * is called when an object is removed from the in-memory storage
     * (object cache).
     */
    protected void destroy() {
        try {
            LOG.fine("closing all connections in " + this);
            channelPool.close();
            LOG.fine("done closing all pooled connections in " + this);
        } catch (Exception e) {
            LOG.warning("failed to close all connections in " +
                        this + ": " + e.getMessage());
        }
    }

    /**
     * Attempts to deactivate this object. This method will evict old
     * connection channels from the pool and should only be called
     * from a background job.
     */
    protected void passivate() {
        try {
            LOG.fine("starting eviction on " + this);
            channelPool.evict();
            LOG.fine("done eviction on " + this);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to evict in " + this, e);
        }
    }

    /**
     * Returns the maximum number of open channels. If the config
     * parameter hasn't been set, a default value of four (4) will be
     * returned.
     *
     * @return the maximum number of open channels
     */
    public int maxOpen() {
        try {
            return dict.getInt(KEY_MAX_OPEN, 4);
        } catch (NumberFormatException e) {
            String msg = this + ": invalid value for config parameter " +
                         KEY_MAX_OPEN + ": " + dict.get(KEY_MAX_OPEN);
            LOG.warning(msg);
            return 4;
        }
    }

    /**
     * Returns the maximum number of seconds a channel is allowed to
     * be idle (in a pool). If the configuration parameter hasn't
     * been set, a default value of 600 seconds (10 minutes) will be
     * returned.
     *
     * @return the maximum number of seconds to idle a channel
     */
    public int maxIdleSeconds() {
        try {
            return dict.getInt(KEY_MAX_IDLE_SECS, 600);
        } catch (NumberFormatException e) {
            String msg = this + ": invalid value for config parameter " +
                         KEY_MAX_IDLE_SECS + ": " + dict.get(KEY_MAX_IDLE_SECS);
            LOG.warning(msg);
            return 600;
        }
    }

    /**
     * Returns the total number of open channels. This is the number
     * of reserved channels plus any idle channels in the pool (if
     * any).
     *
     * @return the total number of open channels, or
     *         zero (0) if no channels are currently open
     */
    public int openChannels() {
        return channelPool.getNumActive() + channelPool.getNumIdle();
    }

    /**
     * Returns the number of channels in use (reserved).
     *
     * @return the number of channels in use
     */
    public int usedChannels() {
        return channelPool.getNumActive();
    }

    /**
     * Returns the timestamp of the last connection usage. This will
     * be updated on each connection reservation or release. It is
     * used in the default mechanism for determining if the
     * connection is active.
     *
     * @return the timestamp of the last connection usage
     */
    public Date lastUsed() {
        return new Date(lastUsedTime);
    }

    /**
     * Returns the error message for the last error. This will set or
     * cleared on each connection reservation.
     *
     * @return the error message for the last error
     */
    public String lastError() {
        return lastError;
    }

    /**
     * Reserves a communication channel for this connection. If the
     * channels supports being pooled, a previously created channel
     * may be returned from this method.
     *
     * @return the reserved connection channel
     *
     * @throws ConnectionException if no communication channel could
     *             be created and validated
     */
    public Channel reserve() throws ConnectionException {
        Channel  channel = null;
        String   msg = null;

        lastUsedTime = System.currentTimeMillis();
        lastError = null;
        try {
            // TODO: handle shared channels
            msg = "reserving connection channel in " + this;
            LOG.fine(msg);
            channel = (Channel) channelPool.borrowObject();
            LOG.fine("done " + msg);
        } catch (ConnectionException e) {
            lastError = e.getMessage();
            LOG.log(Level.WARNING, "failed " + msg, e);
            cleanupChannel(channel);
            throw e;
        } catch (Exception e) {
            lastError = e.getMessage();
            LOG.log(Level.WARNING, "failed " + msg, e);
            cleanupChannel(channel);
            throw new ConnectionException(e.getMessage());
        }
        return channel;
    }

    /**
     * Releases a previously reserved communication channel for this
     * connection. If the channel supports being pooled, it will be
     * added to the pool of channels, otherwise it will be destroyed
     * immediately.
     *
     * @param channel        the channel to release
     */
    public void release(Channel channel) {
        String  msg = null;

        lastUsedTime = System.currentTimeMillis();
        try {
            // TODO: handle shared channels
            msg = "returning pooled connection in " + this;
            LOG.fine(msg);
            if (channel.isValid() && channel.isPoolable()) {
                channelPool.returnObject(channel);
            } else {
                channelPool.invalidateObject(channel);
            }
            LOG.fine("done " + msg);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed " + msg, e);
            cleanupChannel(channel);
        }
    }

    /**
     * Destroys and removes references to a channel. No errors are
     * logged, so this method should only be used after other errors
     * have already been reported.
     *
     * @param channel        the channel to destroy
     */
    protected void cleanupChannel(Channel channel) {
        if (channel != null) {
            try {
                channelPool.invalidateObject(channel);
            } catch (Exception ignore) {
                // Attempted pool removal failed
            }
            try {
                destroyChannel(channel);
            } catch (Exception ignore) {
                // Again, we only tried to cleanup
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
    protected abstract Channel createChannel() throws ConnectionException;

    /**
     * Destroys a connection channel, freeing any resources used
     * (such as database connections, networking sockets, etc).
     *
     * @param channel        the channel to destroy
     */
    protected abstract void destroyChannel(Channel channel);

    /**
     * Returns a serialized representation of this object. Used when
     * persisting to permanent storage or when accessing the object
     * from outside pure Java. Returns a shallow copy of the contained
     * dictionary.
     *
     * @return the serialized representation of this object
     */
    public Dict serialize() {
        Dict copy = super.serialize();
        copy.setInt(PREFIX_COMPUTED + "openChannels", openChannels());
        copy.setInt(PREFIX_COMPUTED + "usedChannels", usedChannels());
        copy.set(PREFIX_COMPUTED + "lastUsedTime", lastUsed());
        copy.set(PREFIX_COMPUTED + "lastError", lastError());
        return copy;
    }


    /**
     * An connection channel factory. This is a simple implementation
     * of the PoolableObjectFactory API for handling channels in the
     * generic object pool.
     *
     * @author   Per Cederberg
     * @version  1.0
     */
    private class ChannelFactory implements PooledObjectFactory<Channel> {

        /**
         * Creates a new connection channel factory.
         */
        ChannelFactory() {
            // Nothing to do here
        }

        /**
         * Creates a new pooled channel.
         *
         * @return a new pooled channel
         *
         * @throws Exception if the channel couldn't be created
         */
        public PooledObject<Channel> makeObject() throws Exception {
            return new DefaultPooledObject<>(createChannel());
        }

        /**
         * Destroys a channel.
         *
         * @param obj            the pooled channel to destroy
         */
        public void destroyObject(PooledObject<Channel> obj) {
            destroyChannel(obj.getObject());
        }

        /**
         * Validates a channel.
         *
         * @param obj            the pooled channel to validate
         *
         * @return true if the channel was valid, or
         *         false otherwise
         */
        public boolean validateObject(PooledObject<Channel> obj) {
            Channel channel = obj.getObject();
            channel.validate();
            return channel.isValid();
        }

        /**
         * Activates a channel.
         *
         * @param obj            the pooled channel to activate
         *
         * @throws Exception if the channel couldn't be activated
         */
        public void activateObject(PooledObject<Channel> obj) throws Exception {
            obj.getObject().reserve();
        }

        /**
         * Passivates a channel.
         *
         * @param obj            the pooled channel to passivate
         *
         * @throws Exception if the channel couldn't be passivated
         */
        public void passivateObject(PooledObject<Channel> obj) throws Exception {
            obj.getObject().release();
        }
    }
}
