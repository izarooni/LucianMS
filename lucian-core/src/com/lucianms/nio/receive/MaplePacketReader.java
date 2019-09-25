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

    /**
     * From the Microsoft's .NET API for BinaryReader#ReadString
     * <p>
     * Reads a string from the current stream. The string is prefixed with the length, encoded as an integer seven bits
     * at a time.
     * </p>
     * <p>
     * Should there be data still present after reading an unsigned byte, shift the bytes over and read the next byte
     * until all data has been decoded
     * </p>
     */
    public String read7BitEncodedString() {
        int value = 0;
        for (int i = 0; ; i++) {
            int b = read();
            value |= (b & 0xFF) << (i * 7);
            if ((b & 0x80) == 0) break;
        }
        return readAsciiString(value);
    }
}
