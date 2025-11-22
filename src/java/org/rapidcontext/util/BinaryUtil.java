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

package org.rapidcontext.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * A set of utility methods for handling binary data.
 *
 * @author Per Cederberg
 */
public final class BinaryUtil {

    /**
     * The currently supported hash algorithms.
     */
    public static interface Hash {

        /**
         * The MD5 (128 bit) hash algorithm. Cryptographically broken
         * and unsuitable for further use.
         *
         * @deprecated Use SHA2 or SHA3 instead.
         */
        @Deprecated
        public static final String MD5 = "MD5";

        /**
         * The SHA-1 (160 bit) hash algorithm. Unsuitable for security
         * usage (i.e. passwords), but may be used as data checksum.
         *
         * @deprecated Use SHA2 or SHA3 instead.
         */
        @Deprecated
        public static final String SHA1 = "SHA-1";

        /**
         * The SHA-256 (256 bit) hash algorithm.
         */
        public static final String SHA2 = "SHA-256";

        /**
         * The SHA3-256 (256 bit) hash algorithm.
         */
        public static final String SHA3 = "SHA3-256";
    }

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
     */
    public static String hashMD5(String input)
    throws NoSuchAlgorithmException {
        return encodeHexString(hashBytes(Hash.MD5, input.getBytes(StandardCharsets.UTF_8)));
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
     */
    public static String hashSHA256(String input)
    throws NoSuchAlgorithmException {
        return encodeHexString(hashBytes(Hash.SHA2, input.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Calculates the SHA-256 digest hash on the bytes of an input file.
     * The result will be returned as an hexadecimal string.
     *
     * @param input          the input stream to read
     *
     * @return the hexadecimal string with the SHA-256 hash
     *
     * @throws NoSuchAlgorithmException if the SHA-256 algorithm isn't
     *             available (should be RuntimeException)
     * @throws IOException if the file couldn't be found or read
     *
     * @see Hash#SHA2
     */
    public static String hashSHA256(InputStream input)
    throws NoSuchAlgorithmException, IOException {
        return encodeHexString(hashBytes(Hash.SHA2, input));
    }

    /**
     * Calculates the SHA3-256 digest hash on the UTF-8 encoding of an input
     * string. The result will be returned as an hexadecimal string.
     *
     * @param input          the input string
     *
     * @return the hexadecimal string with the SHA3-256 hash
     *
     * @throws NoSuchAlgorithmException if the SHA3-256 algorithm isn't
     *             available (should be RuntimeException)
     *
     * @see Hash#SHA3
     */
    public static String hashSHA3(String input)
    throws NoSuchAlgorithmException {
        return encodeHexString(hashBytes(Hash.SHA3, input.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Calculates the SHA3-256 digest hash on the bytes of an input file.
     * The result will be returned as an hexadecimal string.
     *
     * @param input          the input stream to read
     *
     * @return the hexadecimal string with the SHA-256 hash
     *
     * @throws NoSuchAlgorithmException if the SHA-256 algorithm isn't
     *             available (should be RuntimeException)
     * @throws IOException if the file couldn't be found or read
     *
     * @see Hash#SHA3
     */
    public static String hashSHA3(InputStream input)
    throws NoSuchAlgorithmException, IOException {
        return encodeHexString(hashBytes(Hash.SHA3, input));
    }

    /**
     * Performs a digest hash on the specified byte array.
     *
     * @param alg            the supported hash algorithm
     * @param data           the data to hash
     *
     * @return the digest hash of the data
     *
     * @throws NoSuchAlgorithmException if the hash algorithm isn't available
     *
     * @see Hash#SHA2
     * @see Hash#SHA3
     */
    public static byte[] hashBytes(String alg, byte[] data)
    throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(alg);
        digest.reset();
        digest.update(data);
        return digest.digest();
    }

    /**
     * Performs a digest hash on the data from an input stream.
     *
     * @param alg            the supported hash algorithm
     * @param input          the input stream to read
     *
     * @return the digest hash of the data
     *
     * @throws NoSuchAlgorithmException if the hash algorithm isn't available
     * @throws IOException if the input stream couldn't be read
     *
     * @see Hash#SHA2
     * @see Hash#SHA3
     */
    public static byte[] hashBytes(String alg, InputStream input)
    throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(alg);
        digest.reset();
        try (InputStream is = input) {
            byte[] buffer = new byte[16384];
            int size;
            do {
                size = is.read(buffer);
                if (size > 0) {
                    digest.update(buffer, 0, size);
                }
            } while (size > 0);
        }
        return digest.digest();
    }

    /**
     * Encodes a byte array to a string with hexadecimal numbers.
     *
     * @param data           the byte array
     *
     * @return the hexadecimal string with the converted data
     */
    public static String encodeHexString(byte[] data) {
        if (data == null) {
            return null;
        }
        StringBuilder hexString = new StringBuilder();
        for (byte b : data) {
            hexString.append(Character.forDigit((b & 0xF0) >> 4, 16));
            hexString.append(Character.forDigit(b & 0x0F, 16));
        }
        return hexString.toString();
    }

    /**
     * Encodes a byte array to a string with Base64 characters (websafe).
     *
     * @param data           the byte array
     *
     * @return the Base64 string with the converted data
     */
    public static String encodeBase64(byte[] data) {
        return (data == null) ? null : Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Decodes a Base64 string back to the original byte array.
     *
     * @param data           the Base64-encoded string
     *
     * @return the decoded original byte array
     */
    public static byte[] decodeBase64(String data) {
        try {
            return (data == null) ? null : Base64.getUrlDecoder().decode(data);
        } catch (Exception e) {
            return null;
        }
    }

    // No instances
    private BinaryUtil() {}
}
