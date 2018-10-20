package com.lucianms.nio.receive;


import tools.HexTool;

/**
 * @author izarooni
 */
public class LittleEndianAccessor {

    private int pos = 0; // the read index of the byte array
    final byte[] arr; // the array of byte content

    public LittleEndianAccessor(byte[] arr) {
        this.arr = arr;
    }

    @Override
    public String toString() {
        return HexTool.toString(arr);
    }

    int read() {
        return (((int) arr[pos++]) & 0xFF);
    }

    public short readShort() {
        int b1 = read();
        int b2 = read() << 8;
        return (short) (b1 + b2);
    }

    public int readInt() {
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            ret += read() << (i * 8);
        }
        return ret;
    }

    public long readLong() {
        long ret = 0;
        for (int i = 0; i < 8; i++) {
            ret += (long) read() << (i * 8);
        }
        return ret;
    }

    public void skip(int num) {
        for (int i = 0; i < num; i++) {
            read();
        }
    }

    public void goBack(int num) {
        int old = pos;
        pos -= num;
        if (pos < 0 || pos > arr.length) {
            pos = old;
            throw new IllegalArgumentException("Can't decrement or increment position beyond packet length below 0");
        }

    }

    public byte[] read(int num) {
        byte[] ret = new byte[num];
        for (int i = 0; i < num; i++) {
            ret[i] = (byte) read();
        }
        return ret;
    }

    public char readChar() {
        return (char) read();
    }

    public String readAsciiString(int n) {
        char ret[] = new char[n];
        for (int x = 0; x < n; x++) {
            ret[x] = readChar();
        }
        return String.valueOf(ret);
    }

    public int getPosition() {
        return pos;
    }

    public int available() {
        return arr.length - pos;
    }

    public int length() {
        return arr.length;
    }
}
