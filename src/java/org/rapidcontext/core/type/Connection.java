/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2010 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.type;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
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
    public static final Path PATH = new Path("/connection/");

    /**
     * The maximum time to wait for acquiring an object from the pool.
     */
    private static int MAX_ACQUIRE_WAIT = 500;

    /**
     * The list of connection pool instances. Only the connections
     * using a connection pool will be added to this list.
     */
    protected static ArrayList connectionPools = new ArrayList();

    /**
     * The connection channel pool used for managing objects. The
     * pool will be used to create all objects, but only ones
     * supporting it will be returned (others immediately destroyed).
     */
    private GenericObjectPool channelPool = null;

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
        Object  obj = storage.load(PATH.descendant(new Path(id)));

        return (obj instanceof Connection) ? (Connection) obj : null;
    }

    /**
     * Searches for all connections in the storage.
     *
     * @param storage        the storage to search in
     *
     * @return an array of all connections found
     */
    public static Connection[] findAll(Storage storage) {
        Object[]   objs = storage.loadAll(PATH);
        ArrayList  list = new ArrayList(objs.length);

        for (int i = 0; i < objs.length; i++) {
            if (objs[i] instanceof Connection) {
                list.add(objs[i]);
            }
        }
        return (Connection[]) list.toArray(new Connection[list.size()]);
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

        dict.setInt("_" + KEY_MAX_OPEN, open);
        dict.setInt("_" + KEY_MAX_IDLE_SECS, idle);
        channelPool = new GenericObjectPool(new ChannelFactory());
        channelPool.setMaxActive(open);
        channelPool.setMaxIdle(open);
        channelPool.setMinIdle(0);
        channelPool.setMaxWait(MAX_ACQUIRE_WAIT);
        channelPool.setMinEvictableIdleTimeMillis(idle * 1000L);
        channelPool.setLifo(false);
        channelPool.setTestOnBorrow(true);
        channelPool.setTestOnReturn(true);
        channelPool.setTestWhileIdle(true);
        channelPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
        synchronized (connectionPools) {
            connectionPools.add(this);
        }
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
        synchronized (connectionPools) {
            connectionPools.remove(this);
        }
    }

    /**
     * Returns a serialized representation of this object. Used when
     * accessing the object from outside pure Java.
     *
     * @return the serialized representation of this object
     */
    public Dict serialize() {
        dict.setInt("_openChannels", openChannels());
        dict.setInt("_usedChannels", usedChannels());
        return dict;
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

        try {
            // TODO: handle shared channels
            msg = "reserving connection channel in " + this;
            LOG.fine(msg);
            channel = (Channel) channelPool.borrowObject();
            LOG.fine("done " + msg);
        } catch (ConnectionException e) {
            LOG.log(Level.WARNING, "failed " + msg, e);
            cleanupChannel(channel);
            throw e;
        } catch (Exception e) {
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

        try {
            // TODO: handle shared channels
            msg = "returning pooled connection in " + this;
            LOG.fine(msg);
            if (channel.isValid() && channel.isPoolable()) {
                channelPool.returnObject(channel);
                Evictor.startThread();
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
     * Evicts any old connections from the pool. This method is
     * normally called automatically by the evictor background
     * thread.
     */
    protected void evict() {
        try {
            LOG.fine("starting eviction on " + this);
            channelPool.evict();
            LOG.fine("done eviction on " + this);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to evict in " + this, e);
        }
    }


    /**
     * An connection channel factory. This is a simple implementation
     * of the PoolableObjectFactory API for handling channels in the
     * generic object pool.
     *
     * @author   Per Cederberg
     * @version  1.0
     */
    private class ChannelFactory implements PoolableObjectFactory {

        /**
         * Creates a new connection channel factory.
         */
        ChannelFactory() {
            // Nothing to do here
        }

        /**
         * Creates a new channel.
         *
         * @return a new channel
         *
         * @throws Exception if the channel couldn't be created
         */
        public Object makeObject() throws Exception {
            return createChannel();
        }

        /**
         * Destroys a channel.
         *
         * @param obj            the channel to destroy
         */
        public void destroyObject(Object obj) {
            destroyChannel((Channel) obj);
        }

        /**
         * Validates a channel.
         *
         * @param obj            the channel to validate
         *
         * @return true if the channel was valid, or
         *         false otherwise
         */
        public boolean validateObject(Object obj) {
            Channel  channel = (Channel) obj;

            channel.validate();
            return channel.isValid();
        }

        /**
         * Activates a channel.
         *
         * @param obj            the channel to activate
         *
         * @throws Exception if the channel couldn't be activated
         */
        public void activateObject(Object obj) throws Exception {
            ((Channel) obj).reserve();
        }

        /**
         * Passivates a channel.
         *
         * @param obj            the channel to passivate
         *
         * @throws Exception if the channel couldn't be passivated
         */
        public void passivateObject(Object obj) throws Exception {
            ((Channel) obj).release();
        }
    }


    /**
     * A pool evictor. This is run in a separate thread to evict
     * expired channels from all connections. This will also serve
     * to keep channels live with "ping" calls.
     *
     * @author   Per Cederberg
     * @version  1.0
     */
    private static class Evictor implements Runnable {

        /**
         * The class logger.
         */
        private static final Logger LOGGER =
            Logger.getLogger(Evictor.class.getName());

        /**
         * The number of seconds the evictor thread sleeps between runs.
         */
        private static int EVICTOR_SLEEP_SECONDS = 30;

        /**
         * The synchronization lock to use.
         */
        private static Object lock = new Integer(1);

        /**
         * The singleton evictor instance. Used for synchronization
         * so that only a single thread is started.
         */
        private static Evictor instance = null;

        /**
         * Starts a new background evictor thread if one isn't
         * already running.
         */
        public static void startThread() {
            Thread  thread;

            synchronized (lock) {
                if (instance == null) {
                    instance = new Evictor();
                    thread = new Thread(instance, "Connection Pool Evictor");
                    thread.setDaemon(true);
                    thread.start();
                }
            }
        }

        /**
         * Creates a new evictor.
         */
        private Evictor() {
            // Nothing to do here
        }

        /**
         * Runs the evictor thread until no active channels remain.
         */
        public void run() {
            LOGGER.fine("started connection pool evictor thread");
            while (true) {
                try {
                    Thread.sleep(EVICTOR_SLEEP_SECONDS * 1000L);
                } catch (InterruptedException e) {
                    // Do nothing
                }
                int active = evict();
                synchronized (lock) {
                    if (active <= 0) {
                        instance = null;
                        break;
                    }
                }
            }
            LOGGER.fine("terminated connection pool evictor thread");
        }

        /**
         * Evicts old connections from all the pools.
         *
         * @return the approximate number of active connections
         */
        private int evict() {
            Iterator    iter = connectionPools.iterator();
            Connection  con;
            int         active = 0;

            try {
                while (iter.hasNext()) {
                    con = (Connection) iter.next();
                    con.evict();
                    active += con.openChannels();
                }
                return active;
            } catch (ConcurrentModificationException ignore) {
                // Skip reporting this, retry will be attempted later
                return 1;
            }
        }
    }
}
