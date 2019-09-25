package com.lucianms.events.meta;

/**
 * @author izarooni
 */
public enum CommunityActions {
    CREATE(0),
    INVITE(2),
    DECLINE(3),
    VISIT(4),
    ROOM(5),
    CHAT(6),
    CHAT_THING(8),
    EXIT(0xA),
    OPEN(0xB),
    TRADE_BIRTHDAY(0x0E),
    SET_ITEMS(0xF),
    SET_MESO(0x10),
    CONFIRM(0x11),
    TRANSACTION(0x14),
    ADD_ITEM(0x16),
    BUY(0x17),
    UPDATE_MERCHANT(0x19),
    REMOVE_ITEM(0x1B),
    BAN_PLAYER(0x1C),
    MERCHANT_THING(0x1D),
    OPEN_STORE(0x1E),
    PUT_ITEM(0x21),
    MERCHANT_BUY(0x22),
    TAKE_ITEM_BACK(0x26),
    MAINTENANCE_OFF(0x27),
    MERCHANT_ORGANIZE(0x28),
    CLOSE_MERCHANT(0x29),
    REAL_CLOSE_MERCHANT(0x2A),
    MERCHANT_MESO(0x2B),
    SOMETHING(0x2D),
    VIEW_VISITORS(0x2E),
    BLACKLIST(0x2F),
    REQUEST_TIE(0x32),
    ANSWER_TIE(0x33),
    GIVE_UP(0x34),
    EXIT_AFTER_GAME(0x38),
    CANCEL_EXIT(0x39),
    READY(0x3A),
    UN_READY(0x3B),
    START(0x3D),
    GET_RESULT(0x3E),
    SKIP(0x3F),
    MOVE_OMOK(0x40),
    SELECT_CARD(0x44);
    public final byte value;

    CommunityActions(int value) {
        this.value = (byte) value;
    }

    public static CommunityActions getByValue(byte value) {
        for (CommunityActions action : values()) {
            if (action.value == value) {
                return action;
            }
        }
        return null;
    }
}
