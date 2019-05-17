package com.lucianms.client.meta;

import com.lucianms.nio.send.MaplePacketWriter;

import java.util.EnumMap;

/**
 * @author izarooni
 */
public class ForcedStat {

    public enum Type {
        WHAT(0x1),
        STR(0x2),
        DEX(0x4),
        INT(0x8),
        LUK(0x10),
        PDD(0x20),
        MAD(0x40),
        MDD(0x80),
        ACCURACY(0x100),
        EVASION(0x200),
        SPEED(0x400),
        JUMP(0x800),
        SPEED_MAX(0x1000);
        public final int value;

        Type(int value) {
            this.value = value;
        }

        public boolean isEnabled(int mask) {
            return (value & mask) != 0;
        }
    }

    private int bitMask;
    private EnumMap<Type, Integer> forcedStats = new EnumMap<>(Type.class);

    public ForcedStat() {
        bitMask = 0;
        for (Type type : Type.values()) {
            forcedStats.put(type, 0);
        }
    }

    public void enableAll(int value) {
        bitMask = -1;
        for (Type type : Type.values()) {
            forcedStats.put(type, value);
        }
    }

    public void enable(Type type, int value) {
        bitMask |= type.value;
        forcedStats.put(type, value);
    }

    public void disable(Type type) {
        bitMask &= ~type.value;
        forcedStats.put(type, 0);
    }

    public void encode(MaplePacketWriter w) {
        w.writeInt(bitMask);
        for (Type type : Type.values()) {
            if (type.isEnabled(bitMask)) {
                Integer n = forcedStats.get(type);
                if (type.value >= Type.SPEED.value) {
                    w.write(n);
                } else {
                    w.writeShort(n);
                }
            }
        }
    }
}