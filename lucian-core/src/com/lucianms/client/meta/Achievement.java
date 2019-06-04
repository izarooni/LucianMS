package com.lucianms.client.meta;

/**
 * metadata for achievements
 * <p>
 * possible information that could be recorded: monsters killed, quests completed, mesos gained, etc.
 * </p>
 *
 * @author izarooni
 */
public class Achievement {

    public enum Status {
        Incomplete, Complete, RewardGiven
    }

    private Status status;
    private boolean casino1Completed;
    private boolean casino2Completed;
    private int monstersKilled;

    public Status getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = Status.values()[status];
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isCasino1Completed() {
        return casino1Completed;
    }

    public void setCasino1Completed(boolean casino1Completed) {
        this.casino1Completed = casino1Completed;
    }

    public boolean isCasino2Completed() {
        return casino2Completed;
    }

    public void setCasino2Completed(boolean casino2Completed) {
        this.casino2Completed = casino2Completed;
    }

    public int getMonstersKilled() {
        return monstersKilled;
    }

    public void setMonstersKilled(int monstersKilled) {
        this.monstersKilled = monstersKilled;
    }
}
