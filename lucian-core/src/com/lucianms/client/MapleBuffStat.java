package com.lucianms.client;

/**
 * @author Kevin
 */
public enum MapleBuffStat {

    PAD(0),
    PDD(1),
    MAD(2),
    MDD(3),
    ACC(4),
    EVA(5),
    CRAFT(6),
    SPEED(7),
    JUMP(8),
    MAGIC_GUARD(9),
    DARK_SIGHT(10),
    BOOSTER(11),
    POWER_GUARD(12),
    MAX_HP(13),
    MAX_MP(14),
    INVINCIBLE(15),
    SOUL_ARROW(16),
    STUN(17),
    POISON(18),
    SEAL(19),
    DARKNESS(20),
    COMBO_COUNTER(21),
    WEAPON_CHARGE(22),
    DRAGON_BLOOD(23),
    HOLY_SYMBOL(24),
    MESO_UP(25),
    SHADOW_PARTNER(26),
    PICK_POCKET(27),
    MESO_GUARD(28),
    THAW(29),
    WEAKNESS(30),
    CURSE(31),
    SLOW(32),
    MORPH(33),
    REGEN(34),
    BASIC_STAT_UP(35),
    STANCE(36),
    SHARP_EYES(37),
    MANA_REFLECTION(38),
    ATTRACT(39),
    SPIRIT_JAVELIN(40),
    INFINITY(41),
    HOLY_SHIELD(42),
    HAMSTRING(43),
    BLIND(44),
    CONCENTRATE(45),
    BAN_MAP(46),
    MAX_LEVEL_BUFF(47),
    MESO_UP_BY_ITEM(48),
    GHOST(49),
    BARRIER(50),
    REVERSE_INPUT(51),
    ITEM_UP_BY_ITEM(52),
    RESPECT_P_IMMUNE(53),
    RESPECT_M_IMMUNE(54),
    DEFENSE_ATT(55),
    DEFENSE_STATE(56),
    INC_EFFECT_HP_POTION(57),
    INC_EFFECT_MP_POTION(58),
    DOJANG_BERSERK(59),
    DOJANG_INVINCIBLE(60),
    SPARK(61),
    DOJANG_SHIELD(62),
    SOUL_MASTER_FINAL(63),
    WIND_BREAKER_FINAL(64),
    ELEMENTAL_RESET(65),
    WIND_WALK(66),
    EVENT_RATE(67),
    COMBO_ABILITY_BUFF(68),
    COMBO_DRAIN(69),
    COMBO_BARRIER(70),
    BODY_PRESSURE(71),
    SMART_KNOCKBACK(72),
    REPEAT_EFFECT(73),
    EXP_BUFF_RATE(74),
    STOP_PORTION(75),
    STOP_MOTION(76),
    FEAR(77),
    FROZEN(78),
    ASSIST_CHARGE(79),
    ENRAGE(80),
    BEHOLDER(81),
    ENERGY_CHARGE(82),
    DASH_SPEED(83),
    DASH_JUMP(84),
    RIDE_VEHICLE(85),
    PARTY_BOOSTER(86),
    GUIDED_BULLET(87),
    ZOMBIFY(88);
    private final int i;
    private final int index;

    MapleBuffStat(int i) {
        this.i = 1 << (i % 32);
        this.index = (int) Math.floor(i / 32);
    }

    public int getValue() {
        return i;
    }

    public int getIndex() {
        return index;
    }
}