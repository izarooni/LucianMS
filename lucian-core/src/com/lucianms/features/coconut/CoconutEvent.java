package com.lucianms.features.coconut;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ServerConstants;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author izarooni
 */
public class CoconutEvent {

    private int falling, bombing, stopped;
    private int hit;
    private int timeDefault, timeExpand;
    private int timeFinish;
    private int redPoints, bluePoints; // teams
    private long beginTimestamp;
    private String effectWin, effectLose;
    private String soundWin, soundLose;
    private ArrayList<CoconutObject> coconuts;
    private Task timeout;

    public void begin(MapleMap map) {
        timeout = TaskExecutor.cancelTask(timeout);
        shuffleCoconuts();

        // spawn coconuts
        List<Pair<String, Integer>> collect = getCoconuts().stream().map(c -> new Pair<>(c.getName(), 0)).collect(Collectors.toList());
        map.sendPacket(MaplePacketCreator.setObjectState(collect));
        collect.clear();

        beginTimestamp = System.currentTimeMillis();
        map.sendPacket(MaplePacketCreator.getClock(getTimeDefault()));
        timeout = TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                if (redPoints != bluePoints) {
                    boolean redWinner = redPoints > bluePoints;
                    boolean blueLoser = bluePoints < redPoints;

                    List<MapleCharacter> winners = map.getPlayers(p -> redWinner ? p.getTeam() == 0 : p.getTeam() == 1);
                    for (MapleCharacter winner : winners) {
                        winner.addPoints("ep", 1);
                        winner.sendMessage(6, "You have gained 1 Event Point for winning the Coconut event");
                    }
                    winners.clear();

                    map.sendPacketIf(MaplePacketCreator.showEffect(effectWin), p -> p.getTeam() == 0 && redWinner);
                    map.sendPacketIf(MaplePacketCreator.playSound(soundWin), p -> p.getTeam() == 0 && redWinner);
                    map.sendPacketIf(MaplePacketCreator.showEffect(effectLose), p -> p.getTeam() == 1 && blueLoser);
                    map.sendPacketIf(MaplePacketCreator.playSound(soundLose), p -> p.getTeam() == 1 && blueLoser);
                } else { // everybody is a winner?
                    map.sendPacket(MaplePacketCreator.showEffect(effectWin));
                    map.sendPacket(MaplePacketCreator.playSound(soundWin));
                    Collection<MapleCharacter> players = map.getPlayers();
                    for (MapleCharacter player : players) {
                        player.addPoints("ep", 1);
                        player.sendMessage(6, "You have gained 1 Event Point for winning the Coconut event");
                    }
                    players.clear();
                }
                map.sendPacket(MaplePacketCreator.getClock(getTimeFinish()));
                TaskExecutor.createTask(() -> map.warpEveryone(ServerConstants.MAPS.Home), getTimeFinish() * 1000);
            }
        }, getTimeDefault() * 1000);
    }

    /**
     * shuffle the coconuts to randomize their results (i.e. fall, explode and stop)
     */
    public void shuffleCoconuts() {
        // don't modify the original array
        ArrayList<CoconutObject> temp = new ArrayList<>(coconuts);
        Collections.shuffle(temp);
        for (CoconutObject coconut : temp) {
            coconut.setState((byte) 0);
            coconut.setCanHit(true);

            if (bombing > 0 || stopped > 0) {
                boolean bomb = Randomizer.nextBoolean() && bombing > 0;
                coconut.setResult((byte) (bomb ? 1 : 2));
                if (bomb) bombing--;
                else stopped--;
            } else {
                coconut.setResult((byte) 3);
            }
        }
        temp.clear();
    }

    public int getFalling() {
        return falling;
    }

    public void setFalling(int falling) {
        this.falling = falling;
    }

    public int getBombing() {
        return bombing;
    }

    public void setBombing(int bombing) {
        this.bombing = bombing;
    }

    public int getStopped() {
        return stopped;
    }

    public void setStopped(int stopped) {
        this.stopped = stopped;
    }

    public int getHit() {
        return hit;
    }

    public void setHit(int hit) {
        this.hit = hit;
    }

    public int getTimeDefault() {
        return timeDefault;
    }

    public void setTimeDefault(int timeDefault) {
        this.timeDefault = timeDefault;
    }

    public int getTimeExpand() {
        return timeExpand;
    }

    public void setTimeExpand(int timeExpand) {
        this.timeExpand = timeExpand;
    }

    public int getTimeFinish() {
        return timeFinish;
    }

    public void setTimeFinish(int timeFinish) {
        this.timeFinish = timeFinish;
    }

    public int getRedPoints() {
        return redPoints;
    }

    public void setRedPoints(int redPoints) {
        this.redPoints = redPoints;
    }

    public int getBluePoints() {
        return bluePoints;
    }

    public void setBluePoints(int bluePoints) {
        this.bluePoints = bluePoints;
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public void setBeginTimestamp(long beginTimestamp) {
        this.beginTimestamp = beginTimestamp;
    }

    public String getEffectWin() {
        return effectWin;
    }

    public void setEffectWin(String effectWin) {
        this.effectWin = effectWin;
    }

    public String getEffectLose() {
        return effectLose;
    }

    public void setEffectLose(String effectLose) {
        this.effectLose = effectLose;
    }

    public String getSoundWin() {
        return soundWin;
    }

    public void setSoundWin(String soundWin) {
        this.soundWin = soundWin;
    }

    public String getSoundLose() {
        return soundLose;
    }

    public void setSoundLose(String soundLose) {
        this.soundLose = soundLose;
    }

    public ArrayList<CoconutObject> getCoconuts() {
        return coconuts;
    }

    public void setCoconuts(ArrayList<CoconutObject> coconuts) {
        this.coconuts = coconuts;
    }

}
