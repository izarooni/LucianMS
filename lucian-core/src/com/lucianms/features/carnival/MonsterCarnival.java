package com.lucianms.features.carnival;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author izarooni
 */
public class MonsterCarnival {

    private int deathCP;
    private int guardianGenMax;
    private int monsterGenMax;
    private int reactorBlue, reactorRed;
    private int winnerFieldID, loserFieldID;
    private int timeDefault;
    private int timeFinish;
    private boolean mapDivded;
    private String effectLose, effectWin;
    private String soundLose, soundWin;
    private ArrayList<MCarnivalGuardian> guardians;
    private ArrayList<MCarnivalMonster> monsters;
    private ArrayList<MCarnivalMonsterLocation> monsterGenLocations;

    public int getDeathCP() {
        return deathCP;
    }

    public void setDeathCP(int deathCP) {
        this.deathCP = deathCP;
    }

    public int getGuardianGenMax() {
        return guardianGenMax;
    }

    public void setGuardianGenMax(int guardianGenMax) {
        this.guardianGenMax = guardianGenMax;
    }

    public int getMonsterGenMax() {
        return monsterGenMax;
    }

    public void setMonsterGenMax(int monsterGenMax) {
        this.monsterGenMax = monsterGenMax;
    }

    public int getReactorBlue() {
        return reactorBlue;
    }

    public void setReactorBlue(int reactorBlue) {
        this.reactorBlue = reactorBlue;
    }

    public int getReactorRed() {
        return reactorRed;
    }

    public void setReactorRed(int reactorRed) {
        this.reactorRed = reactorRed;
    }

    public int getWinnerFieldID() {
        return winnerFieldID;
    }

    public void setWinnerFieldID(int winnerFieldID) {
        this.winnerFieldID = winnerFieldID;
    }

    public int getLoserFieldID() {
        return loserFieldID;
    }

    public void setLoserFieldID(int loserFieldID) {
        this.loserFieldID = loserFieldID;
    }

    public int getTimeDefault() {
        return timeDefault;
    }

    public void setTimeDefault(int timeDefault) {
        this.timeDefault = timeDefault;
    }

    public int getTimeFinish() {
        return timeFinish;
    }

    public void setTimeFinish(int timeFinish) {
        this.timeFinish = timeFinish;
    }

    public boolean isMapDivded() {
        return mapDivded;
    }

    public void setMapDivded(boolean mapDivded) {
        this.mapDivded = mapDivded;
    }

    public String getEffectLose() {
        return effectLose;
    }

    public void setEffectLose(String effectLose) {
        this.effectLose = effectLose;
    }

    public String getEffectWin() {
        return effectWin;
    }

    public void setEffectWin(String effectWin) {
        this.effectWin = effectWin;
    }

    public String getSoundLose() {
        return soundLose;
    }

    public void setSoundLose(String soundLose) {
        this.soundLose = soundLose;
    }

    public String getSoundWin() {
        return soundWin;
    }

    public void setSoundWin(String soundWin) {
        this.soundWin = soundWin;
    }

    public ArrayList<MCarnivalGuardian> getGuardians() {
        Collections.shuffle(guardians);
        return guardians;
    }

    public void setGuardians(ArrayList<MCarnivalGuardian> guardians) {
        this.guardians = guardians;
    }

    public ArrayList<MCarnivalMonster> getMonsters() {
        return monsters;
    }

    public void setMonsters(ArrayList<MCarnivalMonster> monsters) {
        this.monsters = monsters;
    }

    public ArrayList<MCarnivalMonsterLocation> getMonsterGenLocations() {
        Collections.shuffle(monsterGenLocations);
        return monsterGenLocations;
    }

    public void setMonsterGenLocations(ArrayList<MCarnivalMonsterLocation> monsterGenLocations) {
        this.monsterGenLocations = monsterGenLocations;
    }
}
