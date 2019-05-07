package com.lucianms.nio.receive;

import tools.HexTool;

import java.awt.*;

/**
 * @author izarooni
 */
public class MaplePacketReader extends LittleEndianAccessor {

    public static int Decode4(byte[] arr) {
        return (arr[0] & 0xFF) +
                ((arr[1] & 0xFF) << 8) +
                ((arr[2] & 0xFF) << 16) +
                ((arr[3] & 0xFF) << 24);
    }

    public MaplePacketReader(byte[] arr) {
        super(arr);
    }

    public short getHeader() {
        int b1 = (super.arr[0]);
        int b2 = (super.arr[1] & 0xFF) << 8;
        return (short) (b2 + b1);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public byte readByte() {
        return (byte) super.read();
    }

    public String readMapleAsciiString() {
        return readAsciiString(readShort());
    }

    public String readBytes(int length) {
        byte[] read = read(length);
        return HexTool.toString(read);
    }

    public Point readPoint() {
        return new Point(readShort(), readShort());
    }
}
