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

package org.rapidcontext.core.js;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.WrappedException;
import org.rapidcontext.core.data.Array;
import org.rapidcontext.core.data.Dict;
import org.rapidcontext.core.data.TextEncoding;
import org.rapidcontext.core.storage.StorableObject;
import org.rapidcontext.util.DateUtil;

/**
 * An object serializer and unserializer for the JavaScript object
 * notation (JSON) format. This class also provides methods for
 * wrapping dictionary and array object for access inside the
 * JavaScript engine. The object mapping to JavaScript is not exact,
 * and may omit serialization of data in some cases. The following
 * basic requirements must be met in order to serialize an object:
 *
 * <ul>
 *   <li>No circular references are permitted.
 *   <li>String, Integer, Boolean, Array or Dict objects are supported.
 * </ul>
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public final class JsSerializer {

    /**
     * The class logger.
     */
    private static final Logger LOG =
        Logger.getLogger(JsSerializer.class.getName());

    /**
     * Serializes an object into a JavaScript literal (JSON). The
     * string returned can be used as a constant inside JavaScript
     * code or returned via a JSON API. If the indentation flag is
     * set, the JSON data will be indented and formatted. Otherwise
     * a minimal string will be returned.
     *
     * @param obj            the object to convert, or null
     * @param indent         the indentation flag
     *
     * @return a JavaScript literal
     */
    public static String serialize(Object obj, boolean indent) {
        StringBuilder buffer = new StringBuilder();
        serialize(JsRuntime.unwrap(obj), indent ? 0 : -1, buffer);
        if (indent) {
            buffer.append("\n");
        }
        return buffer.toString();
    }

    /**
     * Serializes an object into a JavaScript literal (JSON). The
     * string returned can be used as a constant inside JavaScript
     * code or returned via a JSON API. If the indentation flag is
     * set, the JSON data will be indented and formatted. Otherwise
     * a minimal string will be created. The serialized result will
     * be written into the specified string buffer.
     *
     * @param obj            the object to convert, or null
     * @param indent         the indentation level, or -1 for none
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Object obj, int indent, StringBuilder buffer) {
        if (obj == null) {
            buffer.append("null");
        } else if (obj instanceof Dict) {
            serialize((Dict) obj, indent, buffer);
        } else if (obj instanceof Array) {
            serialize((Array) obj, indent, buffer);
        } else if (obj instanceof Boolean) {
            buffer.append(obj.toString());
        } else if (obj instanceof Number) {
            serialize((Number) obj, buffer);
        } else if (obj instanceof Date) {
            buffer.append(TextEncoding.encodeJson(DateUtil.asEpochMillis((Date) obj)));
        } else if (obj instanceof Class) {
            buffer.append(TextEncoding.encodeJson(((Class<?>) obj).getName()));
        } else if (obj instanceof StorableObject) {
            serialize(((StorableObject) obj).serialize(), indent, buffer);
        } else {
            buffer.append(TextEncoding.encodeJson(obj.toString()));
        }
    }

    /**
     * Serializes a dictionary into a JavaScript literal (JSON). The
     * string returned can be used as a constant inside JavaScript
     * code or returned via a JSON API. If the indentation flag is
     * set, the JSON data will be indented and formatted. Otherwise
     * a minimal string will be created. The serialized result will
     * be written into the specified string buffer.
     *
     * @param dict           the dictionary to convert
     * @param indent         the indentation level, or -1 for none
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Dict dict, int indent, StringBuilder buffer) {
        int indentNext = (indent < 0) ? indent : indent + 1;
        String prefix = "";
        String infix = ":";
        if (indent >= 0) {
            prefix = "\n" + StringUtils.repeat("  ", indent + 1);
            infix += " ";
        }
        String[] keys = dict.keys();
        buffer.append("{");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                buffer.append(",");
            }
            buffer.append(prefix);
            buffer.append(TextEncoding.encodeJson(keys[i]));
            buffer.append(infix);
            serialize(dict.get(keys[i]), indentNext, buffer);
        }
        if (keys.length > 0 && indent >= 0) {
            buffer.append("\n");
            buffer.append(StringUtils.repeat("  ", indent));
        }
        buffer.append("}");
    }

    /**
     * Serializes an array into a JavaScript literal (JSON). The
     * string returned can be used as a constant inside JavaScript
     * code or returned via a JSON API. If the indentation flag is
     * set, the JSON data will be indented and formatted. Otherwise
     * a minimal string will be created. The serialized result will
     * be written into the specified string buffer.
     *
     * @param arr            the array to convert
     * @param indent         the indentation level, or -1 for none
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Array arr, int indent, StringBuilder buffer) {
        int indentNext = (indent < 0) ? indent : indent + 1;
        String prefix = "";
        if (indent >= 0) {
            prefix = "\n" + StringUtils.repeat("  ", indent + 1);
        }
        buffer.append("[");
        for (int i = 0; i < arr.size(); i++) {
            if (i > 0) {
                buffer.append(",");
            }
            buffer.append(prefix);
            serialize(arr.get(i), indentNext, buffer);
        }
        if (arr.size() > 0 && indent >= 0) {
            buffer.append("\n");
            buffer.append(StringUtils.repeat("  ", indent));
        }
        buffer.append("]");
    }

    /**
     * Serializes a number into a JavaScript literal (JSON). The
     * string returned can be used as a constant inside JavaScript
     * code or returned via a JSON API. The serialized result will
     * be written into the specified string buffer.
     *
     * @param num            the number to convert, or null
     * @param buffer         the string buffer to append into
     */
    private static void serialize(Number num, StringBuilder buffer) {
        int     i = num.intValue();
        double  d = num.doubleValue();

        if (i == d) {
            buffer.append(i);
        } else {
            // TODO: proper number formatting should be used
            buffer.append(num);
        }
    }

    /**
     * Unserializes a JavaScript literal into a Java object. I.e.
     * this method converts a JSON object into the corresponding
     * String, Number, Boolean, Dict and/or Array objects.
     *
     * @param str            the string to convert, or null
     *
     * @return the corresponding Java object
     *
     * @throws JsException if the unserialization failed
     */
    public static Object unserialize(String str) throws JsException {
        Context  cx;
        Object   obj;
        String   msg;

        cx = ContextFactory.getGlobal().enterContext();
        try {
            str = "(" + str + ")";
            obj = cx.evaluateString(cx.initStandardObjects(),
                                    str,
                                    "unserialize",
                                    1,
                                    null);
            return JsRuntime.unwrap(obj);
        } catch (WrappedException e) {
            msg = "Caught unserialization exception for text: " + str;
            LOG.log(Level.WARNING, msg, e);
            throw new JsException(msg, e.getWrappedException());
        } catch (Exception e) {
            msg = "Caught unserialization exception for text: " + str;
            LOG.log(Level.WARNING, msg, e);
            throw new JsException(msg, e);
        } finally {
            Context.exit();
        }
    }

    // No instances
    private JsSerializer() {}
}
