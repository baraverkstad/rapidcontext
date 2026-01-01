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

package org.rapidcontext.util;

import static org.junit.Assert.*;
import static org.rapidcontext.util.BinaryUtil.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class BinaryUtilTest {

    private static final String TEXT = "Hello, World!";
    private static final byte[] BYTES = TEXT.getBytes();
    private static final String EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e";
    private static final String TEXT_MD5 = "65a8e27d8879283831b664bd8b7f0ad4";
    private static final String TEXT_SHA1 = "0a0a9f2a6772942557ab5355d76af442f8f65e01";
    private static final String EMPTY_SHA2 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String TEXT_SHA2 = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";
    private static final String EMPTY_SHA3 = "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a";
    private static final String TEXT_SHA3 = "1af17a664e3fa8e419b8ba05c2a173169df76162a5a286e0c405b460d478f7ef";
    private static final String BASE64 = "SGVsbG8sIFdvcmxkIQ";

    @Test
    public void testHashMD5() throws NoSuchAlgorithmException {
        assertThrows(NullPointerException.class, () -> hashMD5(null));
        assertEquals(EMPTY_MD5, hashMD5(""));
        assertEquals(TEXT_MD5, hashMD5(TEXT));
    }

    @Test
    public void testHashSHA256() throws NoSuchAlgorithmException, IOException {
        assertThrows(NullPointerException.class, () -> hashSHA256((String) null));
        assertEquals(EMPTY_SHA2, hashSHA256(""));
        assertEquals(TEXT_SHA2, hashSHA256(TEXT));
        ByteArrayInputStream is = new ByteArrayInputStream(BYTES);
        assertEquals(TEXT_SHA2, hashSHA256(is));
    }

    @Test
    public void testHashSHA3() throws NoSuchAlgorithmException, IOException {
        assertThrows(NullPointerException.class, () -> hashSHA3((String) null));
        assertEquals(EMPTY_SHA3, hashSHA3(""));
        assertEquals(TEXT_SHA3, hashSHA3(TEXT));
        ByteArrayInputStream is = new ByteArrayInputStream(BYTES);
        assertEquals(TEXT_SHA3, hashSHA3(is));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testHashBytes() throws NoSuchAlgorithmException, IOException {
        assertEquals(TEXT_MD5, encodeHexString(hashBytes(Hash.MD5, BYTES)));
        assertEquals(TEXT_SHA1, encodeHexString(hashBytes(Hash.SHA1, BYTES)));
        assertEquals(TEXT_SHA2, encodeHexString(hashBytes(Hash.SHA2, BYTES)));
        assertEquals(TEXT_SHA3, encodeHexString(hashBytes(Hash.SHA3, BYTES)));
        assertThrows(NoSuchAlgorithmException.class, () -> hashBytes("INVALID", BYTES));
        ByteArrayInputStream is = new ByteArrayInputStream(BYTES);
        assertEquals(TEXT_SHA2, encodeHexString(hashBytes(Hash.SHA2, is)));
        assertThrows(NoSuchAlgorithmException.class, () -> {
            hashBytes("INVALID", new ByteArrayInputStream(BYTES));
        });
    }

    @Test
    public void testHmacSHA256() {
        assertThrows(NullPointerException.class, () -> hmacSHA256((String) null, (String) null));
        assertThrows(NullPointerException.class, () -> hmacSHA256((byte[]) null, (byte[]) null));
        assertThrows(SecurityException.class, () -> hmacSHA256("", "data"));
        String expect = "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8";
        byte[] bytes = hmacSHA256("key", "The quick brown fox jumps over the lazy dog");
        assertEquals(expect, encodeHexString(bytes));
    }

    @Test
    public void testEncodeHexString() {
        assertEquals(null, encodeHexString(null));
        assertEquals("", encodeHexString(new byte[0]));
        assertEquals("0001020a0fff", encodeHexString(new byte[]{0x00, 0x01, 0x02, 0x0A, 0x0F, (byte) 0xFF}));
    }

    @Test
    public void testBase64() {
        assertEquals(null, encodeBase64(null));
        assertEquals("", encodeBase64(new byte[0]));
        assertEquals(BASE64, encodeBase64(BYTES));

        assertArrayEquals(null, decodeBase64(null));
        assertArrayEquals(new byte[0], decodeBase64(""));
        assertArrayEquals(null, decodeBase64("\u00E5\u00E4\u00F6"));
        assertArrayEquals(BYTES, decodeBase64(BASE64));
        assertArrayEquals(BYTES, decodeBase64(BASE64 + "=="));
    }
}
