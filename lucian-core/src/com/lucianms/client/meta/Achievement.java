package com.lucianms.client.meta;

import com.lucianms.client.MapleCharacter;
import com.lucianms.io.scripting.Achievements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.CompiledScript;
import javax.script.Invocable;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(Achievement.class);

    private final String name;
    private Status status;
    private boolean casino1Completed;
    private boolean casino2Completed;
    private int monstersKilled;

    public Achievement(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = name;
        status = Status.Incomplete;
    }

    public String getDescription(MapleCharacter player) {
        CompiledScript script = Achievements.getAchievements().get(name);
        try {
            return (String) ((Invocable) script.getEngine()).invokeFunction("getDescription", player);
        } catch (NoSuchMethodException ignore) {
        } catch (Exception e) {
            LOGGER.error("Failed to get description for '{}'", name, e);
        }
        return null;
    }

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
