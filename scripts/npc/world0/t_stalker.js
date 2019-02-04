/* izarooni */
var InventoryType = Java.type("com.lucianms.client.inventory.MapleInventoryType");
var ItemConstants = Java.type("com.lucianms.constants.ItemConstants");
var InventoryManipulator = Java.type("com.lucianms.server.MapleInventoryManipulator");

var status = 0;
var players = null;

function action(mode, type, selection) {
    if (!player.isGM()) {
        cm.sendOk("you do not have permission to view this NPC")
        cm.dispose();
        return;
    }
    if (mode === -1) {
        cm.dispose();
        return;
    } else if (mode === 0) {
        status--;
        if (status === 0) {
            cm.dispose();
            return;
        }
    } else {
        status++;
    }
    if (players == null) {
        players = onlinePlayers();
    }
    if (status === 1) {
        this.stalk = null; // reset for viewing a different player's information
        var text = "Who you lookin' for?\r\n#b";
        text += "\r\n#L0#Search#l\r\n";
        for (var i = 0; i < players.length; i++) {
            var chr = players[i];
            text += "\r\n#L" + chr.id + "# " + chr.name + "#l";
        }
        cm.sendSimple(text);
    } else if (status === 2) {
        if (this.stalk != null || selection > 0) { // coming from a future status || coming from previous status
            this.inventory = null; // reset for viewing other inventory items
            if (this.stalk == null) { // first time viewing player information
                this.stalk = getPlayerById(selection);
            }
            cm.sendSimple("What would you like to do with #b" + this.stalk.name + "#k?\r\n#b"
                    + "\r\n#L0#View equipped#l"
                    + "\r\n#L1#View equips#l"
                    + "\r\n#L2#View use#l"
                    + "\r\n#L3#View setup#l"
                    + "\r\n#L4#View ETC#l"
                    + "\r\n#L5#View cash#l"
                    + "\r\n"
                    + "\r\n#L6#Give item#l"
                    + "\r\n#L7#View information#l"
                    );
        } else {
            cm.sendGetText("Who you lookin' for?");
        }
    } else if (status === 3) {
        if (cm.getText() == null) {
            if (this.inventory != null || getInventoryType(selection) != null) { // coming from a future status || coming from a previous status
                if (this.inventory == null) { // first time viewing this inventory (i.e previoous status was inventory selection)
                    this.inventory = getPlayer(this.stalk.id).getInventory(getInventoryType(selection));
                }
                this.item = null;
                var text = "\r\n";
                for (var i = (this.inventory.getType() == InventoryType.EQUIPPED ? -128 : 0); i < inventory.getSlotLimit(); i++) { // equip inventory positions have negative integers
                    var item = inventory.getItem(i);
                    if (item != null) {
                        text += "\t#L" + i + "##v" + item.getItemId() + "##l";
                        if (Math.abs(i) % 5 == 0)
                            text += "\r\n"; // looks clean
                    }
                }
                if (text.length > 4) { // because "\r\n".length == 4; make sure items have been added
                    cm.sendSimple(text);
                } else {
                    cm.sendPrev("This inventory is empty!");
                    this.emptyInventory = true; // sigh.. special case just for this status handling shit
                }
            } else {
                if (selection == 6) {
                    cm.sendGetText("What is the Id of the item you want to give?");
                } else if (selection == 7) {
                    var t = getPlayer(this.stalk.id);
                    if (t != null) {
                        var otherChars = "\t";
                        var idNamePair = t.getClient().loadCharacterNames(client.getWorldServer());
                        for (var i = 0; i < idNamePair.size(); i++) {
                            otherChars += idNamePair.get(i) + ", ";
                        }
                        cm.sendOk(
                            "#e" + this.stalk.name + "'s stats#n\r\n"
                            + "\r\nGM Level: " + t.getGMLevel()
                            + "\r\nAccount Name: " + t.getClient().getAccountName()
                            + "\r\nAccount Id: " + t.getAccountID()
                            + "\r\nCharacter Id: " + t.getId()
                            + "\r\nOther characters: \r\n" + otherChars
                            + "\r\n\r\nRemote address: " + t.getClient().getRemoteAddress()
                            + "\r\n\r\nMACs: " + t.getClient().getMacs()
                            + "\r\nHWID: " + t.getClient().getHWID()
                            + "\r\n\r\nCalcualted ATK: " + t.calculateMaxBaseDamage(t.getTotalWatk())
                            + "\r\nEXP, Meso, Drop rate: " + `${t.getExpRate()}x, ${t.getMesoRate()}x, ${t.getDropRate()}x`
                            + "\r\nCrush rings: " + t.getCrushRings()
                            + "\r\nFriendship rings: " + t.getFriendshipRings());
                        cm.dispose();
                    } else {
                        cm.sendNext("The player could not be found");
                        status = 0;
                    }
                } else {
                    cm.sendOk("Error");
                    cm.dispose();
                }
            }
        } else {
            // get all online players again but filter by matching name
            players = onlinePlayers(cm.getText());
            cm.setGetText(null);
            status = 0;
            action(1, 0, -1);
        }
    } else if (status === 4) {
        if (cm.getText() == null) { // viewing inventory
            if (this.inventory == null) {
                // prevent bug occurrences i guess
                cm.sendOk("Something's not right!");
                cm.dispose();
            } else if (this.emptyInventory) {
                // when clicking 'ok' on the previous status and the inventory was empty
                cm.sendOk("The player's inventory is now empty");
                cm.dispose();
                return;
            }
            this.changeStat = null; // reset for changing another item stat
            if (this.item == null) { // first time viewing item information
                this.item = this.inventory.getItem(selection);
                if (this.item == null) { // item disappeared/moved from player inventory
                    cm.sendNext("Invalid item");
                    status = 1; // go back and view inventories again
                    return;
                }
            }
            var isEquip = ItemConstants.getInventoryType(this.item.getItemId()) == InventoryType.EQUIP;
            var text = "#b#v" + this.item.getItemId() + "#\t#z" + this.item.getItemId() + "# (" + this.item.getPosition() + " // " + this.item.getItemId() + ")\r\n";
            if (isEquip) {
                text += "\r\n#L0#STR: " + this.item.getStr() + "#l";
                text += "\r\n#L1#DEX: " + this.item.getDex() + "#l";
                text += "\r\n#L2#INT: " + this.item.getInt() + "#l";
                text += "\r\n#L3#LUK: " + this.item.getLuk() + "#l";
                text += "\r\n#L4#WATK: " + this.item.getWatk() + "#l";
                text += "\r\n#L5#MATK: " + this.item.getMatk() + "#l";
                text += "\r\n#L6#WDEF: " + this.item.getWdef() + "#l";
                text += "\r\n#L7#MDEF: " + this.item.getMdef() + "#l";
                text += "\r\n#L8#HP: " + this.item.getHp() + "#l";
                text += "\r\n#L9#MP: " + this.item.getMp() + "#l";
                text += "\r\n#L10#SPEED: " + this.item.getSpeed() + "#l";
                text += "\r\n#L11#JUMP: " + this.item.getJump() + "#l";
                text += "\r\n#L12#Ring ID: " + this.item.getRingId() + "#l";
            } else {
                text += "\r\n#L13#QUANTITY: " + this.item.getQuantity() + "#l";
            }
            text += "\r\n#L14#EXPIRATION: " + this.item.getExpiration() + "#l";
            text += "\r\n#L15#OWNER: " + this.item.getOwner() + "#l";
            text += "\r\n#L16#Remove#l";
            cm.sendSimple(text);
        } else { // giving item
            let itemID = parseInt(cm.getText());
            if (!isNaN(itemID)) {
                let target = getPlayer(this.stalk.id);
                if (InventoryManipulator.checkSpace(target.getClient(), itemID, 1, "")) {
                    InventoryManipulator.addById(target.getClient(), itemID, 1);
                    player.sendMessage(5, `Given 1 of ${itemID} to player '${this.stalk.name}'`);
                    target.sendMessage(5, `You have received an item from '${player.getName()} in your ${ItemConstants.getInventoryType(itemID).name()} inventory`);
                } else {
                    cm.sendOk("The player's inventory is full");
                }
            } else {
                cm.sendOk(`#r${itemID}#k is not a number!`);
            }
            cm.dispose();
        }
    } else if (status === 5) {
        if (selection === 16) {
            InventoryManipulator.removeFromSlot(getPlayer(this.stalk.id).getClient(), this.inventory.getType(), this.item.getPosition(), this.item.getQuantity(), false);
            status = 2;
            action(1, 0, 0);
        } else {
            if (this.changeStat == null) { // first time changing item stat
                this.changeStat = selection;
            }
            let text = "What do you want to set the #b" + getUpdateName(this.changeStat).toUpperCase() + "#k stat to?";
            if (this.error != null) {
                text = this.error + text;
            }
            cm.sendGetText(text);
        }
    } else if (status === 6) {
        if (this.changeStat === 13) { // changing item tag
            updateItem(getPlayer(this.stalk.id), this.item, this.changeStat, cm.getText());
            status -= 3; // go back to item information
        } else {
            let newval = parseInt(cm.getText()); // the new value for the item stat
            this.error = null; // reset error message
            if (isNaN(newval) || ((newval < 0 || newval > 32767) && this.changeStat != 16)) {
                this.error = "#r'" + cm.getText() + "' is an invalid number#k\r\n"; // set error message
                status -= 2; // return to previous status to display error message
            } else {
                updateItem(getPlayer(this.stalk.id), this.item, this.changeStat, newval); // any other stat we can just apply the new value
                status -= 3; // go back to item information
            }
        }
        cm.setGetText(null);
        action(1, 0, 0); // we didn't display any chat dialogues but we modified the status so just recall the function
    }
}

