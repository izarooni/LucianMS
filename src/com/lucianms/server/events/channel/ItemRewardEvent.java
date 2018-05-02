package com.lucianms.server.events.channel;

import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.ItemConstants;
import net.PacketEvent;
import net.server.Server;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleItemInformationProvider.RewardItem;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.List;

/**
 * @author Jay Estrella
 * @author kevintjuh93
 * @author izarooni
 */
public class ItemRewardEvent extends PacketEvent {

    int itemId;

    short slot;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        slot = slea.readShort();
        itemId = slea.readInt();
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
                    Server.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, msg));
                }
                break;
            }
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
