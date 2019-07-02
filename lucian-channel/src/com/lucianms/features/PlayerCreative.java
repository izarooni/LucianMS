package com.lucianms.features;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.constants.ItemConstants;
import com.lucianms.events.ChangeChannelEvent;
import com.lucianms.events.EnterCashShopEvent;
import com.lucianms.events.PlayerAllChatEvent;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleNPC;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.SavedLocationType;
import tools.MaplePacketCreator;
import tools.Pair;

import java.util.List;
import java.util.stream.Collectors;

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
        MapleMap map = player.getMap();
        String message = event.getContent();

        if (message.isEmpty()) {
            return;
        }

        String[] sInput = message.split(" ");
        if (sInput[0].charAt(0) != '@') {
            return;
        }
        String[] sArgs = new String[sInput.length - 1];
        System.arraycopy(sInput, 1, sArgs, 0, sArgs.length);
        Command cmd = new Command(sInput[0].substring(1));
        CommandArgs args = new CommandArgs(sArgs);

        if (cmd.equals("item")) {
            if (args.length() < 1) {
                player.sendMessage("usage: @item <item_id/name>");
                return;
            }
            Number itemID = args.parseNumber(0, int.class);
            if (itemID != null) {
                if (ItemConstants.getInventoryType(itemID.intValue()) == MapleInventoryType.EQUIP) {
                    map.spawnItemDrop(player, player, new Equip(itemID.intValue()), player.getPosition(), true, false);
                } else {
                    player.sendMessage(5, "Only equips are allowed");
                }
            } else {
                String input = args.concatFrom(0);
                List<Pair<Integer, String>> itemNames = MapleItemInformationProvider.getInstance().getAllItems();
                List<Pair<Integer, String>> found = itemNames.stream()
                        .filter(p -> ItemConstants.getInventoryType(p.getLeft()) == MapleInventoryType.EQUIP)
                        .filter(p -> p.getRight().toLowerCase().contains(input.toLowerCase())).collect(Collectors.toList());
                if (found.size() == 1) {
                    Equip equip = new Equip(found.get(0).getLeft());
                    equip.setSandbox(true);
                    map.spawnItemDrop(player, player, equip, player.getPosition(), true, false);
                } else {
                    found.forEach(p -> player.sendMessage("{} - {}", p.getLeft(), p.getRight()));
                }
                found.clear();
            }
            event.setCanceled(true);
        } else if (cmd.equals("cleardrops")) {
            map.clearDrops();
            map.sendMessage(5, "Drops cleared");
            event.setCanceled(true);
        }
    }

    @PacketWorker
    public void onEnterCashShop(EnterCashShopEvent event) {
        event.getClient().getPlayer().sendMessage("You cannot do that here.");
        event.setCanceled(true);
    }

    @PacketWorker
    public void onChangeChannel(ChangeChannelEvent event) {
        event.getClient().getPlayer().sendMessage("You cannot do that here.");
        event.setCanceled(true);
    }
}
