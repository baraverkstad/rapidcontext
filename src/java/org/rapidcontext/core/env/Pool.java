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

package org.rapidcontext.core.env;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.rapidcontext.core.data.Data;

/**
 * An adapter connection pool. The connection pool contains all the
 * configuration parameters for creating new connections, as well as
 * an optional pool of previously used connections.
 *
 * @author   Per Cederberg, Dynabyte AB
 * @version  1.0
 */
public class Pool {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(Pool.class.getName());

    /**
     * The maximum number of connections configuration parameter name. 
     */
    public static final String PARAM_MAX_CONNECTIONS = "maxConnections";

    /**
     * The maximum idle time (in seconds) configuration parameter name.
     */
    public static final String PARAM_MAX_IDLE_SECS = "maxIdleSecs";

    /**
     * The maximum time to wait for acquiring an object from the pool.
     */
    private static int MAX_ACQUIRE_WAIT = 500;

    /**
     * The list of adapter connection pool instances. Only the
     * instances using a connection pool will be added to this list.
     */
    static ArrayList poolInstances = new ArrayList();

    /**
     * The unique pool name.
     */
    private String name;

    /**
     * The adapter to use for creating connections.
     */
    private Adapter adapter;

    /**
     * The configuration parameters to user for creating connections.
     */
    private Data params;

    /**
     * The adapter connection pool.
     */
    private GenericObjectPool pool = null;

    /**
     * Creates a new adapter connection pool.
     *
     * @param name           the unique pool name
     * @param adapter        the adapter instance to use
     * @param params         the configuration parameters
     *
     * @throws AdapterException if the pool configuration was invalid
     */
    public Pool(String name, Adapter adapter, Data params)
        throws AdapterException {

        String  str;
        int     maxConnections;
        int     maxIdleSecs;

        this.name = name;
        this.adapter = adapter;
        this.params = params;
        try {
            maxConnections = params.getInt(PARAM_MAX_CONNECTIONS, 3);
        } catch (NumberFormatException e) {
            str = "invalid numeric value for config parameter " +
                  PARAM_MAX_CONNECTIONS + ": " +
                  params.getString(PARAM_MAX_CONNECTIONS, "");
            LOG.warning(str);
            throw new AdapterException(str);
        }
        try {
            maxIdleSecs = params.getInt(PARAM_MAX_IDLE_SECS, 600);
        } catch (NumberFormatException e) {
            str = "invalid numeric value for config parameter " +
                  PARAM_MAX_IDLE_SECS + ": " +
                  params.getString(PARAM_MAX_IDLE_SECS, "");
            LOG.warning(str);
            throw new AdapterException(str);
        }
        if (maxConnections > 0) {
            poolInstances.add(this);
            pool = new GenericObjectPool(new ConnectionFactory());
            pool.setMaxActive(maxConnections);
            pool.setMaxIdle(maxConnections);
            pool.setMinIdle(0);
            pool.setMaxWait(MAX_ACQUIRE_WAIT);
            pool.setMinEvictableIdleTimeMillis(maxIdleSecs * 1000L);
            pool.setTestOnBorrow(false);
            pool.setTestOnReturn(false);
            pool.setTestWhileIdle(false);
            pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
            Evictor.startThread();
        }
    }

    /**
     * Returns the unique pool name.
     *
     * @return the unique pool name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique pool name.
     *
     * @param name           the unique pool name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the adapter instance used by this pool.
     *
     * @return the adapter instance used by this pool
     */
    public Adapter getAdapter() {
        return adapter;
    }

    /**
     * Returns the configuration parameter values for this pool.
     *
     * @return the configuration parameter values for this pool
     */
    public Data getParameters() {
        return params;
    }

    /**
     * Reserves a connection from this pool. This will either create
     * a new connection or reserve an existing connection from the
     * pool. Any method reserving a connection from this method MUST
     * make sure to return it with the releaseConnection() method.
     *
     * @return the adapter connection reserved
     *
     * @throws AdapterException if an adapter connection couldn't be
     *             created or reserved
     *
     * @see #releaseConnection(AdapterConnection)
     */
    public AdapterConnection reserveConnection() throws AdapterException {
        if (pool == null) {
            return adapter.createConnection(params);
        } else {
            try {
                return (AdapterConnection) pool.borrowObject();
            } catch (AdapterException e) {
                throw e;
            } catch (Exception e) {
                throw new AdapterException(e.getMessage());
            }
        }
    }

