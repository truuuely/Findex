package com.findex.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CursorCodec {

    public static String encodeString(String value) {
        byte[] b = (value == null) ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String decodeString(String cursor) {
        byte[] b = Base64.getUrlDecoder().decode(cursor);
        return new String(b, StandardCharsets.UTF_8);
    }

    public static String encodeNumber(Integer value) {
        int v = value != null ? value : Integer.MIN_VALUE;
        ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(v);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array());
    }

    public static int decodeNumber(String cursor) {
        byte[] b = Base64.getUrlDecoder().decode(cursor);
        if (b.length != 4) {
            throw new IllegalArgumentException("Invalid numeric cursor");
        }
        return ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getInt();
    }
}
