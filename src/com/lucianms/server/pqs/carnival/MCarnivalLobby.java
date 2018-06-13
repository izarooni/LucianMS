package com.lucianms.server.pqs.carnival;

import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import net.server.channel.Channel;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.util.Optional;

/**
 * @author izarooni
 */
public class MCarnivalLobby {

    public enum State {
        Available, // Lobby is empty
        Waiting,  // Lobby is waiting for opponents
        Starting, // Lobby is about to begin
        InProgress // Game started
    }

    private static final int M_Office = 980000000;

    private final Channel channel;
    private final int maxPartySize;
    private final int mapId;

    private MCarnivalGame carnivalGame = null;
    private State state = State.Available;

    private MapleParty party1 = null;
    private MapleParty party2 = null;

    private Task waitingTask;

    public MCarnivalLobby(Channel channel, int maxPartySize, int mapId) {
        this.channel = channel;
        this.maxPartySize = maxPartySize;
        this.mapId = mapId;
    }

    private void broadcastPacket(byte[] packet) {
        if (party1 != null) {
            party1.getMembers().stream().filter(MaplePartyCharacter::isOnline).map(MaplePartyCharacter::getPlayer).forEach(p -> p.announce(packet));
        }
        if (party2 != null) {
            party2.getMembers().stream().filter(MaplePartyCharacter::isOnline).map(MaplePartyCharacter::getPlayer).forEach(p -> p.announce(packet));
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public int getMaxPartySize() {
        return maxPartySize;
    }

    public int getMapId() {
        return mapId;
    }

    public int getBattlefieldMapId() {
        return getMapId() + 1;
    }

    public int getVictoryMapId() {
        return getMapId() + 3;
    }

    public int getDefeatedMapId() {
        return getMapId() + 4;
    }

    public Task getWaitingTask() {
        return waitingTask;
    }

    public void setWaitingTask(Task waitingTask) {
        this.waitingTask = waitingTask;
    }

    public MCarnivalGame getGame() {
        return carnivalGame;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
        switch (this.state) {
            case InProgress: {
                if (waitingTask != null) {
                    waitingTask.cancel();
                }
                MCarnivalGame carnivalGame = createGame();
                party1.getMembers().forEach(p -> carnivalGame.registerPlayer(p.getPlayer()));
                party2.getMembers().forEach(p -> carnivalGame.registerPlayer(p.getPlayer()));
                waitingTask = TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        party1.getMembers().forEach(p -> carnivalGame.unregisterPlayer(p.getPlayer()));
                        party2.getMembers().forEach(p -> carnivalGame.unregisterPlayer(p.getPlayer()));
                        setState(State.Available);
                    }
                }, 1000 * 60 * 10); // 10 min game
                break;
            }
            case Starting: {
                waitingTask = TaskExecutor.cancelTask(waitingTask);
                broadcastPacket(MaplePacketCreator.getClock(10));
                // party 2 clock via npc
                waitingTask = TaskExecutor.createTask(() -> setState(State.InProgress), 10000);
                break;
            }
            case Waiting: {
                waitingTask = TaskExecutor.cancelTask(waitingTask);
                broadcastPacket(MaplePacketCreator.getClock(300));
                waitingTask = TaskExecutor.createTask(() -> setState(State.Available), 60000 * 5); // 5 minutes
                break;
            }
            case Available: {
                channel.removeMap(getBattlefieldMapId()); // resets
                waitingTask = TaskExecutor.cancelTask(waitingTask);
                Optional.ofNullable(carnivalGame).ifPresent(MCarnivalGame::dispose);
                break;
            }
        }
    }

    /**
     * @param party the party to add to the lobby
     * @return trye if both party slots in the lobby are filled, false otherwise
     */
    public boolean joiningParty(MapleParty party) {
        if (party1 == null) {
            party1 = party;
        } else {
            party2 = party;
        }
        return party1 != null && party2 != null;
    }

    public boolean removeParty(MapleParty party) {
        if (party1.getId() == party.getId()) {
            party1 = null;
        } else if (party2.getId() == party.getId()) {
            party2 = null;
        }
        return party1 == null && party2 == null;
    }

    /**
     * First entering party must have at least 1 party member but may not exceed maximum party size
     * <p>
     * Second entering party must have member count equal to the first entered party
     * </p>
     *
     * @param party the party attempt to enter the lobby
     * @return true if the party may enter, false otherwise
     */
    public boolean canEnter(MapleParty party) {
        if (party1 == null && party2 == null) {
            return party.getMembers().size() <= maxPartySize;
        }
        MapleParty nn = party1 == null ? party2 : party1;
        return nn.getMembers().size() == party.getMembers().size();
    }

    private MCarnivalGame createGame() {
        carnivalGame = new MCarnivalGame(this);
        boolean red = Randomizer.nextBoolean();
        carnivalGame.setTeamRed(new MCarnivalTeam(0, red ? party1 : party2));
        carnivalGame.setTeamBlue(new MCarnivalTeam(1, red ? party2 : party1));
        return carnivalGame;
    }
}