    /**
     * Releases a connection previously acquired via this pool. This method
     * will return the connection to the pool of available connections.
     *
     * @param con            the connection to release
     */
    public void releaseConnection(AdapterConnection con) {
        String  msg;

        if (pool == null) {
            try {
                con.close();
            } catch (AdapterException e) {
                msg = "failed to close non-pooled connection in " + name +
                      ": " + e.getMessage();
                LOG.warning(msg);
            }
        } else {
            try {
                pool.returnObject(con);
            } catch (Exception e) {
                msg = "failed to return pooled connection in " + name +
                      ": " + e.getMessage();
                LOG.warning(msg);
                try {
                    pool.invalidateObject(con);
                } catch (Exception ignore) {
                    // Do nothing here
                }
                try {
                    con.close();
                } catch (Exception ignore) {
                    // Do nothing here
                }
            }
        }
    }

    /**
     * Evicts any old connections from the pool. This method is
     * normally called automatically by the evictor background
     * thread.
     */
    public void evict() {
        try {
            pool.evict();
        } catch (Exception e) {
            LOG.warning("failed to evict pooled connection in " +
                        name + ": " + e.getMessage());
        }
    }

    /**
     * Closes this connection pool. Any existing connections in the
     * pool will be disposed.
     */
    public void close() {
        if (pool != null) {
            try {
                pool.close();
            } catch (Exception e) {
                LOG.warning("failed to close all pooled connections in " +
                            name + ": " + e.getMessage());
            }
            poolInstances.remove(this);
        }
    }


    /**
     * An adapter connection factory. This is a simple implementation
     * of the PoolableObjectFactory API for handling adapter
     * connections in the generic object pool.
     *
     * @author   Per Cederberg
     * @version  1.0
     */
    private class ConnectionFactory implements PoolableObjectFactory {

        /**
         * Creates a new adapter connection factory.
         */
        ConnectionFactory() {
            // Nothing to do here
        }

        /**
         * Creates a new adapter connection.
         *
         * @return a new adapter connection
         *
         * @throws Exception if the adapter connection couldn't be created
         */
        public Object makeObject() throws Exception {
            return getAdapter().createConnection(getParameters());
        }

        /**
         * Destroys an adapter connection.
         *
         * @param obj            the adapter connection
         *
         * @throws Exception if the adapter connection couldn't be destroyed
         */
        public void destroyObject(Object obj) throws Exception {
            ((AdapterConnection) obj).close();
        }

        /**
         * Validates an adapter connection.
         *
         * @param obj            the adapter connection
         *
         * @return true if the connection was validated, or
         *         false otherwise
         */
        public boolean validateObject(Object obj) {
            try {
                ((AdapterConnection) obj).activate();
                ((AdapterConnection) obj).passivate();
                return true;
            } catch (AdapterException e) {
                return false;
            }
        }

        /**
         * Activates an adapter connection.
         *
         * @param obj            the adapter connection
         *
         * @throws Exception if the adapter connection couldn't be activated
         */
        public void activateObject(Object obj) throws Exception {
            ((AdapterConnection) obj).activate();
        }

        /**
         * Passivates an adapter connection.
         *
         * @param obj            the adapter connection
         *
         * @throws Exception if the adapter connection couldn't be passivated
         */
        public void passivateObject(Object obj) throws Exception {
            ((AdapterConnection) obj).passivate();
        }
    }


    /**
     * An adapter connection pool evictor. This will evict old
     * connections from all the pools. 
     *
     * @author   Per Cederberg
     * @version  1.0
     */
    private static class Evictor implements Runnable {

        /**
         * The number of seconds the evictor thread sleeps between runs.
         */
        private static int EVICTOR_SLEEP_SECONDS = 20;

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
         * Runs the evictor thread until no adapter connection pools remain.
         */
        public void run() {
            while (true) {
                synchronized (lock) {
                    if (poolInstances.size() <= 0) {
                        instance = null;
                        break;
                    }
                }
                evict();
                try {
                    Thread.sleep(EVICTOR_SLEEP_SECONDS * 1000L);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
        }

        /**
         * Evicts old connections from all the pools.
         */
        private void evict() {
            Iterator  iter = poolInstances.iterator();

            try {
                while (iter.hasNext()) {
                    ((Pool) iter.next()).evict();
                }
            } catch (ConcurrentModificationException ignore) {
                // Skip reporting this, retry will be attempted later
            }
        }
    }
}
