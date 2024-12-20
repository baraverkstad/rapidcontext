/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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

import java.util.LinkedHashSet;

import org.apache.commons.lang3.StringUtils;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.TextEncoding;

/**
 * A procedure bindings container. The procedure bindings contain
 * virtually all data, information and parameters required by a
 * procedure, and are used during serialization, unserialization,
 * introspection and execution. Each binding has a name, a type and
 * a value. The value object class depends on the binding type, but
 * is frequently a string. The bindings can be linked together into a
 * hierarchy, which is used when redefining binding values (such as
 * when binding call argument values). The bindings can also be
 * sealed, thereby protecting the particular bindings instance from
 * further modifications (it can still be inherited, though).
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class Bindings {

    /**
     * The static string data binding type. Any data value of this
     * type must be a string.
     */
    public static final int DATA = 1;

    /**
     * The procedure name binding type. Any data value of this type
     * must be a string containing the procedure name.
     */
    public static final int PROCEDURE = 2;

    /**
     * The connection pool name binding type. Any data value of this
     * type must be a string containing the connection pool name.
     */
    public static final int CONNECTION = 3;

    /**
     * The argument binding type. The data values of this type can be
     * any object.
     */
    public static final int ARGUMENT = 4;

    /**
     * The parent bindings, or null for none.
     */
    private Bindings parent = null;

    /**
     * The bindings array.
     */
    private Array data;

    /**
     * Returns the type name corresponding to a binding type constant.
     *
     * @param type           the binding type
     *
     * @return the corresponding type name
     *
     * @throws ProcedureException if the binding type is invalid
     */
    private static String toTypeName(int type) throws ProcedureException {
        return switch (type) {
            case DATA -> "data";
            case PROCEDURE -> "procedure";
            case CONNECTION -> "connection";
            case ARGUMENT -> "argument";
            default -> throw new ProcedureException("invalid binding type number: " + type);
        };
    }

    /**
     * Creates a new empty bindings container.
     */
    public Bindings() {
        this(null, null);
    }

    /**
     * Creates a new empty child bindings container.
     *
     * @param parent         the parent bindings container
     */
    public Bindings(Bindings parent) {
        this(parent, null);
    }

    /**
     * Creates a new bindings container with the specified data.
     *
     * @param parent         the parent bindings container, or null
     * @param arr            the data array to use, or null
     */
    public Bindings(Bindings parent, Array arr) {
        this.parent = parent;
        this.data = (arr == null) ? new Array() : arr;
    }

    /**
     * Checks if the specified binding name exists.
     *
     * @param name           the binding name
     *
     * @return true if the name exists, or
     *         false otherwise
     */
    public boolean hasName(String name) {
        return findLocal(name) >= 0 ||
               (parent != null && parent.hasName(name));
    }

    /**
     * Returns an array with all names defined in the hierarchy of
     * bindings.
     *
     * @return an array with all binding names
     */
    public String[] getNames() {
        LinkedHashSet<String> set = getNames(new LinkedHashSet<>());
        String[] res = new String[set.size()];
        return set.toArray(res);
    }

    /**
     * Sets all binding names in the hierarchy to a set. This method
     * will recursively call the parent bindings to find all names.
     *
     * @param set            the name set to modify
     *
     * @return the input name set
     */
    private LinkedHashSet<String> getNames(LinkedHashSet<String> set) {
        if (parent != null) {
            parent.getNames(set);
        }
        for (Object o : data) {
            Dict bind = (Dict) o;
            set.add(bind.get("name", String.class));
        }
        return set;
    }

    /**
     * Finds the type for a binding.
     *
     * @param name           the binding name
     *
     * @return the binding data type
     *
     * @throws ProcedureException if the binding name wasn't found
     */
    public int getType(String name) throws ProcedureException {
        String type = find(name).get("type", String.class);
        if (type == null) {
            throw new ProcedureException("no binding type for '" + name + "' found");
        }
        if (type.equals(toTypeName(DATA))) {
            return DATA;
        } else if (type.equals(toTypeName(PROCEDURE))) {
            return PROCEDURE;
        } else if (type.equals(toTypeName(CONNECTION))) {
            return CONNECTION;
        } else if (type.equals(toTypeName(ARGUMENT))) {
            return ARGUMENT;
        } else {
            throw new ProcedureException("invalid binding type for '" + name + "': " + type);
        }
    }

    /**
     * Finds the type name identifier for a binding. Use the
     * getType() method for most access. Only use this string value
     * for debug printing or serialization.
     *
     * @param name           the binding name
     *
     * @return the binding data type name
     *
     * @throws ProcedureException if the binding name wasn't found
     */
    public String getTypeName(String name) throws ProcedureException {
        return toTypeName(getType(name));
    }

    /**
     * Finds the value for a binding.
     *
     * @param name           the binding name
     *
     * @return the value binding
     *
     * @throws ProcedureException if the binding name wasn't found or
     *             if the value was null
     */
    public Object getValue(String name) throws ProcedureException {
        Object value = find(name).get("value");
        if (value == null) {
            throw new ProcedureException("no binding value for '" + name + "' found");
        }
        return value;
    }

    /**
     * Finds the value for a binding.
     *
     * @param name           the binding name
     * @param defaultValue   the default value
     *
     * @return the value binding
     *
     * @throws ProcedureException if the binding name wasn't found
     */
    public Object getValue(String name, Object defaultValue)
        throws ProcedureException {

        Object value = find(name).get("value");
        return (value == null) ? defaultValue : value;
    }

    /**
     * Returns the human-readable description for a binding.
     *
     * @param name           the binding name
     *
     * @return the binding description, or
     *         an empty string if not set
     *
     * @throws ProcedureException if the binding name wasn't found
     */
    public String getDescription(String name) throws ProcedureException {
        String desc = find(name).get("description", String.class, "");
        if (desc.isBlank() && parent != null && parent.hasName(name)) {
            return parent.getDescription(name);
        } else if (desc.isBlank() && getType(name) == ARGUMENT) {
            // TODO: remove this hack once all serialized add-on procedures
            //       have been corrected
            return (String) getValue(name, "");
        } else {
            return desc;
        }
    }

    /**
     * Sets the value for a binding. If the binding already exists
     * locally it will be modified, otherwise it will be added. If
     * the description is set to null, any parent binding description
     * will be used instead.
     *
     * @param name           the binding name
     * @param type           the binding type
     * @param value          the binding value, or null
     * @param description    the binding description, or null
     *
     * @throws ProcedureException if this object was sealed
     */
    public void set(String name, int type, Object value, String description)
        throws ProcedureException {

        try {
            int index = findLocal(name);
            Dict bind = (index < 0) ? new Dict() : data.getDict(index);
            bind.set("name", name);
            bind.set("type", toTypeName(type));
            bind.set("value", value);
            bind.set("description", description);
            if (index < 0) {
                data.add(bind);
            }
        } catch (UnsupportedOperationException e) {
            throw new ProcedureException("cannot modify binding in sealed object");
        }
    }

    /**
     * Seals these bindings and prevents future modification. This
     * method is used by built-in procedures once they have created
     * their default (read-only) binding objects.
     */
    public void seal() {
        data.seal(true);
    }

    /**
     * Searches for a binding with the specified name. The search
     * will recurse through the parent binding object if no binding
     * was found.
     *
     * @param name           the binding name
     *
     * @return the binding data object
     *
     * @throws ProcedureException if the binding name wasn't found
     *             in the hierarchy
     */
    private Dict find(String name) throws ProcedureException {
        int index = findLocal(name);
        if (index >= 0) {
            return data.getDict(index);
        } else if (parent != null) {
            return parent.find(name);
        } else {
            throw new ProcedureException("no binding for '" + name + "' found");
        }
    }

    /**
     * Searches for a local binding with the specified name.
     *
     * @param name           the binding name
     *
     * @return the local data array index, or
     *         -1 if not found
     */
    private int findLocal(String name) {
        for (int i = 0; i < data.size(); i++) {
            Dict bind = data.getDict(i);
            if (name.equals(bind.get("name", String.class))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Replaces template variables with the corresponding argument
     * value from the bindings. This method supports both raw (e.g.
     * "@key") and encoded (e.g. ":key") template variables.
     *
     * @param tpl            the template string to process
     * @param encoding       the text encoding for values
     *
     * @return the processed template string
     */
    public String processTemplate(String tpl, TextEncoding encoding) {
        for (Object o : data) {
            Dict bind = (Dict) o;
            String key = bind.get("name", String.class);
            String type = bind.get("type", String.class, "");
            if (type.equals("4") || type.equals("argument")) {
                String val = bind.get("value", String.class, "");
                if (StringUtils.contains(tpl, "@" + key)) {
                    tpl = StringUtils.replace(tpl, "@" + key, val);
                }
                if (StringUtils.contains(tpl, ":" + key)) {
                    if (encoding == TextEncoding.JSON) {
                        val = TextEncoding.encodeJsonString(val);
                    } else {
                        val = TextEncoding.encode(encoding, val);
                    }
                    tpl = StringUtils.replace(tpl, ":" + key, val);
                }
            }
        }
        return tpl;
    }
}
