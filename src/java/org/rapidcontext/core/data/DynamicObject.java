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

package org.rapidcontext.core.data;

/**
 * A dynamic object base class. This class provides a basic dynamic
 * data container (a dictionary) that can be used for simple
 * serialization of the object. It also enables containers and others
 * to store additional data in this object, breaking the concept of
 * encapsulation (for improved utility).
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public abstract class DynamicObject {

    /**
     * The dictionary key for the object type. The value stored is a
     * string and should be descriptive outside of the Java world.
     */
    public static final String KEY_TYPE = "type";

    /**
     * The dictionary containing the serializable data for this
     * object.
     */
    public Dict dict = null;

    /**
     * Creates a new dynamic object. This constructor is used to
     * enforce that subclasses set the type key properly.
     *
     * @param type           the type name
     */
    protected DynamicObject(String type) {
        this.dict = new Dict();
        this.dict.add(KEY_TYPE, type);
    }

    /**
     * Creates a new dynamic object from a serialized representation.
     * The key-value pairs from the specified dictionary will be
     * copied (shallow copy) into this object dictionary. Only
     * subclasses wishing to provide unserialization support should
     * call this constructor.
     *
     * @param type           the type name
     * @param dict           the serialized representation
     */
    protected DynamicObject(String type, Dict dict) {
        this.dict = new Dict(dict.size() + 1);
        this.dict.add(KEY_TYPE, type);
        this.dict.setAll(dict);
    }

    /**
     * Returns the object type name.
     *
     * @return the object type name
     */
    public String type() {
        return dict.getString(KEY_TYPE, "");
    }

    /**
     * Returns a serialized representation of this object. Used when
     * accessing the object from outside pure Java. By default this
     * method will return the contained dictionary.
     *
     * @return the serialized representation of this object
     */
    public Dict serialize() {
        return dict;
    }
}
