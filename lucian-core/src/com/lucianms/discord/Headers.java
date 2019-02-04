package com.lucianms.discord;

/**
 * @author izarooni
 */
public enum Headers {

    // @formatter:off
    Shutdown   ((byte) 0x0),
    SetFace    ((byte) 0x1),
    SetHair    ((byte) 0x2),
    Online     ((byte) 0x3),
    Bind       ((byte) 0x4),
    Search     ((byte) 0x5),
    Disconnect ((byte) 0x6),
    ReloadCS   ((byte) 0x7),
    MessageChannel((byte) 0x8);
    // @formatter:on
    public final byte value;

    Headers(byte value) {
        this.value = value;
    }
}
