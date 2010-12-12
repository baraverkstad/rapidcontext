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

package org.rapidcontext.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A set of utility methods for handling binary data.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class BinaryUtil {

    /**
     * Performs an MD5 digest hash on the specified input string. The
     * result will be returned as an hexadecimal string.
     *
     * @param input          the input string
     *
     * @return the hexadecimal string with the MD5 hash
     *
     * @throws NoSuchAlgorithmException if the MD5 algorithm isn't
     *             available
     */
    public static String hashMD5(String input) throws NoSuchAlgorithmException {
        try {
            return toHexString(hashMD5(input.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            return toHexString(hashMD5(input.getBytes()));
        }
    }

    /**
     * Performs an MD5 digest hash on the specified byte array.
     *
     * @param data           the data to hash
     *
     * @return the MD5 hash of the data
     *
     * @throws NoSuchAlgorithmException if the MD5 algorithm isn't
     *             available
     */
    public static byte[] hashMD5(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.reset();
        digest.update(data);
        return digest.digest();
    }

    /**
     * Converts a string with hexadecimal numbers to a byte array.
     *
     * @param hexString      the hexadecimal string
     *
     * @return the byte array with the converted data
     */
    public static byte[] toBytes(String hexString) {
        int     len = hexString.length();
        byte[]  data = new byte[len / 2];
        int     high;
        int     low;

        for (int i = 0; i < len; i += 2) {
            high = Character.digit(hexString.charAt(i), 16);
            low = Character.digit(hexString.charAt(i+1), 16);
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }

    /**
     * Converts a byte array to a string with hexadecimal numbers.
     *
     * @param data           the byte array
     *
     * @return the hexadecimal string with the converted data
     */
    public static String toHexString(byte[] data) {
        StringBuffer  hexString = new StringBuffer();
        String        str;

        for (int i = 0; i < data.length; i++) {
            str = Integer.toHexString(data[i] & 0xFF);
            if (str.length() < 2) {
                hexString.append("0");
            }
            hexString.append(str);
        }
        return hexString.toString();
    }
}
