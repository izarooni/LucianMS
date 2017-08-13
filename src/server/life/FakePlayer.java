package server.life;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import tools.MaplePacketCreator;

import java.util.ArrayList;

/**
 * @author izarooni
 */
public class FakePlayer extends MapleCharacter {

    private boolean following = false;

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
        client.announce(MaplePacketCreator.spawnPlayerMapobject(this));
    }

    public void clonePlayer(MapleCharacter player) {
        ArrayList<Item> items = new ArrayList<>(player.getInventory(MapleInventoryType.EQUIPPED).list());
        try {
            setHair(player.getHair());
            setFace(player.getFace());
            setSkinColor(player.getSkinColor());
            setLevel(player.getLevel());
            setJob(player.getJob());
            for (Item item : items) {
                getInventory(MapleInventoryType.EQUIPPED).addFromDB(item.copy());
            }
        } finally {
            items.clear();
        }
    }

    public boolean isFollowing() {
        return following;
    }

    public void setFollowing(boolean following) {
        this.following = following;
    }
}