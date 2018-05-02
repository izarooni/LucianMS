package com.lucianms.cquest.requirement;

/**
 * @author izarooni
 */
public abstract class CQuestRequirement {

    private boolean isFinished = false;

    /**
     * <p>
     * Prevents the quest completion notification from being spammed when incrementing
     * a quest requirement variable due to the quest not being completed, but is finished.
     * <p>
     * This boolean will be used to determine if all progress variables have already been checked and
     * meet their requirement.
     *
     * @return true if the quest is finished, false otherwise
     */
    public final boolean isFinished() {
        return isFinished;
    }

    /**
     * @param finished true or false if all progress variables are tested to be completed
     */
    public final void setFinished(boolean finished) {
        this.isFinished = finished;
    }
}
