/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2019 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.proc;

import java.util.Date;

import org.rapidcontext.core.data.Dict;

/**
 * An add-on procedure base class. All procedures that are not
 * built-in must extend this base class, since it serves as a marker
 * for standard security, modification and serialization support. It
 * also provides default implementations for some of the methods in
 * the procedure interface. Subclasses to this class should meet the
 * following requirements:
 *
 * <ul>
 * <li>The class must have a public constructor taking no arguments.
 * <li>The constructor should only throw ProcedureException:s.
 * <li>A unique procedure type name must be registered for the
 *     procedure class. See Library.registerProcedureType().
 * <li>All configurable aspects of the procedure must be stored as
 *     string values in the default procedure bindings. This is the
 *     only procedure data handled in the serialization process.
 * <li>The procedure should preferably not manage connections to
 *     other systems by itself when called. A better model is to use
 *     declared adapter connection pool names, which is handled
 *     transparently by the framework.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class AddOnProcedure implements Procedure {

    /**
     * The procedure data object.
     */
    private Dict data = new Dict();

    /**
     * The last modification time for the procedure.
     */
    private Date lastModified = null;

    /**
     * The default procedure bindings. These are normally modified
     * and thereafter sealed by the subclass constructors. The
     * procedure bindings returned will always inherit the default
     * bindings.
     */
    protected Bindings defaults = new Bindings();

    /**
     * The procedure bindings, based on the default bindings.
     */
    private Bindings bindings = new Bindings(defaults);

    /**
     * The default public constructor required for serialization
     * support.
     *
     * @throws ProcedureException if the initialization failed
     */
    public AddOnProcedure() throws ProcedureException {
        this.lastModified = new Date();
    }

    /**
     * Returns the procedure data object. This object contains all
     * the required procedure configuration data and is used when
     * serializing the procedure.
     *
     * @return the procedure data object
     */
    public Dict getData() {
        return data;
    }

    /**
     * Sets the procedure data object. This method will completely
     * reset this procedure to whatever the contents of the data
     * object is. It is called when procedures are unserialized or
     * changed due to user action.
     *
     * @param data           the procedure data object
     */
    public void setData(Dict data) {
        this.data = data;
        this.bindings = new Bindings(defaults, data.getArray("binding"));
        this.lastModified = new Date();
    }

    /**
     * Returns the procedure name.
     *
     * @return the procedure name
     */
    public String getName() {
        return data.getString("name", "");
    }

    /**
     * Returns the procedure description.
     *
     * @return the procedure description
     */
    public String getDescription() {
        return data.getString("description", "");
    }

    /**
     * Returns the procedure type name.
     *
     * @return procedure type name
     */
    public String getType() {
        return data.getString("type", "");
    }

    /**
     * Returns the timestamp for the last modification of this
     * procedure. Note that this will be reset to the current system
     * time whenever a change is made to the procedure, such as when
     * initially created or similar.
     *
     * @return the timestamp for the last modification
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Returns the bindings for this procedure. If this procedure
     * requires any special data, adapter connection or input
     * argument binding, those bindings should be set (but possibly
     * to null or blank values).
     *
     * @return the bindings for this procedure
     */
    public Bindings getBindings() {
        return bindings;
    }
}
