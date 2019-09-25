package com.lucianms.client;

/**
 * @author izarooni
 */
public class SkillEntry {

    public int masterLevel;
    public byte level;
    public long expiration;

    public SkillEntry(byte level, int masterLevel, long expiration) {
        this.level = level;
        this.masterLevel = masterLevel;
        this.expiration = expiration;
    }

    @Override
    public String toString() {
        return String.format("SkillEntry{masterLevel=%d, level=%s, expiration=%d}", masterLevel, level, expiration);
    }

    public int getMasterLevel() {
        return masterLevel;
    }

    public void setMasterLevel(int masterLevel) {
        this.masterLevel = masterLevel;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
