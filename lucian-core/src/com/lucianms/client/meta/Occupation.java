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
        if (this.level > 10) {
            this.level = 10;
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
            int needed = getType().getExperienceForLv(getLevel());
            this.experience += Math.min(experience, needed);
            if (this.experience >= needed) {
                setLevel((byte) (getLevel() + 1));
                return true;
            }
        }
        return false;
    }

    public enum Type {
        // @formatter:off
        Pharaoh(1,0),
        Undead(1,0),
        Demon(1,0),
        Human(1,0),

        Trainer(10, 500000),
        Troll(5, 5000),
        Farmer(3, 1000),
        Looter(5, 10000);
        // @formatter:on

        private int maxLevel;
        private int maxExperience;

        Type(int maxLevel, int maxExperience) {
            this.maxLevel = maxLevel;
            this.maxExperience = maxExperience;
        }

        public static Type fromValue(int n) {
            if (n == -1) return null;
            return Type.values()[n];
        }

        public int getExperienceForLv(int level) {
            return (maxExperience / maxLevel) * level;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public int getMaxExperience() {
            return maxExperience;
        }
    }
}
