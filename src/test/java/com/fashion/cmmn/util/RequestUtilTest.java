package com.fashion.cmmn.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestUtilTest {

    @Test
    void loopbackWithHeaderUsesHeader() {
        assertEquals("203.0.113.7", RequestUtil.clientIp("127.0.0.1", "203.0.113.7"));
        assertEquals("203.0.113.7", RequestUtil.clientIp("0:0:0:0:0:0:0:1", " 203.0.113.7 "));
    }

    @Test
    void loopbackWithoutHeaderUsesRemote() {
        assertEquals("127.0.0.1", RequestUtil.clientIp("127.0.0.1", null));
        assertEquals("127.0.0.1", RequestUtil.clientIp("127.0.0.1", "  "));
    }

    @Test
    void nonLoopbackIgnoresHeader() {
        assertEquals("198.51.100.2", RequestUtil.clientIp("198.51.100.2", "203.0.113.7"));
    }

    @Test
    void tooLongHeaderIgnored() {
        assertEquals("127.0.0.1", RequestUtil.clientIp("127.0.0.1", "1".repeat(46)));
    }
}
