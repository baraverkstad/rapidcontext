/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.util.DateUtil;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;

/**
 * A data serializer and unserializer for the YAML format. The object
 * mapping to YAML is not exact, and may omit serialization of data
 * in some cases. The following basic requirements must be met in
 * order to serialize an object:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean, Array, Dict and StorableObject
 *       values are supported.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class YamlSerializer {

    /**
     * The YAML serialization settings to use.
     */
    private static DumpSettings settings = DumpSettings.builder()
        .setDefaultFlowStyle(FlowStyle.BLOCK)
        .setIndent(4)
        .setIndicatorIndent(2)
        .build();

    /**
     * Serializes an object into a YAML representation.
     *
     * @param obj            the object to convert, or null
     * @param os             the output stream to write to
     *
     * @throws IOException if the data couldn't be serialized
     */
    public static void serialize(Object obj, OutputStream os) throws IOException {
        try (OutputStreamWriter ow = new OutputStreamWriter(os, "UTF-8")) {
            ow.write(serialize(obj));
            ow.flush();
        }
    }

    /**
     * Serializes an object into a YAML representation.
     *
     * @param obj            the object to serialize, or null
     *
     * @return a YAML file representation
     */
    public static String serialize(Object obj) {
        return new Dump(settings).dumpToString(toYaml(obj));
    }

    /**
     * Converts a value to a YAML-serializable representation.
     *
     * @param obj            the value to convert
     *
     * @return the YAML-serializable representation
     */
    private static Object toYaml(Object obj) {
        if (obj instanceof Dict) {
            return toYaml((Dict) obj);
        } else if (obj instanceof Array) {
            return toYaml((Array) obj);
        } else if (obj instanceof StorableObject) {
            return toYaml(((StorableObject) obj).serialize());
        } else if (obj instanceof Date) {
            return DateUtil.asEpochMillis((Date) obj);
        } else if (obj == null || obj instanceof Boolean || obj instanceof Number) {
            return obj;
        } else {
            return obj.toString();
        }
    }

    /**
     * Converts a dictionary to a YAML-serializable representation.
     *
     * @param dict           the dictionary to convert
     *
     * @return the YAML-serializable representation
     */
    private static Object toYaml(Dict dict) {
        LinkedHashMap<String,Object> map = new LinkedHashMap<>();
        LinkedList<String> delayed = new LinkedList<>();
        for (String k : dict.keys()) {
            Object v = dict.get(k);
            if (v instanceof Dict || v instanceof Array || v instanceof StorableObject) {
                delayed.add(k);
            } else {
                map.put(k, toYaml(dict.get(k)));
            }
        }
        for (String k : delayed) {
            Object v = dict.get(k);
            boolean isEmpty = (
                (v instanceof Dict && ((Dict) v).size() == 0) ||
                (v instanceof Array && ((Array) v).size() == 0)
            );
            if (!isEmpty) {
                map.put(k, toYaml(v));
            }
        }
        return map;
    }

    /**
     * Converts an array to a YAML-serializable representation.
     *
     * @param arr            the array to convert
     *
     * @return the YAML-serializable representation
     */
    private static Object toYaml(Array arr) {
        LinkedList<Object> list = new LinkedList<>();
        for (Object item : arr) {
            if (item != null) {
                list.add(toYaml(item));
            }
        }
        return list;
    }

    // No instances
    private YamlSerializer() {}
}
