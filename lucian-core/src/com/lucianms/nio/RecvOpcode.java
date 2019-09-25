package com.lucianms.nio;

import com.lucianms.events.ClientCrashReportEvent;
import com.lucianms.events.IgnoredPacketEvent;
import com.lucianms.events.KeepAliveEvent;
import com.lucianms.events.PacketEvent;

public enum RecvOpcode {

    // @formatter:off
    LOGIN_PASSWORD          (1, ReceivePacketState.LoginServer),
    GUEST_LOGIN             (2, ReceivePacketState.LoginServer),
    SERVERLIST_REREQUEST    (4, ReceivePacketState.LoginServer),
    CHARLIST_REQUEST        (5, ReceivePacketState.LoginServer),
    SERVERSTATUS_REQUEST    (6, ReceivePacketState.LoginServer),
    ACCEPT_TOS              (7, ReceivePacketState.LoginServer),
    SET_GENDER              (8, ReceivePacketState.LoginServer),
    AFTER_LOGIN             (9, ReceivePacketState.LoginServer),
    REGISTER_PIN            (10, ReceivePacketState.LoginServer),
    SERVERLIST_REQUEST      (11, ReceivePacketState.LoginServer),
    PLAYER_DC               (12, ReceivePacketState.LoginServer),
    VIEW_ALL_CHAR           (13, ReceivePacketState.LoginServer),
    PICK_ALL_CHAR           (14, ReceivePacketState.LoginServer),
    CHAR_SELECT             (19, ReceivePacketState.LoginServer),
    PLAYER_LOGGEDIN         (20, ReceivePacketState.ChannelServer),
    CHECK_CHAR_NAME         (21, ReceivePacketState.LoginServer),
    CREATE_CHAR             (22, ReceivePacketState.LoginServer),
    DELETE_CHAR             (23, ReceivePacketState.LoginServer),
    PONG                    (24, ReceivePacketState.Both, KeepAliveEvent.class),
    CLIENT_START_ERROR      (25, ReceivePacketState.LoginServer, ClientCrashReportEvent.class),
    CLIENT_ERROR            (26, ReceivePacketState.LoginServer),
    STRANGE_DATA            (27, ReceivePacketState.Both),
    RELOG                   (28, ReceivePacketState.LoginServer),
    REGISTER_PIC            (29, ReceivePacketState.LoginServer),
    CHAR_SELECT_WITH_PIC    (30, ReceivePacketState.LoginServer),
    VIEW_ALL_PIC_REGISTER   (31, ReceivePacketState.LoginServer),
    VIEW_ALL_WITH_PIC       (32, ReceivePacketState.LoginServer),
    UNKNOWN                 (35, ReceivePacketState.Both, IgnoredPacketEvent.class),
    CHANGE_MAP              (38, ReceivePacketState.ChannelServer),
    CHANGE_CHANNEL          (39, ReceivePacketState.ChannelServer),
    ENTER_CASHSHOP          (40, ReceivePacketState.ChannelServer),
    MOVE_PLAYER             (41, ReceivePacketState.ChannelServer),
    CANCEL_CHAIR            (42, ReceivePacketState.ChannelServer),
    USE_CHAIR               (43, ReceivePacketState.ChannelServer),
    CLOSE_RANGE_ATTACK      (44, ReceivePacketState.ChannelServer),
    RANGED_ATTACK           (45, ReceivePacketState.ChannelServer),
    MAGIC_ATTACK            (46, ReceivePacketState.ChannelServer),
    TOUCH_MONSTER_ATTACK    (47, ReceivePacketState.ChannelServer),
    TAKE_DAMAGE             (48, ReceivePacketState.ChannelServer),
    GENERAL_CHAT            (49, ReceivePacketState.ChannelServer),
    CLOSE_CHALKBOARD        (50, ReceivePacketState.ChannelServer),
    FACE_EXPRESSION         (51, ReceivePacketState.ChannelServer),
    USE_ITEMEFFECT          (52, ReceivePacketState.ChannelServer),
    USE_DEATHITEM           (53, ReceivePacketState.ChannelServer),
    MONSTER_BOOK_COVER      (57, ReceivePacketState.ChannelServer),
    NPC_TALK                (58, ReceivePacketState.ChannelServer),
    REMOTE_STORE            (59, ReceivePacketState.ChannelServer),
    NPC_TALK_MORE           (60, ReceivePacketState.ChannelServer),
    NPC_SHOP                (61, ReceivePacketState.ChannelServer),
    STORAGE                 (62, ReceivePacketState.ChannelServer),
    HIRED_MERCHANT_REQUEST  (63, ReceivePacketState.ChannelServer),
    FREDRICK_ACTION         (64, ReceivePacketState.ChannelServer),
    DUEY_ACTION             (65, ReceivePacketState.ChannelServer),
    ADMIN_SHOP              (68, ReceivePacketState.ChannelServer),
    ITEM_SORT               (69, ReceivePacketState.ChannelServer),
    ITEM_SORT2              (70, ReceivePacketState.ChannelServer),
    ITEM_MOVE               (71, ReceivePacketState.ChannelServer),
    USE_ITEM                (72, ReceivePacketState.ChannelServer),
    CANCEL_ITEM_EFFECT      (73, ReceivePacketState.ChannelServer),
    UNKNOWN2                 (74, ReceivePacketState.Both, IgnoredPacketEvent.class),
    USE_SUMMON_BAG          (75, ReceivePacketState.ChannelServer),
    PET_FOOD                (76, ReceivePacketState.ChannelServer),
    USE_MOUNT_FOOD          (77, ReceivePacketState.ChannelServer),
    SCRIPTED_ITEM           (78, ReceivePacketState.ChannelServer),
    USE_CASH_ITEM           (79, ReceivePacketState.ChannelServer),
    USE_CATCH_ITEM          (81, ReceivePacketState.ChannelServer),
    USE_SKILL_BOOK          (82, ReceivePacketState.ChannelServer),
    USE_TELEPORT_ROCK       (84, ReceivePacketState.ChannelServer),
    USE_RETURN_SCROLL       (85, ReceivePacketState.ChannelServer),
    USE_UPGRADE_SCROLL      (86, ReceivePacketState.ChannelServer),
    DISTRIBUTE_AP           (87, ReceivePacketState.ChannelServer),
    AUTO_DISTRIBUTE_AP      (88, ReceivePacketState.ChannelServer),
    HEAL_OVER_TIME          (89, ReceivePacketState.ChannelServer),
    DISTRIBUTE_SP           (90, ReceivePacketState.ChannelServer),
    SPECIAL_MOVE            (91, ReceivePacketState.ChannelServer),
    CANCEL_BUFF             (92, ReceivePacketState.ChannelServer),
    SKILL_EFFECT            (93, ReceivePacketState.ChannelServer),
    MESO_DROP               (94, ReceivePacketState.ChannelServer),
    GIVE_FAME               (95, ReceivePacketState.ChannelServer),
    CHAR_INFO_REQUEST       (97, ReceivePacketState.ChannelServer),
    SPAWN_PET               (98, ReceivePacketState.ChannelServer),
    RESET_TEMP_STAT         (99, ReceivePacketState.ChannelServer),
    CHANGE_MAP_SPECIAL      (100, ReceivePacketState.ChannelServer),
    USE_INNER_PORTAL        (101, ReceivePacketState.ChannelServer),
    TROCK_ADD_MAP           (102, ReceivePacketState.ChannelServer),
    REPORT                  (106, ReceivePacketState.ChannelServer),
    QUEST_ACTION            (107, ReceivePacketState.ChannelServer),
    DAMAGE_STAT_CHANGED     (108, ReceivePacketState.ChannelServer),
    SKILL_MACRO             (110, ReceivePacketState.ChannelServer),
    USE_ITEM_REWARD         (112, ReceivePacketState.ChannelServer),
    MAKER_SKILL             (113, ReceivePacketState.ChannelServer),
    USE_REMOTE              (116, ReceivePacketState.ChannelServer),
    ADMIN_CHAT              (118, ReceivePacketState.ChannelServer),
    PARTYCHAT               (119, ReceivePacketState.ChannelServer),
    WHISPER                 (120, ReceivePacketState.ChannelServer),
    SPOUSE_CHAT             (121, ReceivePacketState.ChannelServer),
    MESSENGER               (122, ReceivePacketState.ChannelServer),
    PLAYER_INTERACTION      (123, ReceivePacketState.ChannelServer),
    PARTY_OPERATION         (124, ReceivePacketState.ChannelServer),
    DENY_PARTY_REQUEST      (125, ReceivePacketState.ChannelServer),
    GUILD_OPERATION         (126, ReceivePacketState.ChannelServer),
    DENY_GUILD_REQUEST      (127, ReceivePacketState.ChannelServer),
    ADMIN_COMMAND           (128, ReceivePacketState.ChannelServer),
    ADMIN_LOG               (129, ReceivePacketState.ChannelServer, IgnoredPacketEvent.class),
    BUDDYLIST_MODIFY        (130, ReceivePacketState.ChannelServer),
    NOTE_ACTION             (131, ReceivePacketState.ChannelServer),
    USE_DOOR                (133, ReceivePacketState.ChannelServer),
    CHANGE_KEYMAP           (135, ReceivePacketState.ChannelServer),
    RPS_ACTION              (136, ReceivePacketState.ChannelServer),
    RING_ACTION             (137, ReceivePacketState.ChannelServer),
    UNKNOWN3                (143, ReceivePacketState.Both, IgnoredPacketEvent.class),
    WEDDING_ACTION          (138, ReceivePacketState.ChannelServer),
    VIEW_FAMILY_PEDIGREE    (145, ReceivePacketState.ChannelServer),
    OPEN_FAMILY             (146, ReceivePacketState.ChannelServer),
    ADD_FAMILY              (147, ReceivePacketState.ChannelServer),
    ACCEPT_FAMILY           (150, ReceivePacketState.ChannelServer),
    USE_FAMILY              (151, ReceivePacketState.ChannelServer),
    ALLIANCE_OPERATION      (152, ReceivePacketState.ChannelServer),
    BBS_OPERATION           (155, ReceivePacketState.ChannelServer),
    ENTER_MTS               (156, ReceivePacketState.ChannelServer),
    USE_SOLOMON_ITEM        (157, ReceivePacketState.ChannelServer),
    USE_GACHA_EXP           (158, ReceivePacketState.ChannelServer),
    CLICK_GUIDE             (162, ReceivePacketState.ChannelServer),
    ARAN_COMBO_COUNTER      (163, ReceivePacketState.ChannelServer),
    MOVE_PET                (167, ReceivePacketState.ChannelServer),
    PET_CHAT                (168, ReceivePacketState.ChannelServer),
    PET_COMMAND             (169, ReceivePacketState.ChannelServer),
    PET_LOOT                (170, ReceivePacketState.ChannelServer),
    PET_AUTO_POT            (171, ReceivePacketState.ChannelServer),
    PET_EXCLUDE_ITEMS       (172, ReceivePacketState.ChannelServer),
    MOVE_SUMMON             (175, ReceivePacketState.ChannelServer),
    SUMMON_ATTACK           (176, ReceivePacketState.ChannelServer),
    DAMAGE_SUMMON           (177, ReceivePacketState.ChannelServer),
    BEHOLDER                (178, ReceivePacketState.ChannelServer),
    MOVE_DRAGON             (181, ReceivePacketState.ChannelServer),
    QUICK_SLOT_UPDATE       (183, ReceivePacketState.ChannelServer, IgnoredPacketEvent.class),
    MOVE_LIFE               (188, ReceivePacketState.ChannelServer),
    AUTO_AGGRO              (189, ReceivePacketState.ChannelServer),
    MOB_DAMAGE_MOB_FRIENDLY (192, ReceivePacketState.ChannelServer),
    MONSTER_BOMB            (193, ReceivePacketState.ChannelServer),
    MOB_DAMAGE_MOB          (194, ReceivePacketState.ChannelServer),
    NPC_ACTION              (197, ReceivePacketState.ChannelServer),
    ITEM_PICKUP             (202, ReceivePacketState.ChannelServer),
    DAMAGE_REACTOR          (205, ReceivePacketState.ChannelServer),
    TOUCHING_REACTOR        (206, ReceivePacketState.ChannelServer),
    FIELD_INITIALIZE        (207, ReceivePacketState.ChannelServer),
    SNOWBALL                (211, ReceivePacketState.ChannelServer),
    LEFT_KNOCKBACK          (212, ReceivePacketState.ChannelServer),
    COCONUT                 (213, ReceivePacketState.ChannelServer),
    MATCH_TABLE             (214, ReceivePacketState.ChannelServer),
    MONSTER_CARNIVAL        (218, ReceivePacketState.ChannelServer),
    PARTY_SEARCH_REGISTER   (220, ReceivePacketState.ChannelServer),
    PARTY_SEARCH_START      (222, ReceivePacketState.ChannelServer),
    PARTY_SEARCH_CANCEL     (223, ReceivePacketState.ChannelServer, IgnoredPacketEvent.class),
    CHECK_CASH              (228, ReceivePacketState.ChannelServer),
    CASHSHOP_OPERATION      (229, ReceivePacketState.ChannelServer),
    COUPON_CODE             (230, ReceivePacketState.ChannelServer),
    OPEN_ITEMUI             (235, ReceivePacketState.ChannelServer),
    CLOSE_ITEMUI            (236, ReceivePacketState.ChannelServer),
    USE_ITEMUI              (237, ReceivePacketState.ChannelServer),
    MTS_OPERATION           (253, ReceivePacketState.ChannelServer),
    USE_MAPLELIFE           (254, ReceivePacketState.ChannelServer),
    USE_HAMMER              (260, ReceivePacketState.ChannelServer),
    MAPLETV                 (65534, ReceivePacketState.ChannelServer);
    public final int value;
    public final ReceivePacketState packetState;
    public Class<? extends PacketEvent> clazz;
    // @formatter:on

    RecvOpcode(int value, ReceivePacketState packetState) {
        this(value, packetState, null);
    }

    RecvOpcode(int value, ReceivePacketState packetState, Class<? extends PacketEvent> clazz) {
        this.value = value;
        this.packetState = packetState;
        this.clazz = clazz;
    }

    public int getValue() {
        return value;
    }
}
