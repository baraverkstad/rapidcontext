/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2015 Per Cederberg. All rights reserved.
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
     * Calculates the MD5 digest hash on the UTF-8 encoding of an input string.
     * The result will be returned as an hexadecimal string.
     *
     * @param input          the input string
     *
     * @return the hexadecimal string with the MD5 hash
     *
     * @throws NoSuchAlgorithmException if the MD5 algorithm isn't
     *             available (should be RuntimeException)
     * @throws UnsupportedEncodingException if the UTF-8 encoding isn't
     *             available (should be RuntimeException)
     */
    public static String hashMD5(String input)
    throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return toHexString(hashBytes("MD5", input.getBytes("UTF-8")));
    }

    /**
     * Calculates the SHA-256 digest hash on the UTF-8 encoding of an input
     * string. The result will be returned as an hexadecimal string.
     *
     * @param input          the input string
     *
     * @return the hexadecimal string with the SHA-256 hash
     *
     * @throws NoSuchAlgorithmException if the SHA-256 algorithm isn't
     *             available (should be RuntimeException)
     * @throws UnsupportedEncodingException if the UTF-8 encoding isn't
     *             available (should be RuntimeException)
     */
    public static String hashSHA256(String input)
    throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return toHexString(hashBytes("SHA-256", input.getBytes("UTF-8")));
    }

    /**
     * Performs a digest hash on the specified byte array.
     *
     * @param alg            the hash algorithm (e.g. "MD5" or "SHA-256")
     * @param data           the data to hash
     *
     * @return the digest hash of the data
     *
     * @throws NoSuchAlgorithmException if the hash algorithm isn't available
     */
    public static byte[] hashBytes(String alg, byte[] data)
    throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(alg);
        digest.reset();
        digest.update(data);
        return digest.digest();
    }

    /**
     * Converts a byte array to a string with hexadecimal numbers.
     *
     * @param data           the byte array
     *
     * @return the hexadecimal string with the converted data
     */
    public static String toHexString(byte[] data) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            hexString.append(Character.forDigit(data[i] & 0xF0 >> 4, 16));
            hexString.append(Character.forDigit(data[i] & 0x0F, 16));
        }
        return hexString.toString();
    }
}
