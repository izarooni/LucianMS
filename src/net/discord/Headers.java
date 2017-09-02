package net.discord;

/**
 * @author izarooni
 */
public enum Headers {

    // @formatter:off
    Shutdown((byte) 0x0),
    SetFace((byte) 0x1),
    SetHair((byte) 0x2),
    Online((byte) 0x3);
    // @formatter:on
    public final byte value;

    Headers(byte value) {
        this.value = value;
    }
}
