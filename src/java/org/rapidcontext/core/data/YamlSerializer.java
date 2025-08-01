/*
 * RapidContext <https://www.rapidcontext.com/>
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

package org.rapidcontext.core.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.rapidcontext.util.DateUtil;
import org.rapidcontext.util.ValueUtil;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.NonPrintableStyle;

/**
 * A data serializer and unserializer for the YAML format. The object
 * mapping to YAML is not exact, and may omit serialization of data
 * in some cases. The following basic requirements must be met in
 * order to serialize an object:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean, Date, Array and Dict values are
 *       supported.
 * </ul>
 *
 * @author Per Cederberg
 */
public class YamlSerializer {

    /**
     * The YAML serialization settings to use.
     */
    private static DumpSettings dumpSettings = DumpSettings.builder()
        .setDefaultFlowStyle(FlowStyle.BLOCK)
        .setIndent(4)
        .setIndicatorIndent(2)
        .setUseUnicodeEncoding(false)
        .setNonPrintableStyle(NonPrintableStyle.ESCAPE)
        .build();

    /**
     * The YAML unserialization settings to use.
     */
    private static LoadSettings loadSettings = LoadSettings.builder().build();

    /**
     * Serializes an object into a YAML representation.
     *
     * @param obj            the object to convert, or null
     * @param os             the output stream to write to
     *
     * @throws IOException if the data couldn't be serialized
     */
    public static void serialize(Object obj, OutputStream os) throws IOException {
        try (OutputStreamWriter ow = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
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
        return new Dump(dumpSettings).dumpToString(toYaml(obj));
    }

    /**
     * Unserializes an object from a YAML representation.
     *
     * @param yaml           the YAML text to load
     *
     * @return the object read
     *
     * @throws IOException if an error occurred while reading
     */
    public static Object unserialize(String yaml) throws IOException {
        return fromYaml(new Load(loadSettings).loadFromString(yaml));
    }

    /**
     * Converts a value to a YAML-serializable representation.
     *
     * @param obj            the value to convert
     *
     * @return the YAML-serializable representation
     */
    private static Object toYaml(Object obj) {
        if (obj instanceof Dict d) {
            return toYaml(d);
        } else if (obj instanceof Array a) {
            return toYaml(a);
        } else if (obj instanceof Map<?,?> m) {
            return toYaml(Dict.from(m));
        } else if (obj instanceof Iterable<?> i) {
            return toYaml(Array.from(i));
        } else if (obj instanceof Date dt) {
            return DateUtil.asEpochMillis(dt);
        } else if (obj == null || obj instanceof Boolean || obj instanceof Number) {
            return obj;
        } else {
            return toYaml(obj.toString());
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
        for (String k : dict.keys()) {
            Object v = dict.get(k);
            if (v != null) {
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

    /**
     * Converts a string to a YAML-serializable representation.
     *
     * @param str            the string to convert
     *
     * @return the YAML-serializable representation
     */
    private static Object toYaml(String str) {
        if (str.contains("\n")) {
            StringBuilder buf = new StringBuilder();
            for (String line : str.split("\\n")) {
                if (buf.length() > 0) {
                    buf.append("\n");
                }
                line = line.replace("\t", "    ");
                line = line.replace("\f", "");
                line = line.replace("\r", "");
                line = line.replace("\u00A0", " ");
                line = line.stripTrailing();
                buf.append(line);
            }
            return buf.toString();
        } else {
            return str.strip();
        }
    }

    /**
     * Converts a YAML-serialized object to a native one.
     *
     * @param obj            the unserialized object to convert
     *
     * @return the converted object
     */
    @SuppressWarnings("unchecked")
    private static Object fromYaml(Object obj) {
        if (obj instanceof Map<?,?> m) {
            return fromYaml(m);
        } else if (obj instanceof List<?> l) {
            return fromYaml(l);
        } else if (obj instanceof String s) {
            return ValueUtil.convert(s);
        } else {
            return obj;
        }
    }

    /**
     * Converts a YAML-serialized map to a Dict object.
     *
     * @param map            the unserialized map to convert
     *
     * @return the Dict containing the map data
     */
    private static Dict fromYaml(Map<?,?> map) {
        Dict dict = new Dict(map.size());
        for (var entry : map.entrySet()) {
            dict.set(Objects.toString(entry.getKey()), fromYaml(entry.getValue()));
        }
        return dict;
    }

    /**
     * Converts a YAML-serialized list to an Array object.
     *
     * @param list           the unserialized list to convert
     *
     * @return the Array containing the list data
     */
    private static Array fromYaml(List<?> list) {
        Array arr = new Array(list.size());
        for (Object item : list) {
            arr.add(fromYaml(item));
        }
        return arr;
    }

    // No instances
    private YamlSerializer() {}
}
