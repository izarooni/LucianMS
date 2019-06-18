package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import com.lucianms.events.meta.CommunityActions;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.*;
import com.lucianms.server.maps.*;
import tools.MaplePacketCreator;
import tools.Pair;

import java.util.Arrays;

/**
 * @author Matze
 * @author izarooni
 */
public class PlayerInteractionEvent extends PacketEvent {

    private CommunityActions action;
    private String username;
    private String content;
    private String password = null;
    private int itemID;
    private int objectID;
    private int playerID;
    private int inventoryType;
    private int mesos;
    private int x, y;
    private short slot;
    private short targetSlot;
    private short quantity;
    private short bundles;
    private short perBundle;
    private byte pieceType;
    private byte type;
    private byte turn, position;

    @Override
    public void processInput(MaplePacketReader reader) {
        MapleCharacter player = getClient().getPlayer();
        action = CommunityActions.getByValue(reader.readByte());
        if (action == null) {
            setCanceled(true);
        }
        switch (action) {
            case CREATE:
                type = reader.readByte();
                if (type == 1 || type == 2) {
                    content = reader.readMapleAsciiString();
                    if (reader.readByte() == 1) {
                        password = reader.readMapleAsciiString();
                    }
                    pieceType = reader.readByte();
                } else if (type == 4 || type == 5) {
                    content = reader.readMapleAsciiString();
                    reader.skip(3);
                    itemID = reader.readInt();
                }
                break;
            case INVITE:
                playerID = reader.readInt();
                if (playerID == getClient().getPlayer().getId()) {
                    setCanceled(true);
                }
                break;
            case DECLINE:
                break;
            case VISIT:
                if (player.getTrade() == null) {
                    objectID = reader.readInt();
                    MapleMapObject mapObject = player.getMap().getMapObject(objectID);
                    if (mapObject != null) {
                        if (mapObject instanceof MapleMiniGame) {
                            if (reader.readByte() == 1) {
                                password = reader.readMapleAsciiString();
                            }
                        }
                    } else {
                        setCanceled(true);
                    }
                }
                break;
            case ROOM:
                break;
            case CHAT:
                if (player.getTrade() != null
                        || player.getPlayerShop() != null
                        || player.getMiniGame() != null
                        || player.getHiredMerchant() != null) {
                    content = reader.readMapleAsciiString();
                } else {
                    setCanceled(true);
                }
                break;
            case CHAT_THING:
                break;
            case EXIT:
                break;
            case OPEN:
                break;
            case TRADE_BIRTHDAY:
                break;
            case SET_ITEMS:
                inventoryType = reader.readByte();
                slot = reader.readShort();
                quantity = reader.readShort();
                targetSlot = reader.readByte();
                break;
            case SET_MESO:
                if (player.getTrade() != null) {
                    mesos = reader.readInt();
                } else {
                    setCanceled(true);
                }
                break;
            case CONFIRM:
                break;
            case TRANSACTION:
                break;
            case PUT_ITEM:
            case ADD_ITEM:
                inventoryType = reader.readByte();
                slot = reader.readShort();
                bundles = reader.readShort();
                perBundle = reader.readShort();
                mesos = reader.readInt();
                break;
            case UPDATE_MERCHANT:
                break;
            case REMOVE_ITEM:
                slot = reader.readShort();
                break;
            case BAN_PLAYER:
                username = reader.readMapleAsciiString();
                break;
            case MERCHANT_THING:
                break;
            case OPEN_STORE:
                break;
            case BUY:
            case MERCHANT_BUY:
                itemID = reader.readByte();
                quantity = reader.readShort();
                break;
            case TAKE_ITEM_BACK:
                slot = reader.readShort();
                break;
            case MAINTENANCE_OFF:
                if (player.getHiredMerchant() == null) {
                    setCanceled(true);
                }
                break;
            case MERCHANT_ORGANIZE:
                if (player.getHiredMerchant() == null) {
                    setCanceled(true);
                }
                break;
            case CLOSE_MERCHANT:
                break;
            case REAL_CLOSE_MERCHANT:
                break;
            case MERCHANT_MESO:
                break;
            case SOMETHING:
                break;
            case VIEW_VISITORS:
                break;
            case BLACKLIST:
                break;
            case REQUEST_TIE:
                break;
            case ANSWER_TIE:
                break;
            case GIVE_UP:
                break;
            case EXIT_AFTER_GAME:
                break;
            case CANCEL_EXIT:
                break;
            case READY:
                break;
            case UN_READY:
                break;
            case START:
                break;
            case GET_RESULT:
                break;
            case SKIP:
                break;
            case MOVE_OMOK:
                x = reader.readInt();
                y = reader.readInt();
                type = reader.readByte();
                break;
            case SELECT_CARD:
                turn = reader.readByte();
                position = reader.readByte();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMap map = player.getMap();
        switch (action) {
            case CREATE: {
                if (type == 3) {// trade
                    MapleTrade.startTrade(player);
                } else if (type == 1) { // omok mini game
                    if (player.getChalkboard() != null || FieldLimit.CANNOTMINIGAME.check(map.getFieldLimit())) {
                        return null;
                    }
                    if (player.getInventory(MapleInventoryType.ETC).findById(4080000 + pieceType) == null) {
                        // make sure player has required omok table to start this minigame
                        return null;
                    }
                    MapleMiniGame game = new MapleMiniGame(type, player, content, password);
                    game.setPieceType(pieceType);
                    player.setMiniGame(game);
                    game.setGameType("omok");
                    map.addMapObject(game);
                    map.broadcastMessage(MaplePacketCreator.addOmokBox(player, game));
                    game.sendOmok(getClient());
                } else if (type == 2) { // matchcard
                    if (player.getChalkboard() != null) {
                        return null;
                    }
                    if (player.getInventory(MapleInventoryType.ETC).findById(4080100) == null) {
                        // make sure player has matchcard to start this minigame
                        return null;
                    }
                    MapleMiniGame game = new MapleMiniGame(type, player, content, password);
                    game.setPieceType(Math.max(0, Math.min(3, pieceType)));
                    if (pieceType == 0) {
                        game.setMatchesToWin(6);
                    } else if (pieceType == 1) {
                        game.setMatchesToWin(10);
                    } else if (pieceType == 2) {
                        game.setMatchesToWin(15);
                    }
                    game.setGameType("matchcard");
                    player.setMiniGame(game);
                    map.addMapObject(game);
                    map.broadcastMessage(MaplePacketCreator.addMatchCardBox(player, game));
                    game.sendMatchCard(getClient());
                } else if (type == 4 || type == 5) { // shop
                    if (!map.getMapObjectsInRange(player.getPosition(), 23000, Arrays.asList(MapleMapObjectType.SHOP, MapleMapObjectType.HIRED_MERCHANT)).isEmpty()) {
                        return null;
                    }
                    if (player.getInventory(MapleInventoryType.CASH).countById(itemID) < 1) {
                        return null;
                    }

                    if (player.getMapId() > 910000000 && player.getMapId() < 910000023
                            || itemID > 5030000 && itemID < 5030012
                            || itemID > 5140000 && itemID < 5140006) {
                        if (type == 4) {
                            MaplePlayerShop shop = new MaplePlayerShop(player, content);
                            player.setPlayerShop(shop);
                            map.addMapObject(shop);
                            shop.sendShop(getClient());
                            getClient().announce(MaplePacketCreator.getPlayerShopRemoveVisitor(1));
                        } else {
                            HiredMerchant merchant = new HiredMerchant(player, itemID, content);
                            player.setHiredMerchant(merchant);
                            player.getClient().getChannelServer().addHiredMerchant(player.getId(), merchant);
                            player.announce(MaplePacketCreator.getHiredMerchant(player, merchant, true));
                        }
                    }
                }
                break;
            }
            case INVITE: {
                MapleCharacter target = map.getCharacterById(playerID);
                MapleTrade.inviteTrade(player, target);
                break;
            }
            case DECLINE:
                MapleTrade.declineTrade(player);
                break;
            case VISIT: {
                if (player.getTrade() != null && player.getTrade().getPartner() != null) {
                    if (!player.getTrade().isFullTrade() && !player.getTrade().getPartner().isFullTrade()) {
                        MapleTrade.visitTrade(player, player.getTrade().getPartner().getChr());
                    } else {
                        getClient().announce(MaplePacketCreator.enableActions()); //Ill be nice and not dc u
                        return null;
                    }
                } else {
                    MapleMapObject ob = map.getMapObject(objectID);
                    if (ob instanceof MaplePlayerShop) {
                        MaplePlayerShop shop = (MaplePlayerShop) ob;
                        if (shop.isBanned(player.getName())) {
                            player.dropMessage(1, "You have been banned from this store.");
                            return null;
                        }
                        if (shop.hasFreeSlot() && !shop.isVisitor(player)) {
                            shop.addVisitor(player);
                            player.setPlayerShop(shop);
                            shop.sendShop(getClient());
                        }
                    } else if (ob instanceof MapleMiniGame) {
                        MapleMiniGame game = (MapleMiniGame) ob;
                        if (password != null && !password.equals(game.getPassword())) {
                            player.dropMessage("The password is not correct.");
                            return null;
                        }
                        if (game.hasFreeSlot() && !game.isVisitor(player)) {
                            game.addVisitor(player);
                            player.setMiniGame(game);
                            switch (game.getGameType()) {
                                case "omok":
                                    game.sendOmok(getClient());
                                    break;
                                case "matchcard":
                                    game.sendMatchCard(getClient());
                                    break;
                            }
                        } else {
                            player.getClient().announce(MaplePacketCreator.getMiniGameFull());
                        }
                    } else if (ob instanceof HiredMerchant && player.getHiredMerchant() == null) {
                        HiredMerchant merchant = (HiredMerchant) ob;
                        if (merchant.isOwner(player)) {
                            merchant.setOpen(false);
                            merchant.removeAllVisitors("");
                            getClient().announce(MaplePacketCreator.getHiredMerchant(player, merchant, false));
                        } else if (!merchant.isOpen()) {
                            player.dropMessage(1, "This shop is in maintenance, please come by later.");
                            return null;
                        } else if (merchant.getFreeSlot() == -1) {
                            player.dropMessage(1, "This shop has reached it's maximum capacity, please come by later.");
                            return null;
                        } else {
                            merchant.addVisitor(player);
                            getClient().announce(MaplePacketCreator.getHiredMerchant(player, merchant, false));
                        }
                        player.setHiredMerchant(merchant);
                    }
                }
                break;
            }
            case ROOM:
                break;
            case CHAT: {
                HiredMerchant merchant = player.getHiredMerchant();
                if (player.getTrade() != null) {
                    player.getTrade().chat(content);
                } else if (player.getPlayerShop() != null) { //mini game
                    MaplePlayerShop shop = player.getPlayerShop();
                    if (shop != null) {
                        shop.chat(getClient(), content);
                    }
                } else if (player.getMiniGame() != null) {
                    MapleMiniGame game = player.getMiniGame();
                    if (game != null) {
                        game.chat(getClient(), content);
                    }
                } else if (merchant != null) {
                    String message = player.getName() + " : " + content;
                    byte slot = (byte) (merchant.getVisitorSlot(player) + 1);
                    merchant.getMessages().add(new Pair<>(message, slot));
                    merchant.broadcastToVisitors(MaplePacketCreator.hiredMerchantChat(message, slot));
                }
                break;
            }
            case CHAT_THING:
                break;
            case EXIT: {
                if (player.getTrade() != null) {
                    MapleTrade.cancelTrade(player);
                } else {
                    MaplePlayerShop shop = player.getPlayerShop();
                    MapleMiniGame game = player.getMiniGame();
                    HiredMerchant merchant = player.getHiredMerchant();
                    if (shop != null) {
                        if (shop.isOwner(player)) {
                            for (MaplePlayerShopItem mpsi : shop.getItems()) {
                                if (mpsi.getBundles() > 2) {
                                    Item iItem = mpsi.getItem().duplicate();
                                    iItem.setQuantity((short) (mpsi.getBundles() * iItem.getQuantity()));
                                    MapleInventoryManipulator.addFromDrop(getClient(), iItem, false);
                                } else if (mpsi.isExist()) {
                                    MapleInventoryManipulator.addFromDrop(getClient(), mpsi.getItem(), true);
                                }
                            }
                            map.broadcastMessage(MaplePacketCreator.removeCharBox(player));
                            shop.removeVisitors();
                        } else {
                            shop.removeVisitor(player);
                        }
                        player.setPlayerShop(null);
                    } else if (game != null) {
                        player.setMiniGame(null);
                        if (game.isOwner(player)) {
                            map.broadcastMessage(MaplePacketCreator.removeCharBox(player));
                            game.broadcastToVisitor(MaplePacketCreator.getMiniGameClose());
                        } else {
                            game.removeVisitor(player);
                        }
                    } else if (merchant != null) {
                        merchant.removeVisitor(player);
                        player.setHiredMerchant(null);
                    }
                }
                break;
            }
            case OPEN: {
                MaplePlayerShop shop = player.getPlayerShop();
                HiredMerchant merchant = player.getHiredMerchant();
                if (shop != null && shop.isOwner(player)) {
                    map.broadcastMessage(MaplePacketCreator.addCharBox(player, 4));
                } else if (merchant != null && merchant.isOwner(player)) {
                    player.setHasMerchant(true);
                    merchant.setOpen(true);
                    map.addMapObject(merchant);
                    player.setHiredMerchant(null);
                    map.broadcastMessage(MaplePacketCreator.spawnHiredMerchant(merchant));
                }
                break;
            }
            case TRADE_BIRTHDAY:
                break;
            case SET_ITEMS: {
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                MapleInventoryType ivType = MapleInventoryType.getByType((byte) inventoryType);
                Item item = player.getInventory(ivType).getItem(slot);
                if (quantity < 1 || quantity > item.getQuantity()) {
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }
                if (player.getTrade() != null) {
                    if ((quantity <= item.getQuantity() && quantity >= 0) || ItemConstants.isRechargable(item.getItemId())) {
                        if (ii.isDropRestricted(item.getItemId())) { // ensure that undroppable items do not make it to the trade window
                            if (!((item.getFlag() & ItemConstants.KARMA) == ItemConstants.KARMA || (item.getFlag() & ItemConstants.SPIKES) == ItemConstants.SPIKES)) {
                                getClient().announce(MaplePacketCreator.enableActions());
                                return null;
                            }
                        }
                        Item tradeItem = item.duplicate();
                        if (ItemConstants.isRechargable(item.getItemId())) {
                            tradeItem.setQuantity(item.getQuantity());
                            MapleInventoryManipulator.removeFromSlot(getClient(), ivType, item.getPosition(), item.getQuantity(), true);
                        } else {
                            tradeItem.setQuantity(quantity);
                            MapleInventoryManipulator.removeFromSlot(getClient(), ivType, item.getPosition(), quantity, true);
                        }
                        tradeItem.setPosition(targetSlot);
                        player.getTrade().addItem(tradeItem);
                    }
                }
                break;
            }
            case SET_MESO: {
                player.getTrade().setMeso(mesos);
                break;
            }
            case CONFIRM: {
                MapleTrade.completeTrade(player);
                break;
            }
            case TRANSACTION:
                break;
            case PUT_ITEM:
            case ADD_ITEM: {
                MapleInventoryType type = MapleInventoryType.getByType((byte) inventoryType);
                if (player.getInventory(type).getItem(slot) == null
                        || player.getItemQuantity(player.getInventory(type).getItem(slot).getItemId(), false) < bundles
                        || ((player.getInventory(type).getItem(slot).getFlag() >> ItemConstants.UNTRADEABLE) & 1) != 0) {
                    return null;
                }
                if (perBundle <= 0 || perBundle * bundles > 2000 || bundles <= 0 || mesos <= 0) {
                    return null;
                }
                Item ivItem = player.getInventory(type).getItem(slot);
                Item sellItem = ivItem.duplicate();
                if (player.getItemQuantity(ivItem.getItemId(), false) < perBundle * bundles) {
                    return null;
                }
                sellItem.setQuantity(perBundle);
                MaplePlayerShopItem item = new MaplePlayerShopItem(sellItem, bundles, mesos);
                MaplePlayerShop shop = player.getPlayerShop();
                HiredMerchant merchant = player.getHiredMerchant();
                if (shop != null && shop.isOwner(player)) {
                    if (ivItem.getQuantity() >= bundles * perBundle) {
                        shop.addItem(item);
                        getClient().announce(MaplePacketCreator.getPlayerShopItemUpdate(shop));
                    }
                } else if (merchant != null && merchant.isOwner(player)) {
                    merchant.addItem(item);
                    getClient().announce(MaplePacketCreator.updateHiredMerchant(merchant, player));
                }
                if (ItemConstants.isRechargable(ivItem.getItemId())) {
                    MapleInventoryManipulator.removeFromSlot(getClient(), type, slot, ivItem.getQuantity(), true);
                } else {
                    MapleInventoryManipulator.removeFromSlot(getClient(), type, slot, (short) (bundles * perBundle), true);
                }
                break;
            }
            case MERCHANT_BUY:
            case BUY: {
                if (quantity < 1) {
                    return null;
                }
                MaplePlayerShop shop = player.getPlayerShop();
                HiredMerchant merchant = player.getHiredMerchant();
                if (merchant != null && merchant.getOwner().equals(player.getName())) {
                    return null;
                }
                if (shop != null && shop.isVisitor(player)) {
                    shop.buy(getClient(), itemID, quantity);
                    shop.broadcast(MaplePacketCreator.getPlayerShopItemUpdate(shop));
                } else if (merchant != null) {
                    merchant.buy(getClient(), itemID, quantity);
                    merchant.broadcastToVisitors(MaplePacketCreator.updateHiredMerchant(merchant, player));
                }
                break;
            }
            case UPDATE_MERCHANT:
                break;
            case REMOVE_ITEM: {
                MaplePlayerShop shop = player.getPlayerShop();
                if (shop != null && shop.isOwner(player)) {
                    if (slot >= shop.getItems().size() || slot < 0) {
                        return null;
                    }
                    MaplePlayerShopItem item = shop.getItems().get(slot);
                    Item ivItem = item.getItem().duplicate();
                    shop.removeItem(slot);
                    ivItem.setQuantity(item.getBundles());
                    MapleInventoryManipulator.addFromDrop(getClient(), ivItem, false);
                    getClient().announce(MaplePacketCreator.getPlayerShopItemUpdate(shop));
                }
                break;
            }
            case BAN_PLAYER: {
                if (player.getPlayerShop() != null && player.getPlayerShop().isOwner(player)) {
                    player.getPlayerShop().banPlayer(username);
                }
                break;
            }
            case MERCHANT_THING:
                break;
            case OPEN_STORE:
                break;
            case TAKE_ITEM_BACK: {
                HiredMerchant merchant = player.getHiredMerchant();
                if (merchant != null && merchant.isOwner(player)) {
                    MaplePlayerShopItem item = merchant.getItems().get(slot);
                    if (!MapleInventory.checkSpot(player, item.getItem())) {
                        getClient().announce(MaplePacketCreator.enableActions());
                        return null;
                    }
                    if (item.getBundles() > 0) {
                        Item iitem = item.getItem();
                        iitem.setQuantity((short) (item.getItem().getQuantity() * item.getBundles()));
                        MapleInventoryManipulator.addFromDrop(getClient(), iitem, true);
                    }
                    merchant.removeFromSlot(slot);
                    getClient().announce(MaplePacketCreator.updateHiredMerchant(merchant, player));
                }
                break;
            }
            case MAINTENANCE_OFF: {
                HiredMerchant merchant = player.getHiredMerchant();
                if (merchant.getItems().isEmpty() && merchant.isOwner(player)) {
                    merchant.closeShop(getClient(), false);
                    player.setHasMerchant(false);
                }
                if (merchant.isOwner(player)) {
                    merchant.getMessages().clear();
                    merchant.setOpen(true);
                }
                player.setHiredMerchant(null);
                getClient().announce(MaplePacketCreator.enableActions());
                break;
            }
            case MERCHANT_ORGANIZE: {
                HiredMerchant merchant = player.getHiredMerchant();
                if (!merchant.isOwner(player)) {
                    return null;
                }

                if (player.getMerchantMeso() > 0) {
                    int possible = Integer.MAX_VALUE - player.getMerchantMeso();
                    if (possible > 0) {
                        if (possible < player.getMerchantMeso()) {
                            player.gainMeso(possible, false);
                            player.setMerchantMeso(player.getMerchantMeso() - possible);
                        } else {
                            player.gainMeso(player.getMerchantMeso(), false);
                            player.setMerchantMeso(0);
                        }
                    }
                }
                for (int i = 0; i < merchant.getItems().size(); i++) {
                    if (!merchant.getItems().get(i).isExist()) {
                        merchant.removeFromSlot(i);
                    }
                }
                if (merchant.getItems().isEmpty()) {
                    getClient().announce(MaplePacketCreator.hiredMerchantOwnerLeave());
                    getClient().announce(MaplePacketCreator.leaveHiredMerchant(0x00, 0x03));
                    merchant.closeShop(getClient(), false);
                    player.setHasMerchant(false);
                    return null;
                }
                getClient().announce(MaplePacketCreator.updateHiredMerchant(merchant, player));
                break;
            }
            case CLOSE_MERCHANT: {
                HiredMerchant merchant = player.getHiredMerchant();
                if (merchant != null && merchant.isOwner(player)) {
                    getClient().announce(MaplePacketCreator.hiredMerchantOwnerLeave());
                    getClient().announce(MaplePacketCreator.leaveHiredMerchant(0x00, 0x03));
                    merchant.closeShop(getClient(), false);
                    player.setHasMerchant(false);
                }
                break;
            }
            case REAL_CLOSE_MERCHANT:
                break;
            case MERCHANT_MESO:
                break;
            case SOMETHING:
                break;
            case VIEW_VISITORS:
                break;
            case BLACKLIST:
                break;
            case REQUEST_TIE: {
                MapleMiniGame game = player.getMiniGame();
                if (game.isOwner(player)) {
                    game.broadcastToVisitor(MaplePacketCreator.getMiniGameRequestTie(game));
                } else {
                    game.getOwner().getClient().announce(MaplePacketCreator.getMiniGameRequestTie(game));
                }
                break;
            }
            case ANSWER_TIE: {
                MapleMiniGame game = player.getMiniGame();
                if (game.getGameType().equals("omok")) {
                    game.broadcast(MaplePacketCreator.getMiniGameTie(game));
                }
                if (game.getGameType().equals("matchcard")) {
                    game.broadcast(MaplePacketCreator.getMatchCardTie(game));
                }
                break;
            }
            case GIVE_UP: {
                MapleMiniGame game = player.getMiniGame();
                if (game.getGameType().equals("omok")) {
                    if (game.isOwner(player)) {
                        game.broadcast(MaplePacketCreator.getMiniGameOwnerForfeit(game));
                    } else {
                        game.broadcast(MaplePacketCreator.getMiniGameVisitorForfeit(game));
                    }
                }
                if (game.getGameType().equals("matchcard")) {
                    if (game.isOwner(player)) {
                        game.broadcast(MaplePacketCreator.getMatchCardVisitorWin(game));
                    } else {
                        game.broadcast(MaplePacketCreator.getMatchCardOwnerWin(game));
                    }
                }
                break;
            }
            case EXIT_AFTER_GAME:
                break;
            case CANCEL_EXIT:
                break;
            case READY: {
                MapleMiniGame game = player.getMiniGame();
                game.broadcast(MaplePacketCreator.getMiniGameReady(game));
                break;
            }
            case UN_READY: {
                MapleMiniGame game = player.getMiniGame();
                game.broadcast(MaplePacketCreator.getMiniGameUnReady(game));
                break;
            }
            case START: {
                MapleMiniGame game = player.getMiniGame();
                game.setStarted(true);
                if (game.getGameType().equals("omok")) {
                    game.broadcast(MaplePacketCreator.getMiniGameStart(game, game.getLoser()));
                    map.broadcastMessage(MaplePacketCreator.addOmokBox(game.getOwner(), game));
                }
                if (game.getGameType().equals("matchcard")) {
                    game.shuffleList();
                    game.broadcast(MaplePacketCreator.getMatchCardStart(game, game.getLoser()));
                    map.broadcastMessage(MaplePacketCreator.addMatchCardBox(game.getOwner(), game));
                }
                break;
            }
            case GET_RESULT:
                break;
            case SKIP: {
                MapleMiniGame game = player.getMiniGame();
                if (game.isOwner(player)) {
                    game.broadcast(MaplePacketCreator.getMiniGameSkipOwner(game));
                } else {
                    game.broadcast(MaplePacketCreator.getMiniGameSkipVisitor(game));
                }
                break;
            }
            case MOVE_OMOK: {
                player.getMiniGame().setPiece(x, y, type, player);
                break;
            }
            case SELECT_CARD: {
                MapleMiniGame game = player.getMiniGame();
                int firstSlot = game.getFirstSlot();
                if (turn == 1) {
                    game.setFirstSlot(position);
                    if (game.isOwner(player)) {
                        game.broadcastToVisitor(MaplePacketCreator.getMatchCardSelect(game, turn, position, firstSlot, turn));
                    } else {
                        game.getOwner().getClient().announce(MaplePacketCreator.getMatchCardSelect(game, turn, position, firstSlot, turn));
                    }
                } else if ((game.getCardId(firstSlot + 1)) == (game.getCardId(position + 1))) {
                    if (game.isOwner(player)) {
                        game.broadcast(MaplePacketCreator.getMatchCardSelect(game, turn, position, firstSlot, 2));
                        game.setOwnerPoints();
                    } else {
                        game.broadcast(MaplePacketCreator.getMatchCardSelect(game, turn, position, firstSlot, 3));
                        game.setVisitorPoints();
                    }
                } else if (game.isOwner(player)) {
                    game.broadcast(MaplePacketCreator.getMatchCardSelect(game, turn, position, firstSlot, 0));
                } else {
                    game.broadcast(MaplePacketCreator.getMatchCardSelect(game, turn, position, firstSlot, 1));
                }
                break;
            }
        }
        return null;
    }
}
