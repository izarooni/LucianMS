package com.lucianms.features;

import client.MapleCharacter;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.server.events.channel.PlayerAllChatEvent;
import com.lucianms.server.events.channel.EnterCashShopEvent;
import constants.ItemConstants;
import constants.PlayerToggles;
import net.server.channel.handlers.ChangeChannelEvent;
import server.FieldBuilder;
import server.life.MapleLifeFactory;
import server.life.MapleNPC;
import server.maps.MapleMap;
import server.maps.SavedLocationType;
import tools.MaplePacketCreator;

import java.util.List;

/**
 * @author izarooni
 */
public class PlayerCreative extends GenericEvent {

    private static final int CreativeField = 808;

    public PlayerCreative() {
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        List<GenericEvent> events = player.getGenericEvents();
        GenericEvent event = events.stream().filter(p -> (p instanceof PlayerCreative)).findFirst().orElse(null);
        if (event != null) {
            player.sendMessage(5, "You are already in a Sandbox environment.");
            return;
        }

        NPCScriptManager.dispose(player.getClient());

        player.saveLocation(SavedLocationType.OTHER.name());
        FieldBuilder fb = new FieldBuilder(player.getWorld(), player.getClient().getChannel(), CreativeField);
        MapleMap map = fb.loadNPCs().loadPortals().loadFootholds().build();
        map.getPortals().forEach(portal -> portal.setPortalStatus(false));

        MapleNPC npc = MapleLifeFactory.getNPC(9899958);
        npc.setPosition(player.getPosition().getLocation());
        npc.setCy(186);
        npc.setRx0(135 + 50);
        npc.setRx1(135 - 50);
        npc.setFh(0);

        MapleInventoryType[] inventoryTypes = MapleInventoryType.values();
        MapleInventory[] inventory = new MapleInventory[inventoryTypes.length];
        for (MapleInventoryType types : inventoryTypes) {
            inventory[types.ordinal()] = new MapleInventory(types, (byte) 26);
        }
        player.setCreativeInventory(inventory);

        player.changeMap(map);
        player.announce(MaplePacketCreator.getCharInfo(player));
        player.addGenericEvent(this);

        map.addMapObject(npc);
        map.broadcastMessage(MaplePacketCreator.spawnNPC(npc));
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.getToggles().remove(PlayerToggles.CommandNPCAccess);
        player.removeGenericEvent(this);
        player.setCreativeInventory(null);
        player.announce(MaplePacketCreator.getCharInfo(player));
        player.changeMap(player.getSavedLocation(SavedLocationType.OTHER.name()));
    }

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        unregisterPlayer(player);
        return false;
    }

    @PacketWorker
    public void onChatEvent(PlayerAllChatEvent event) {
        MapleCharacter player = event.getClient().getPlayer();
        String message = event.getContent();

        String[] args = message.split(" ");

        player.getToggles().put(PlayerToggles.CommandNPCAccess, false);

        if (args[0].equals("@want")) {
            int itemID;
            try {
                itemID = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(5, "'{}' is not a number.", args[1]);
                return;
            }
            Item item;
            if (ItemConstants.getInventoryType(itemID) == MapleInventoryType.EQUIP) {
                item = new Equip(itemID, (short) 0);
            } else {
                player.sendMessage(5, "Only equips are allowed!");
                return;
            }
            player.getMap().spawnItemDrop(player, player, item, player.getPosition(), true, false);
        } else if (args[0].equals("cleardrops")) {
            player.getMap().clearDrops();
        }
    }

    @PacketWorker
    public void onEnterCashShop(EnterCashShopEvent event) {
        event.getClient().getPlayer().sendMessage(5, PlayerToggles.ErrorMessage);
        event.setCanceled(true);
    }

    @PacketWorker
    public void onChangeChannel(ChangeChannelEvent event) {
        event.getClient().getPlayer().sendMessage(5, PlayerToggles.ErrorMessage);
        event.setCanceled(true);
    }
}
