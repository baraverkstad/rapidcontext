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

package org.rapidcontext.app.ui;

import java.io.InputStream;

import org.apache.commons.lang3.SystemUtils;

/**
 * Provides a few utilities for UI applications.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class AppUtils {

    /**
     * The default array of browser open commands (on Linux).
     */
    private static final String[] BROWSERS = {
        "xdg-open", "gnome-open", "kde-open",
        "google-chrome", "firefox", "mozilla"
    };

    /**
     * Checks if a command is available (in a Unix environment).
     *
     * @param command        the command-line application name
     *
     * @return true if the application is available, or
     *         false otherwise
     */
    public static boolean hasCommand(String command) {
        String[] cmd = {"which", command};
        try (InputStream is = Runtime.getRuntime().exec(cmd).getInputStream()) {
            return is.read() != -1;
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * Opens the specified URL in the user's default browser.
     *
     * @param url            the URL to open
     *
     * @throws Exception if the URL failed to open or if no browser was found
     */
    public static void openURL(String url) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        if (SystemUtils.IS_OS_MAC_OSX) {
            runtime.exec(new String[]{"open", url});
        } else if (SystemUtils.IS_OS_WINDOWS) {
            runtime.exec(new String[]{"cmd.exe", "/C", "start", url});
        } else {
            for (String cmd : BROWSERS) {
                if (hasCommand(cmd)) {
                    runtime.exec(new String[]{cmd, url});
                    return;
                }
            }
            throw new Exception("No browser found.");
        }
    }
}
