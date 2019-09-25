package com.lucianms.server.life;

import com.lucianms.client.FakeClient;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import tools.MaplePacketCreator;

import java.util.ArrayList;

/**
 * @author izarooni
 */
public class FakePlayer extends MapleCharacter {

    private final FakeClient client = new FakeClient();
    private long expiration;
    private boolean following;

    public FakePlayer(String username) {
        // default character data
        setName(username);
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(MaplePacketCreator.removePlayerFromMap(getId()));
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (isHidden()) {
            return;
        }
        client.announce(MaplePacketCreator.getUserEnterField(this));
    }

    @Override
    public FakeClient getClient() {
        return client;
    }

    @Override
    public void announce(byte[] packet) {
        // do nothing
    }

    public void clonePlayer(MapleCharacter player) {
        setPosition(player.getPosition());
        setHair(player.getHair());
        setFace(player.getFace());
        setSkinColor(player.getSkinColor());
        setLevel(player.getLevel());
        setJob(player.getJob());
        setStance(player.getStance());

        ArrayList<Item> items = new ArrayList<>(player.getInventory(MapleInventoryType.EQUIPPED).list());
        for (Item item : items) {
            Equip eq = (Equip) item;
            if (eq.getRingId() < 1) {
                getInventory(MapleInventoryType.EQUIPPED).addFromDB(item.duplicate());
            }
        }
        items.clear();
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public boolean isFollowing() {
        return following;
    }

    public void setFollowing(boolean following) {
        this.following = following;
    }
}
