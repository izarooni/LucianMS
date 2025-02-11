/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lucianms.nio;

public enum SendOpcode {

    //@formatter:off
    LOGIN_STATUS                               (0),
    GUEST_ID_LOGIN                             (1),
    ACCOUNT_INFO                               (2),
    SERVERSTATUS                               (3),
    GENDER_DONE                                (4),
    CONFIRM_EULA_RESULT                        (5),
    CHECK_PINCODE                              (6),
    UPDATE_PINCODE                             (7),
    VIEW_ALL_CHAR                              (8),
    SELECT_CHARACTER_BY_VAC                    (9),
    SERVERLIST                                 (10),
    CHARLIST                                   (11),
    SERVER_IP                                  (12),
    CHAR_NAME_RESPONSE                         (13),
    ADD_NEW_CHAR_ENTRY                         (14),
    DELETE_CHAR_RESPONSE                       (15),
    CHANGE_CHANNEL                             (16),
    PING                                       (17),
    KOREAN_INTERNET_CAFE_SHIT                  (18),
    CHANNEL_SELECTED                           (20),
    HACKSHIELD_REQUEST                         (21),
    RELOG_RESPONSE                             (22),
    CHECK_CRC_RESULT                           (25),
    LAST_CONNECTED_WORLD                       (26),
    RECOMMENDED_WORLD_MESSAGE                  (27),
    CHECK_SPW_RESULT                           (28),
    INVENTORY_OPERATION                        (29),
    INVENTORY_GROW                             (30),
    STAT_CHANGED                               (31),
    GIVE_BUFF                                  (32),
    CANCEL_BUFF                                (33),
    FORCED_STAT_SET                            (34),
    FORCED_STAT_RESET                          (35),
    UPDATE_SKILLS                              (36),
    SKILL_USE_RESULT                           (37),
    FAME_RESPONSE                              (38),
    SHOW_STATUS_INFO                           (39),
    OPEN_FULL_CLIENT_DOWNLOAD_LINK             (40),
    MEMO_RESULT                                (41),
    MAP_TRANSFER_RESULT                        (42),
    ANTI_MACRO_RESULT                          (43),
    CLAIM_RESULT                               (45),
    CLAIM_AVAILABLE_TIME                       (46),
    CLAIM_STATUS_CHANGED                       (47),
    SET_TAMING_MOB_INFO                        (48),
    QUEST_CLEAR                                (49),
    ENTRUSTED_SHOP_CHECK_RESULT                (50),
    SKILL_LEARN_ITEM_RESULT                    (51),
    GATHER_ITEM_RESULT                         (52),
    SORT_ITEM_RESULT                           (53),
    SUE_CHARACTER_RESULT                       (55),
    TRADE_MONEY_LIMIT                          (57),
    SET_GENDER                                 (58),
    GUILD_BBS_PACKET                           (59),
    CHAR_INFO                                  (61),
    PARTY_OPERATION                            (62),
    BUDDYLIST                                  (63),
    GUILD_OPERATION                            (65),
    ALLIANCE_OPERATION                         (66),
    SPAWN_PORTAL                               (67),
    SERVERMESSAGE                              (68),
    INCUBATOR_RESULT                           (69),
    SHOP_SCANNER_RESULT                        (70),
    SHOP_LINK_RESULT                           (71),
    MARRIAGE_REQUEST                           (72),
    MARRIAGE_RESULT                            (73),
    WEDDING_GIFT_RESULT                        (74),
    NOTIFY_MARRIED_PARTNER_MAP_TRANSFER        (75),
    CASH_PET_FOOD_RESULT                       (76),
    SET_WEEK_EVENT_MESSAGE                     (77),
    SET_POTION_DISCOUNT_RATE                   (78),
    BRIDLE_MOB_CATCH_FAIL                      (79),
    IMITATED_NPC_RESULT                        (80),
    IMITATED_NPC_DATA                          (81),
    LIMITED_NPC_DISABLE_INFO                   (82),
    MONSTER_BOOK_SET_CARD                      (83),
    MONSTER_BOOK_SET_COVER                     (84),
    HOUR_CHANGED                               (85),
    MINIMAP_ON_OFF                             (86),
    CONSULT_AUTHKEY_UPDATE                     (87),
    CLASS_COMPETITION_AUTHKEY_UPDATE           (88),
    WEB_BOARD_AUTHKEY_UPDATE                   (89),
    SESSION_VALUE                              (90),
    PARTY_VALUE                                (91),
    FIELD_SET_VARIABLE                         (92),
    BONUS_EXP_CHANGED                          (93),
    FAMILY_CHART_RESULT                        (94),
    FAMILY_INFO_RESULT                         (95),
    FAMILY_RESULT                              (96),
    FAMILY_JOIN_REQUEST                        (97),
    FAMILY_JOIN_REQUEST_RESULT                 (98),
    FAMILY_JOIN_ACCEPTED                       (99),
    FAMILY_PRIVILEGE_LIST                      (100),
    FAMILY_FAMOUS_POINT_INC_RESULT             (101),
    FAMILY_NOTIFY_LOGIN_OR_LOGOUT              (102),
    FAMILY_SET_PRIVILEGE                       (103),
    FAMILY_SUMMON_REQUEST                      (104),
    NOTIFY_LEVELUP                             (105),
    NOTIFY_MARRIAGE                            (106),
    NOTIFY_JOB_CHANGE                          (107),
    MAPLE_TV_USE_RES                           (109),
    AVATAR_MEGAPHONE_RESULT                    (110),
    SET_AVATAR_MEGAPHONE                       (111),
    CLEAR_AVATAR_MEGAPHONE                     (112),
    CANCEL_NAME_CHANGE_RESULT                  (113),
    CANCEL_TRANSFER_WORLD_RESULT               (114),
    DESTROY_SHOP_RESULT                        (115),
    FAKE_GM_NOTICE                             (116),
    SUCCESS_IN_USE_GACHAPON_BOX                (117),
    NEW_YEAR_CARD_RES                          (118),
    RANDOM_MORPH_RES                           (119),
    CANCEL_NAME_CHANGE_BY_OTHER                (120),
    SET_BUY_EQUIP_EXT                          (121),
    SCRIPT_PROGRESS_MESSAGE                    (122),
    DATA_CRC_CHECK_FAILED                      (123),
    MACRO_SYS_DATA_INIT                        (124),
    SET_FIELD                                  (125),
    SET_ITC                                    (126),
    SET_CASH_SHOP                              (127),
    SET_BACK_EFFECT                            (128),
    SET_MAP_OBJECT_VISIBLE                     (129),
    CLEAR_BACK_EFFECT                          (130),
    BLOCKED_MAP                                (131),
    BLOCKED_SERVER                             (132),
    FIELD_SPECIFIC_DATA                        (133),
    MULTICHAT                                  (134),
    WHISPER                                    (135),
    SPOUSE_CHAT                                (136),
    SUMMON_ITEM_INAVAILABLE                    (137),
    FIELD_EFFECT                               (138),
    FIELD_OBSTACLE_ONOFF                       (139),
    FIELD_OBSTACLE_ONOFF_STATUS                (140),
    FIELD_OBSTACLE_ALL_RESET                   (141),
    BLOW_WEATHER                               (142),
    PLAY_JUKEBOX                               (143),
    ADMIN_RESULT                               (144),
    OX_QUIZ                                    (145),
    GMEVENT_INSTRUCTIONS                       (146),
    CLOCK                                      (147),
    CONTI_MOVE                                 (148),
    CONTI_STATE                                (149),
    SET_QUEST_CLEAR                            (150),
    SET_QUEST_TIME                             (151),
    WARN_MESSAGE                               (152),
    SET_OBJECT_STATE                           (153),
    STOP_CLOCK                                 (154),
    ARIANT_ARENA_SHOW_RESULT                   (155),
    MASSACRE_GAUGE_INC                         (157),
    MASSACRE_RESULT                            (158),
    //region CUserPool::OnUserCommonPacket
    SPAWN_PLAYER                               (160), // CUserPool::OnUserEnterField
    REMOVE_PLAYER_FROM_MAP                     (161), // CUserPool::OnUserLeaveField
    CHATTEXT                                   (162),
    CHATTEXT1                                  (163),
    CHALKBOARD                                 (164), // CUser::OnADBoard
    UPDATE_CHAR_BOX                            (165), // CUser::OnMiniRoomBalloon
    SHOW_CONSUME_EFFECT                        (166), // CUser::SetConsumeItemEffect_0
    SHOW_SCROLL_EFFECT                         (167), // CUser::ShowItemUpgradeEffect
    //region CUser::OnPetPacket
    SPAWN_PET                                  (168),
    MOVE_PET                                   (170),
    PET_CHAT                                   (171),
    PET_NAMECHANGE                             (172),
    PET_SHOW                                   (173),
    PET_COMMAND                                (174),
    //endregion
    //region CSummonedPool::OnPacket
    SUMMONED_ENTER_FIELD                       (175),
    SUMMONED_LEAVE_FIELD                       (176),
    SUMMONED_MOVE                              (177),
    SUMMONED_ATTACK                            (178),
    SUMMONED_HIT                               (179),
    SUMMONED_SKILL                             (180),
    //endregion
    //region CUser::OnDragonPacket
    SPAWN_DRAGON                               (181),
    MOVE_DRAGON                                (182),
    REMOVE_DRAGON                              (183),
    //endregion
    //region CUserPool::OnUserRemotePacket
    MOVE_PLAYER                                (185),
    CLOSE_RANGE_ATTACK                         (186),
    RANGED_ATTACK                              (187),
    MAGIC_ATTACK                               (188),
    ENERGY_ATTACK                              (189),
    SKILL_EFFECT                               (190),
    CANCEL_SKILL_EFFECT                        (191),
    DAMAGE_PLAYER                              (192),
    FACIAL_EXPRESSION                          (193),
    SHOW_ITEM_EFFECT                           (194),
    SHOW_CHAIR                                 (196),
    UPDATE_CHAR_LOOK                           (197),
    SHOW_FOREIGN_EFFECT                        (198),
    GIVE_FOREIGN_BUFF                          (199),
    CANCEL_FOREIGN_BUFF                        (200),
    UPDATE_PARTYMEMBER_HP                      (201),
    //endregion
    //region CUserPool::OnUserLocalPacket
    CANCEL_CHAIR                               (205),
    SHOW_ITEM_GAIN_INCHAT                      (206),
    DOJO_WARP_UP                               (207),
    LUCKSACK_PASS                              (208),
    LUCKSACK_FAIL                              (209),
    MESO_BAG_MESSAGE                           (210),
    UPDATE_QUEST_INFO                          (211),
    PLAYER_HINT                                (214),
    KOREAN_EVENT                               (219),
    OPEN_UI                                    (220),
    LOCK_UI                                    (221),
    DISABLE_UI                                 (222),
    SPAWN_GUIDE                                (223),
    TALK_GUIDE                                 (224),
    SHOW_COMBO                                 (225),
    COOLDOWN                                   (234),
    //endregion
    //endregion
    //region CMobPool::OnPacket
    SPAWN_MONSTER                              (236), // CMobPool::OnMobEnterField
    KILL_MONSTER                               (237), // CMobPool::OnMobLeaveField
    SPAWN_MONSTER_CONTROL                      (238), // CMobPool::OnMobChangeController
    //region CMobPool::OnMobPacket
    MOVE_MONSTER                               (239), // CMob::OnMove
    MOVE_MONSTER_RESPONSE                      (240), // CMob::OnCtrlAck
    APPLY_MONSTER_STATUS                       (242), // CMob::OnStatSet
    CANCEL_MONSTER_STATUS                      (243), // CMob::OnStatReset
    RESET_MONSTER_ANIMATION                    (244), // CMob::OnSuspendReset
    DAMAGE_MONSTER                             (246), // CMob::OnDamaged
    ARIANT_THING                               (249), // CMobPool::OnMobCrcKeyChanged
    SHOW_MONSTER_HP                            (250), // CMob::OnHPIndicator
    SHOW_DRAGGED                               (251), // CMob::OnCatchEffect
    CATCH_MONSTER                              (252), // CMob::OnEffectByItem
    SHOW_MAGNET                                (253), // CMob::OnMobSpeaking
    //endregion
    //endregion
    SPAWN_NPC                                  (257),
    REMOVE_NPC                                 (258),
    SPAWN_NPC_REQUEST_CONTROLLER               (259),
    NPC_ACTION                                 (260),
    SPAWN_HIRED_MERCHANT                       (265),
    DESTROY_HIRED_MERCHANT                     (266),
    UPDATE_HIRED_MERCHANT                      (267),
    DROP_ITEM_FROM_MAPOBJECT                   (268),
    REMOVE_ITEM_FROM_MAP                       (269),
    KITE_MESSAGE                               (270),
    KITE                                       (271),
    SPAWN_MIST                                 (273),
    REMOVE_MIST                                (274),
    SPAWN_DOOR                                 (275),
    REMOVE_DOOR                                (276),
    REACTOR_HIT                                (277),
    REACTOR_SPAWN                              (279),
    REACTOR_DESTROY                            (280),
    SNOWBALL_STATE                             (281),
    SNOWBALL_HIT                               (282),
    SNOWBALL_MESSAGE                           (283),
    SNOWBALL_TOUCH                             (284),
    COCONUT_HIT                                (285),
    COCONUT_SCORE                              (286),
    GUILD_BOSS_HEALER_MOVE                     (287),
    GUILD_BOSS_PULLEY_STATE_CHANGE             (288),
    MONSTER_CARNIVAL_START                     (289),
    MONSTER_CARNIVAL_OBTAINED_CP               (290),
    MONSTER_CARNIVAL_PARTY_CP                  (291),
    MONSTER_CARNIVAL_SUMMON                    (292),
    MONSTER_CARNIVAL_MESSAGE                   (293),
    MONSTER_CARNIVAL_DIED                      (294),
    MONSTER_CARNIVAL_LEAVE                     (295),
    ARIANT_ARENA_USER_SCORE                    (297),
    SHEEP_RANCH_INFO                           (299),
    SHEEP_RANCH_CLOTHES                        (300),
    ARIANT_SCORE                               (301),
    HORNTAIL_CAVE                              (302),
    ZAKUM_SHRINE                               (303),
    NPC_TALK                                   (304),
    OPEN_NPC_SHOP                              (305),
    CONFIRM_SHOP_TRANSACTION                   (306),
    ADMIN_SHOP_MESSAGE                         (307),
    ADMIN_SHOP                                 (308),
    STORAGE                                    (309),
    FREDRICK_MESSAGE                           (310),
    FREDRICK                                   (311),
    RPS_GAME                                   (312),
    MESSENGER                                  (313),
    PLAYER_INTERACTION                         (314),
    TOURNAMENT                                 (315),
    TOURNAMENT_MATCH_TABLE                     (316),
    TOURNAMENT_SET_PRIZE                       (317),
    TOURNAMENT_UEW                             (318),
    TOURNAMENT_CHARACTERS                      (319),
    WEDDING_PROGRESS                           (320),
    WEDDING_CEREMONY_END                       (321),
    PARCEL                                     (322),
    CHARGE_PARAM_RESULT                        (323),
    QUERY_CASH_RESULT                          (324),
    CASHSHOP_OPERATION                         (325),
    KEYMAP                                     (335),
    AUTO_HP_POT                                (336),
    AUTO_MP_POT                                (337),
    MAPLE_TV_SET_MESSAGE                       (341),
    MAPLE_TV_CLEAR_MESSAGE                     (342),
    MAPLE_TV_SEND_MESSAGE                      (343),
    MTS_OPERATION2                             (347),
    MTS_OPERATION                              (348),
    VICIOUS_HAMMER                             (354);
    //@formatter:on
    private int code;

    SendOpcode(int code) {
        this.code = code;
    }

    public int getValue() {
        return code;
    }
}
