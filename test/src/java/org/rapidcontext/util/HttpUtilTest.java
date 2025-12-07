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

import static org.junit.Assert.assertNull;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.rapidcontext.util.HttpUtil.Helper.*;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class HttpUtilTest {

    @Test
    public void testBrowserInfo() {
        String ua;

        // Chrome
        ua = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
        assertThat(browserInfo(ua), startsWith("Chrome 41.0.2228.0, Windows NT 6.1, Desktop"));
        ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.124 Safari/537.36";
        assertThat(browserInfo(ua), startsWith("Chrome 37.0.2062.124, Mac OS X 10.10.1, Desktop"));
        ua = "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19";
        assertThat(browserInfo(ua), is("Chrome 18.0.1025.133, Android 4.0.4, Mobile"));
        ua = "Mozilla/5.0 (Linux; Android 5.1.1; SM-G928X Build/LMY47X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36";
        assertThat(browserInfo(ua), is("Chrome 47.0.2526.83, Android 5.1.1, Mobile"));
        ua = "Mozilla/5.0 (iPhone; U; CPU iPhone OS 5_1_1 like Mac OS X; en-gb) AppleWebKit/534.46.0 (KHTML, like Gecko) CriOS/19.0.1084.60 Mobile/9B206 Safari/7534.48.3";
        assertThat(browserInfo(ua), is("Chrome iOS 19.0.1084.60, iOS 5.1.1, iPhone"));

        // Edge
        ua = "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10136";
        assertThat(browserInfo(ua), startsWith("Edge 12.10136, Windows NT 10.0, Desktop"));
        ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36 Edg/100.0.1185.39";
        assertThat(browserInfo(ua), startsWith("Edg 100.0.1185.39, Windows NT 10.0, Desktop"));
        ua = "Mozilla/5.0 (Windows Phone 10.0; Android 4.2.1; Microsoft; Lumia 950) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Mobile Safari/537.36 Edge/13.10586";
        assertThat(browserInfo(ua), is("Edge 13.10586, Android 4.2.1, Mobile"));
        ua = "Mozilla/5.0 (Linux; Android 10; HD1913) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.61 Mobile Safari/537.36 EdgA/100.0.1185.50";
        assertThat(browserInfo(ua), is("EdgA 100.0.1185.50, Android 10, Mobile"));

        // MSIE
        ua = "Mozilla/5.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)";
        assertThat(browserInfo(ua), startsWith("MSIE 6.0, Windows NT 5.1, Desktop"));
        ua = "Mozilla/5.0 (compatible; MSIE 7.0; Windows NT 6.0; en-US))";
        assertThat(browserInfo(ua), startsWith("MSIE 7.0, Windows NT 6.0, Desktop"));
        ua = "Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)";
        assertThat(browserInfo(ua), startsWith("MSIE 8.0, Windows NT 5.1, Desktop"));
        ua = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; chromeframe/12.0.742.112)";
        assertThat(browserInfo(ua), startsWith("MSIE 9.0, Windows NT 6.1, Desktop"));
        ua = "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 7.0; InfoPath.3; .NET CLR 3.1.40767; Trident/6.0; en-IN)";
        assertThat(browserInfo(ua), startsWith("MSIE 10.0, Windows NT 7.0, Desktop"));
        ua = "Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko";
        assertThat(browserInfo(ua), startsWith("MSIE 11.0, Windows NT 6.3, Desktop"));
        ua = "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko";
        assertThat(browserInfo(ua), startsWith("MSIE 11.0, Windows NT 6.1, Desktop"));

        // Firefox
        ua = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1";
        assertThat(browserInfo(ua), startsWith("Firefox 40.1, Windows NT 6.1, Desktop"));
        ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:25.0) Gecko/20100101 Firefox/25.0";
        assertThat(browserInfo(ua), startsWith("Firefox 25.0, Mac OS X 10.6, Desktop"));
        ua = "Mozilla/5.0 (X11; Linux i686 on x86_64; rv:10.0) Gecko/20100101 Firefox/10.0";
        assertThat(browserInfo(ua), startsWith("Firefox 10.0, Linux i686 on x86_64, Desktop"));
        ua = "Mozilla/5.0 (Android 4.4; Mobile; rv:41.0) Gecko/41.0 Firefox/41.0";
        assertThat(browserInfo(ua), startsWith("Firefox 41.0, Android 4.4, Mobile"));
        ua = "Mozilla/5.0 (Android 4.4; Tablet; rv:41.0) Gecko/41.0 Firefox/41.0";
        assertThat(browserInfo(ua), startsWith("Firefox 41.0, Android 4.4, Tablet"));
        ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_3 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) FxiOS/1.0 Mobile/12F69 Safari/600.1.4";
        assertThat(browserInfo(ua), is("Firefox iOS 1.0, iOS 8.3, iPhone"));
        ua = "Mozilla/5.0 (iPad; CPU iPhone OS 8_3 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) FxiOS/1.0 Mobile/12F69 Safari/600.1.4";
        assertThat(browserInfo(ua), is("Firefox iOS 1.0, iOS 8.3, iPad"));

        // Safari
        ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A";
        assertThat(browserInfo(ua), startsWith("Safari 7.0.3, Mac OS X 10.9.3, Desktop"));
        ua = "Mozilla/5.0 (Windows; U; Windows NT 6.1; ko-KR) AppleWebKit/533.20.25 (KHTML, like Gecko) Version/5.0.4 Safari/533.20.27";
        assertThat(browserInfo(ua), startsWith("Safari 5.0.4, Windows NT 6.1, Desktop"));
        ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 9_2 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13C75 Safari/601.1";
        assertThat(browserInfo(ua), is("Safari 9.0, iOS 9.2, iPhone"));
        ua = "Mozilla/5.0 (iPad; CPU OS 9_0 like Mac OS X) AppleWebKit/601.1.16 (KHTML, like Gecko) Version/8.0 Mobile/13A171a Safari/600.1.4";
        assertThat(browserInfo(ua), is("Safari 8.0, iOS 9.0, iPad"));

        // Unknown
        ua = "curl/7.9.8 (i686-pc-linux-gnu) libcurl 7.9.8 (OpenSSL 0.9.6b) (ipv6 enabled)";
        assertNull(browserInfo(ua));
        ua = "Mozilla/5.0 (compatible; Konqueror/3.5; Linux) KHTML/3.5.10 (like Gecko) (Debian)";
        assertNull(browserInfo(ua));
        ua = "Opera/9.80 (Android 2.3.3; Linux; Opera Mobi/ADR-1111101157; U; es-ES) Presto/2.9.201 Version/11.50";
        assertNull(browserInfo(ua));
    }
}
