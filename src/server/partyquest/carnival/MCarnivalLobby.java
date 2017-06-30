package server.partyquest.carnival;

import net.server.world.MapleParty;
import scheduler.Task;
import scheduler.TaskExecutor;
import tools.Randomizer;

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

    private final int maxPartySize;
    private final int battlefieldMapId;

    private State state = State.Available;

    private MapleParty party1 = null;
    private MapleParty party2 = null;

    private Task waitingTask;

    public MCarnivalLobby(int maxPartySize, int battlefieldMapId) {
        this.maxPartySize = maxPartySize;
        this.battlefieldMapId = battlefieldMapId;
    }

    public int getMaxPartySize() {
        return maxPartySize;
    }

    public int getBattlefieldMapId() {
        return battlefieldMapId;
    }

    public Task getWaitingTask() {
        return waitingTask;
    }

    public void setWaitingTask(Task waitingTask) {
        this.waitingTask = waitingTask;
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
                break;
            }
            case Starting: {
                if (waitingTask != null) {
                    waitingTask.cancel();
                }
                waitingTask = TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        setState(State.InProgress);
                    }
                }, 5000);
                break;
            }
            case Waiting: {
                waitingTask = TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        setState(State.Available);
                    }
                }, 60000 * 5); // 5 minutes
                break;
            }
            case Available: {
                if (waitingTask != null) {
                    waitingTask.cancel();
                    waitingTask = null;
                }
                int hubMapId = 980000000;
                if (party1 != null) {
                    party1.getMembers().forEach(m -> m.getPlayer().changeMap(hubMapId));
                    party1 = null;
                }
                if (party2 != null) {
                    party2.getMembers().forEach(m -> m.getPlayer().changeMap(hubMapId));
                    party2 = null;
                }
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

    public void removeParty(MapleParty party) {
        if (party1 == party) {
            party1 = null;
        } else if (party2 == party) {
            party2 = null;
        }
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
        MCarnivalGame carnivalGame = new MCarnivalGame(this);
        if (Randomizer.nextInt(1) == 0) {
            carnivalGame.setTeamRed(new MCarnivalTeam(0, party1));
            carnivalGame.setTeamBlue(new MCarnivalTeam(1, party2));
        } else {
            carnivalGame.setTeamRed(new MCarnivalTeam(0, party2));
            carnivalGame.setTeamBlue(new MCarnivalTeam(1, party1));
        }
        return carnivalGame;
    }
}
