package com.findex.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CursorCodec {

    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    public static String encodeString(String value) {
        byte[] b = (value == null) ? new byte[0] : value.getBytes(UTF8);
        return B64E.encodeToString(b);
    }

    public static String decodeString(String cursor) {
        return new String(B64D.decode(cursor), UTF8);
    }

    public static String encodeNumber(Integer value) {
        int v = value != null ? value : Integer.MIN_VALUE;

        ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(v);
        return B64E.encodeToString(buf.array());
    }

    public static int decodeNumber(String cursor) {
        byte[] b = B64D.decode(cursor);
        if (b.length != 4) {
            throw new IllegalArgumentException("Invalid numeric cursor");
        }
        return ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getInt();
    }
}
