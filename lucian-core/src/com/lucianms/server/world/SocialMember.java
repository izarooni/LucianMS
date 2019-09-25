package com.lucianms.server.world;

import com.lucianms.client.MapleCharacter;
import tools.Disposable;

/**
 * @author izarooni
 */
public class SocialMember implements Disposable {

    private MapleCharacter player;

    //region for offline use
    private int playerID;
    private String username;
    private int channelID;
    //endregion

    public SocialMember(MapleCharacter player) {
        this.player = player;
        updateWithPlayer(player);
    }

    public void updateWithPlayer(MapleCharacter player) {
        if (player != null) {
            setPlayer(player);
            setPlayerID(player.getId());
            setUsername(player.getName());
            setChannelID(player.getClient().getChannel());
        } else {
            setUsername("");
            setChannelID(-1);
        }
    }

    @Override
    public void dispose() {
        // remove references for GC
        player = null;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public int getPlayerID() {
        return playerID;
    }

    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }
}
