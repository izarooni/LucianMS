/* izarooni */
const InventoryType = Java.type("com.lucianms.client.inventory.MapleInventoryType");
const ItemConstants = Java.type("com.lucianms.constants.ItemConstants");
const InventoryManipulator = Java.type("com.lucianms.server.MapleInventoryManipulator");

let status = 0;
let target = {
    p: undefined, // the targeted Player object
    inv: undefined, // the targeted Player's Inventory object
    item: undefined, // the targeted Player's Item object
    stat: undefined, // the stat to change on the targeted Player's Item Object
    error: undefined, // the output message for Item object stat change result
    events: [], // array of targeted Player's GenericEvent objects
    reset() {
        target.p = undefined;
        target.inv = undefined;
        target.item = undefined;
        target.events = [];
    }
};

function action(mode, type, selection) {
    if (!player.isGM()) {
        cm.sendOk("you do not have permission to view this NPC");
        return cm.dispose();
    }
         if (mode == 0) status--;
    else if (mode == 1) status++;
    else if (mode < 0 || status < 1) return cm.dispose();

    if (status == 1) {
        target.reset();
        let content = "Who you lookin' for?\r\n#b";
        content += "\r\n#L0#Search#l\r\n";
        let online = GetPlayers();
        for (let i = 0; i < online.length; i++) {
            content += `\r\n#L${online[i].id}#${online[i].name}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        if (selection == 0) {
            return cm.sendGetText("");
        }
        if (target.p == undefined) target.p = GetPlayer(selection);
        target.inv = undefined;
        let content = "What would you like to do with #b" + target.p.name + "#k?\r\n#b";
        let types = InventoryType.values();
        for (let i = 1; i < types.length; i++) {
            content += `\r\n#L${i}#${types[i].name()}#l`;
        }
        content += "\r\n";
        content += "\r\n#L7#Give item#l";               
        content += "\r\n#L8#View information#l";               
        content += "\r\b#L9#Manage Generic Events#l";           
        cm.sendSimple(content);
    } else if (status == 3) {
        if (cm.getText() != null) {
            // player list filter
            status = 0; // redirect to player list (1st status) with text
            return action(1, 0, 0);
        }
        cm.setGetText(null);
        switch (selection) {
            case 7: {
                cm.sendGetText("What is the ID of the item you want to give?");
                break;
            }
            case 8: {
                let relatedPlayers = "\t";
                let pair = target.p.getClient().getCharacterIdentifiers();
                pair.forEach(p => {
                    relatedPlayers += `${p.getRight()}, `;
                });
                pair.clear();
                cm.sendPrev(
                    "#e" + target.p.name + "'s stats#n\r\n"
                    + "\r\nGM Level: " + target.p.getGMLevel()
                    + "\r\nAccount Name: " + target.p.getClient().getAccountName()
                    + "\r\nAccount Id: " + target.p.getAccountID()
                    + "\r\nCharacter Id: " + target.p.getId()
                    + "\r\nOther characters: \r\n" + relatedPlayers
                    + "\r\n\r\nRemote address: " + target.p.getClient().getRemoteAddress()
                    + "\r\n\r\nMACs: " + target.p.getClient().getMacs()
                    + "\r\nHWID: " + target.p.getClient().getHardwareIDs()
                    + "\r\n\r\nCalcualted ATK: " + target.p.calculateMaxBaseDamage(target.p.getTotalWatk())
                    + "\r\nEXP, Meso, Drop rate: " + `${target.p.getExpRate()}x, ${target.p.getMesoRate()}x, ${target.p.getDropRate()}x`
                    + "\r\nCrush rings: " + target.p.getCrushRings()
                    + "\r\nFriendship rings: " + target.p.getFriendshipRings());
                break;
            }
            case 9: {
                let content = "";
                let events = target.p.getGenericEvents();
                events.forEach(g => {
                    target.events.push(g);
                    content += `#L${target.events.length - 1}# ${g.getClass().getSimpleName()} #l`;
                });
                if (target.events.length == 0) cm.sendPrev(`${target.p.name} is not registered in any generic events`);
                else cm.sendSimple(content);
                break;
            }
            default: {
                if (target.inv == undefined && selection > 0 && selection < 7) {
                    target.inv = target.p.getInventory(InventoryType.values()[selection]);
                }
                if (target.inv != undefined) {
                    let content = "";
                    let items = target.inv.list();
                    items.forEach(item => {
                        content += `#L${item.getPosition()}# #v${item.getItemId()}# #l\t`;
                    });
                    if (content.length == 0) cm.sendPrev(`#b${target.inv.getType()}#k inventory is empty`);
                    else cm.sendSimple(content);
                }
                break;
            }
        }
    } else if (status == 4) {
        if (cm.getText() != null) {
            let itemID = parseInt(cm.getText());
            if (!isNaN(itemID)) {
                let type = ItemConstants.getInventoryType(itemID);
                if (type == InventoryType.UNDEFINED) {
                    cm.sendPrev(`${itemID} is not a valid item ID`);
                } else if (InventoryManipulator.checkSpace(target.p.getClient(), itemID, 1, "")) {
                    InventoryManipulator.addById(target.p.getClient(), itemID, 1);
                    target.p.sendMessage(5, `You have received an item from '${player.getName()} in your ${type.name()} inventory`);
                    cm.sendNext(`Given 1 of #b#v${itemID}# #z${itemID}##k to player #b${target.p.name}#k`);
                    status = 1;
                } else {
                    cm.sendOk(`#b${target.p.name}#k's #b${type.name()}#k inventory is full and cannot receive the item`);
                }
            } else {
                status = 1;
                cm.sendNext(`#b'${cm.getText()}'#k is not a number`);
            }
            cm.setGetText(null);
            return;
        }
        if (target.inv == undefined) {
            status = 1;
            return action(1, 0, -1);
        } else if (target.item == undefined && (target.item = target.inv.getItem(selection)) == null) {
            status = 1;
            return cm.sendNext("Item not found. Possibly removed?");
        }
        target.stat = undefined;
        let item = target.item;
        let isEquip = ItemConstants.getInventoryType(target.item.getItemId()) == InventoryType.EQUIP;
        let content = `#b#v${item.getItemId()}# #z${item.getItemId}# (${item.getPosition()} // ${item.getItemId()})\r\n`;
        content += "\r\n#L16#Remove#l";
        content += "\r\n#L17#Send to another player#l";
        content += "\r\n";
        if (isEquip) {
            content += "\r\n#L0#STR: " + item.getStr() + "#l";
            content += "\r\n#L1#DEX: " + item.getDex() + "#l";
            content += "\r\n#L2#INT: " + item.getInt() + "#l";
            content += "\r\n#L3#LUK: " + item.getLuk() + "#l";
            content += "\r\n#L4#WATK: " + item.getWatk() + "#l";
            content += "\r\n#L5#MATK: " + item.getMatk() + "#l";
            content += "\r\n#L6#WDEF: " + item.getWdef() + "#l";
            content += "\r\n#L7#MDEF: " + item.getMdef() + "#l";
            content += "\r\n#L8#HP: " + item.getHp() + "#l";
            content += "\r\n#L9#MP: " + item.getMp() + "#l";
            content += "\r\n#L10#SPEED: " + item.getSpeed() + "#l";
            content += "\r\n#L11#JUMP: " + item.getJump() + "#l";
            content += "\r\n#L12#Ring ID: " + item.getRingId() + "#l";
            content += "\r\n#L18#Eliminations: " + item.getEliminations() + "#l";
        } else {
            content += "\r\n#L13#QUANTITY: " + item.getQuantity() + "#l";
        }
        content += "\r\n#L14#EXPIRATION: " + item.getExpiration() + "#l";
        content += "\r\n#L15#OWNER: " + item.getOwner() + "#l";
        cm.sendSimple(content);
    } else if (status == 5) {
        if (target.stat == undefined) target.stat = selection;
        if (target.stat == 16) {
            InventoryManipulator.removeFromSlot(target.p.getClient(), target.inv.getType(), target.item.getPosition(), target.item.getQuantity(), false);
            target.item = undefined;
            status = 2;
            action(1, 0, 0);
        } else if (target.stat == 17) {
            cm.sendGetText("Who will you give this item to?");
        } else {
            cm.sendGetText(`What will you to set the #b${getUpdateName(target.stat)}#k stat to?`);
        }
    } else if (status == 6) {
        target.error = undefined;
        let stat = target.stat;
        if (stat == 15) { // changing item tag
            updateItem(stat, cm.getText());
            cm.setGetText(null);
            status = 3;
            action(1, 0, 0);
        } else if (stat == 17) {
            status = 3;
            let item = target.item.getItemId();
            let type = ItemConstants.getInventoryType(item);
            let found = GetPlayer(cm.getText());
            cm.setGetText(null);
            if (found == null) {
                cm.sendNext(`Unable to find anybody named '${found.getName()}'`);
            } else if (InventoryManipulator.checkSpace(found.getClient(), item, 1, "")) {
                InventoryManipulator.addById(found.getClient(), item, 1);
                found.sendMessage(5, `You have received an item from '${player.getName()} in your ${type.name()} inventory`);
                cm.sendNext(`Given 1 of #b#v${item}# #z${item}##k to player #b${found.getName()}#k`);
                status = 1;
            } else {
                cm.sendNext(`#b${found.getName()}#k's #b${type.name()}#k inventory is full and cannot receive the item`);
            }
        } else {
            let newval = parseInt(cm.getText()); // the new value for the item stat
            if (isNaN(newval) || newval < 0) {
                target.error = `'#b${cm.getText()}#k'#r is not a number and must be greater than or equal to 0\r\n'`;
                status = 4;
            } else {
                updateItem(stat, newval);
                status = 3;
            }
            cm.setGetText(null);
            action(1, 0, 0);
        }
    }
}

