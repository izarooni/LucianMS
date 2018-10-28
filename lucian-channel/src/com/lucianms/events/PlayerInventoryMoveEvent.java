package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

/**
 * @author Matze
 * @author izarooni
 */
public class PlayerInventoryMoveEvent extends PacketEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerInventoryMoveEvent.class);

    private MapleInventoryType inventoryType;
    private short source;
    private short action;
    private short quantity;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(4);
        inventoryType = MapleInventoryType.getByType(reader.readByte());
        source = reader.readShort();
        action = reader.readShort();
        quantity = reader.readShort();

//        if (source < 0 && action > 0 && source == -149) {
//            NPCScriptManager.start(getClient(), 9010000, "f_equip_info");
//            setCanceled(true);
//        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.InventoryMove);
        if (spamTracker.testFor(100) && spamTracker.getTriggers() > 4) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        spamTracker.record();

        Item item;
        if (source < 0 && action > 0) {
            item = player.getInventory(MapleInventoryType.EQUIPPED).getItem(source);
            MapleInventoryManipulator.unequip(getClient(), source, action);
            return item;
        } else if (action < 0) {
            item = player.getInventory(MapleInventoryType.EQUIP).getItem(source);
            MapleInventoryManipulator.equip(getClient(), source, action);
            return item;
        } else if (action == 0) {
            return MapleInventoryManipulator.drop(getClient(), inventoryType, source, quantity);
        } else {
            item = player.getInventory(inventoryType).getItem(source);
            MapleInventoryManipulator.move(getClient(), inventoryType, source, action);
            return item;
        }
    }

    public MapleInventoryType getInventoryType() {
        return inventoryType;
    }

    public short getSource() {
        return source;
    }

    public short getAction() {
        return action;
    }

    public short getQuantity() {
        return quantity;
    }
}