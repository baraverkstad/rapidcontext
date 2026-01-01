/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

package org.rapidcontext.core.security;

import java.security.SecureRandom;

import org.rapidcontext.util.BinaryUtil;

/**
 * Helper methods for random data, useful for secrets and keys.
 *
 * @author Per Cederberg
 */
public class Random {

    /**
     * The random number generator.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Creates an array with random bytes.
     *
     * @param size           the array size (in bytes)
     *
     * @return the byte array
     */
    public static byte[] bytes(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Creates a Base64 encoded string with random bytes.
     *
     * @param size           the number of bytes
     *
     * @return the Base64 encoded string
     */
    public static String base64(int size) {
        return BinaryUtil.encodeBase64(bytes(size));
    }

    // No instances
    private Random() {}
}
