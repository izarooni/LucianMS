package com.lucianms.client.meta;

/**
 * @author izarooni
 */
public class Occupation {

    private final Type type;
    private byte level;
    private int experience;
    public Occupation(Type type) {
        this.type = type;
        this.level = 1;
    }

    public Type getType() {
        return type;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
        this.experience = 0;
        if (this.level > 15) {
            this.level = 15;
        }
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public boolean gainExperience(int experience) {
        if (getLevel() < getType().getMaxLevel()) {
            double needed = getType().getExperienceForLv(getLevel());
            this.experience += Math.min(experience, needed);
            if (this.experience >= needed) {
                experience -= needed;
                if (experience > 0) {
                    gainExperience(experience);
                }
                setLevel((byte) (getLevel() + 1));
                return true;
            }
        }
        return false;
    }

    public enum Type {
        // @formatter:off
        Grinder(15, 525),
        FM_WHORE(15, 525),
        Miser(15, 525),
        Hoarder(15, 525),
        Mediocrity(1, 1);
        // @formatter:on

        private int maxLevel;
        private int baseExperience;

        Type(int maxLevel, int baseExperience) {
            this.maxLevel = maxLevel;
            this.baseExperience = baseExperience;
        }

        public static Type fromValue(int n) {
            if (n == -1) return null;
            return Type.values()[n];
        }

        public int getExperienceForLv(int level) {
            return (int) (baseExperience * Math.pow(1.25, level - 1));
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public int getBaseExperience() {
            return baseExperience;
        }
    }
}
