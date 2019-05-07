package com.lucianms.nio.send;


import tools.HexTool;

import java.awt.*;
import java.io.ByteArrayOutputStream;

/**
 * @author izarooni
 */
public class MaplePacketWriter extends LittleEndianWriter {

    /**
     * @return Little-Endian encoding representation of the specified integer
     */
    public static byte[] Encode4(int i) {
        return new byte[]{
                (byte) (i & 0xFF),
                (byte) ((i >>> 8) & 0xFF),
                (byte) ((i >>> 16) & 0xFF),
                (byte) ((i >>> 24) & 0xFF)
        };
    }

    private final ByteArrayOutputStream baos;

    public MaplePacketWriter() {
        this(32);
    }

    public MaplePacketWriter(int size) {
        baos = new ByteArrayOutputStream(size);
        setByteArrayOutputStream(baos);
    }

    public void writeMapleString(String s) {
        if (s == null) {
            throw new NullPointerException("Can't write a null string to the byte array");
        }
        writeShort(s.length());
        writeAsciiString(s);
    }

    public void writeLocation(Point loc) {
        writeShort(loc.x);
        writeShort(loc.y);
    }

    public void writeBoolean(boolean b) {
        baos.write(b ? 1 : 0);
    }

    public byte[] getPacket() {
        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return HexTool.toString(getPacket());
    }
}