/* ********** functions ********** */
function GetPlayer(id) {
    if (typeof id == "number") return world.getPlayerStorage().get(id);
    else return world.getPlayerStorage().find(p => p.getName().equalsIgnoreCase(id));
}

// Get all online players in the player's world server
// and return them as an array of Player object
function GetPlayers() {
    let ret = [];
    let storage = world.getPlayers();
    storage.forEach(p => {
        let filter = cm.getText();
        if (filter != null) {
            if (!p.getName().toUpperCase().contains(filter.toUpperCase())) {
                return;
            }
        }
        ret.push(new Player(p.getId(), p.getName()));
    });
    storage.clear();
    cm.setGetText(null);
    return ret;
}

// Used to get stat name by selection value
function getUpdateName(stat) {
    switch (stat) {
        case 0: return "STR";
        case 1: return "DEX";
        case 2: return "INT";
        case 3: return "LUK";
        case 4: return "WATK";
        case 5: return "MATK";
        case 6: return "WDEF";
        case 7: return "MDEF";
        case 8: return "HP";
        case 9: return "MP";
        case 10: return "SPEED";
        case 11: return "JUMP";
        case 12: return "RING ID";
        case 13: return "QUANTITY";
        case 14: return "EXPIRATION";
        case 15: return "OWNER";
    }
}

// set the value of the item stat to the newval and update the item
function updateItem(stat, newval) {
         if (stat == 0) target.item.setStr(newval);
    else if (stat == 1) target.item.setDex(newval);
    else if (stat == 2) target.item.setInt(newval);
    else if (stat == 3) target.item.setLuk(newval);
    else if (stat == 4) target.item.setWatk(newval);
    else if (stat == 5) target.item.setMatk(newval);
    else if (stat == 6) target.item.setWdef(newval);
    else if (stat == 7) target.item.setMdef(newval);
    else if (stat == 8) target.item.setHp(newval);
    else if (stat == 9) target.item.setMp(newval);
    else if (stat == 10) target.item.setSpeed(newval);
    else if (stat == 11) target.item.setJump(newval);
    else if (stat == 12) target.item.setRingId(newval);
    else if (stat == 13) target.item.setQuantity(newval);
    else if (stat == 14) target.item.setExpiration(newval);
    else if (stat == 15) target.item.setOwner(newval);
    target.inv.updateItem(target.p.getClient(), target.item);
}

// objects
function Player(id, name) {
    this.id = id;
    this.name = name;
}