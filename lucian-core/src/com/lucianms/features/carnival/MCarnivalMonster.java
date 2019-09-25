package com.lucianms.features.carnival;

/**
 * @author izarooni
 */
public class MCarnivalMonster {

    private final int ID;
    private final int mobTime;
    private final int spendCP;

    public MCarnivalMonster(int ID, int mobTime, int spendCP) {
        this.ID = ID;
        this.mobTime = mobTime;
        this.spendCP = spendCP;
    }

    public int getID() {
        return ID;
    }

    public int getMobTime() {
        return mobTime;
    }

    public int getSpendCP() {
        return spendCP;
    }
}
