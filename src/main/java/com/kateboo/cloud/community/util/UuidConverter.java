package com.kateboo.cloud.community.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidConverter {

    public static byte[] stringToBytes(String uuid) {
        UUID id = UUID.fromString(uuid);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return bb.array();
    }

    public static String bytesToString(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong).toString();
    }
}