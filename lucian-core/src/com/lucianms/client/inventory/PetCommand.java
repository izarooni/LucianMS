package com.lucianms.client.inventory;

/**
 * @author izarooni
 */
public class PetCommand {

    private final int ID;
    private final int increase;
    private final int levelLower;
    private final int levelUpper;
    private final int probability;

    public PetCommand(int ID, int increase, int levelLower, int levelUpper, int probability) {
        this.ID = ID;
        this.increase = increase;
        this.levelLower = levelLower;
        this.levelUpper = levelUpper;
        this.probability = probability;
    }

    @Override
    public String toString() {
        return String.format("PetCommand{ID=%d}", ID);
    }

    /**
     * the command ID as represented in WZ
     */
    public int getID() {
        return ID;
    }

    /**
     * @return Closeness increase value should the command succeed
     */
    public int getIncrease() {
        return increase;
    }

    /**
     * @return Lower bound value of level requirement to use the command
     */
    public int getLevelLower() {
        return levelLower;
    }

    /**
     * @return Upper bound value of level requirement ot use the command
     */
    public int getLevelUpper() {
        return levelUpper;
    }

    /**
     * @return Out of 100 percentage chance the command should succeed
     */
    public int getProbability() {
        return probability;
    }
}
