package client.meta;

/**
 * metadata for achievements
 * <p>
 * possible information that could be recorded: monsters killed, quests completed, mesos gained, etc.
 * </p>
 *
 * @author izarooni
 */
public class Achievement {

    private boolean completed = false;
    private boolean casino1Completed = false;
    private boolean casino2Completed = false;
    private int monstersKilled = 0;

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
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