/* ********** functions ********** */
function getPlayer(playerId) {
    for (let i = 0; i < client.getWorldServer().getChannels().size(); i++) {
        let ch = client.getWorldServer().getChannel(i + 1);
        let chr = ch.getPlayerStorage().get(playerId);
        if (chr != null) {
            return chr;
        }
    }
    return null;
}

// Get all online players in the player's world server
// and return them as an array of Player object
function onlinePlayers(filter) {
    let ret = [];
    for (let i = 0; i < client.getWorldServer().getChannels().size(); i++) {
        let ch = client.getWorldServer().getChannel(i + 1);
        let iter = ch.getPlayerStorage().values().iterator();
        while (iter.hasNext()) {
            let p = iter.next();
            if (filter != null) {
                if (!p.getName().toUpperCase().contains(filter.toUpperCase())) {
                    continue;
                }
            }
            let chr = new Player(p.getId(), p.getName());
            ret.push(chr);
        }
    }
    return ret;
}

// Get a Player object by iteration and matching player Ids
function getPlayerById(id) {
    if (players == null) {
        return null;
    }
    for (let i = players.length - 1; i >= 0; i--) {
        if (players[i].id === id) {
            return players[i];
        }
    }
    return null;
}

// Used to get inventory type by selection value
function getInventoryType(i) {
    switch (i) {
        case 0: return InventoryType.EQUIPPED;
        case 1: return InventoryType.EQUIP;
        case 2: return InventoryType.USE;
        case 3: return InventoryType.SETUP;
        case 4: return InventoryType.ETC;
        case 5: return InventoryType.CASH;
    }
    return null;
}

