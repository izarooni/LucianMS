package constants;

/**
 * @author The Spookster
 */
public enum EquipSlot {

    HAT("Cp", -1),
    SPECIAL_HAT("HrCp", -1),
    FACE_ACCESSORY("Af", -2),
    EYE_ACCESSORY("Ay", -3),
    EARRINGS("Ae", -4),
    TOP("Ma", -5),
    OVERALL("MaPn", -5),
    PANTS("Pn", -6),
    SHOES("So", -7),
    GLOVES("GlGw", -8),
    CASH_GLOVES("Gv", -8),
    CAPE("Sr", -9),
    SHIELD("Si", -10),
    WEAPON("Wp", -11),
    WEAPON_2("WpSi", -11),
    LOW_WEAPON("WpSp", -11),
    RING("Ri", -12, -13, -15, -16),
    PENDANT("Pe", -17),
    TAMED_MOB("Tm", -18),
    SADDLE("Sd", -19),
    MEDAL("Me", -49),
    BELT("Be", -50),
    PET_EQUIP;

    private String name;
    private int[] allowed;

    EquipSlot() {
    }

    EquipSlot(String wz, int... in) {
        name = wz;
        allowed = in;
    }

    public String getName() {
        return name;
    }

    public boolean isAllowed(int slot, boolean cash) {
        if (slot < 0) {
            if (allowed != null) {
                for (Integer allow : allowed) {
                    int condition = cash ? allow - 100 : allow;
                    if (slot == condition) {
                        return true;
                    }
                }
            }
        }
        return cash && slot < 0;
    }

    public static EquipSlot getFromTextSlot(String slot) {
        if (!slot.isEmpty()) {
            for (EquipSlot c : values()) {
                if (c.getName() != null) {
                    if (c.getName().equals(slot)) {
                        return c;
                    }
                }
            }
        }
        return PET_EQUIP;
    }

    public static EquipSlot getFromItemId(int itemId) {
        int a = itemId / 10000;
        if (a >= 130 && a <= 170) {
            return WEAPON;
        } else if (a == 100) {
            return HAT;
        } else if (a == 103) {
            return EARRINGS;
        } else if (a == 104) {
            return TOP;
        } else if (a == 105) {
            return OVERALL;
        } else if (a == 106) {
            return PANTS;
        } else if (a == 107) {
            return SHOES;
        } else if (a == 108) {
            return GLOVES;
        } else if (a == 109) {
            return SHIELD;
        } else if (a == 110) {
            return CAPE;
        } else if (a == 111) {
            return RING;
        } else if (a == 190) {
            return TAMED_MOB;
        }
        return null;
    }
}
