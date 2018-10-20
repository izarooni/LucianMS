package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.constants.ItemConstants;
import com.lucianms.events.PacketEvent;
import com.lucianms.server.Server;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleItemInformationProvider.RewardItem;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.util.List;

/**
 * @author Jay Estrella
 * @author kevintjuh93
 * @author izarooni
 */
public class PlayerItemUseRewardEvent extends PacketEvent {

    int itemId;
    short slot;

    @Override
    public void processInput(MaplePacketReader reader) {
        slot = reader.readShort();
        itemId = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (itemId == 2022336) {
            NPCScriptManager.start(getClient(), 2007, "f_level_rewards");
            NPCScriptManager.action(getClient(), (byte) 1, (byte) 0, slot); // open box
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        Item item = player.getInventory(MapleInventoryType.USE).getItem(slot);
        if (item == null || item.getItemId() != itemId) {
            return null;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Pair<Integer, List<RewardItem>> rewards = ii.getItemReward(itemId);

        for (RewardItem reward : rewards.getRight()) {

            if (!MapleInventoryManipulator.checkSpace(getClient(), reward.itemid, reward.quantity, "")) {
                getClient().announce(MaplePacketCreator.getShowInventoryFull());
                break;
            }

            if (Randomizer.nextInt(rewards.getLeft()) < reward.prob) {
                if (ItemConstants.getInventoryType(reward.itemid) == MapleInventoryType.EQUIP) {
                    final Item ritem = ii.getEquipById(reward.itemid);
                    if (reward.period != -1) {
                        ritem.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
                    }
                    MapleInventoryManipulator.addFromDrop(getClient(), ritem, false);
                } else {
                    MapleInventoryManipulator.addById(getClient(), reward.itemid, reward.quantity);
                }
                MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemId, 1, false, false);
                if (reward.worldmsg != null) {
                    String msg = reward.worldmsg;
                    msg = msg.replaceAll("/name", player.getName());
                    msg = msg.replaceAll("/item", ii.getName(reward.itemid));
                    Server.broadcastMessage(MaplePacketCreator.serverNotice(6, msg));
                }
                break;
            }
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