// Used to get stat name by selection value
function getUpdateName(stat) {
    switch (stat) {
        case 0: return "str";
        case 1: return "dex";
        case 2: return "int";
        case 3: return "luk";
        case 4: return "watk";
        case 5: return "matk";
        case 6: return "wdef";
        case 7: return "mdef";
        case 8: return "hp";
        case 9: return "mp";
        case 10: return "speed";
        case 11: return "jump";
        case 12: return "ring id";
        case 13: return "quantity";
        case 14: return "expiration";
        case 15: return "owner";
    }
}

// set the value of the item stat to the newval and
// update the item
function updateItem(player, item, stat, newval) {
    if (player == null) {
        cm.getPlayer().dropMesseage("The player could not be found");
        return;
    }
    if (stat === 0) item.setStr(newval);
    else if (stat === 1) item.setDex(newval);
    else if (stat === 2) item.setInt(newval);
    else if (stat === 3) item.setLuk(newval);
    else if (stat === 4) item.setWatk(newval);
    else if (stat === 5) item.setMatk(newval);
    else if (stat === 6) item.setWdef(newval);
    else if (stat === 7) item.setMdef(newval);
    else if (stat === 8) item.setHp(newval);
    else if (stat === 9) item.setMp(newval);
    else if (stat === 10) item.setSpeed(newval);
    else if (stat === 11) item.setJump(newval);
    else if (stat === 12) item.setRingId(newval);
    else if (stat === 13) item.setQuantity(newval);
    else if (stat === 14) item.setExpiration(newval);
    else if (stat === 15) item.setOwner(newval);
    player.forceUpdateItem(item);
}

// objects
function Player(id, name) {
    this.id = id;
    this.name = name;
}