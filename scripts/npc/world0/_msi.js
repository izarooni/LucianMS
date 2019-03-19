const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
/* izarooni */
const STAT_RESET = 0;
const STAT_MAX = 32767;
const WeeklyRequirement = [
    [new Requirement(4000016, 1, 10), // sunday
    new Requirement(4000004, 1, 10),
    new Requirement(4000001, 1, 10),
    new Requirement(4000039, 1, 10),
    new Requirement(4000022, 1, 10)],

    [new Requirement(4000329, 1, 10), // monday
    new Requirement(4010007, 1, 10),
    new Requirement(4000331, 1, 10),
    new Requirement(4000328, 1, 10),
    new Requirement(4000324, 1, 10)],

    [new Requirement(4000059, 1, 10), // tuesday
    new Requirement(4000083, 1, 10),
    new Requirement(4000084, 1, 10),
    new Requirement(4000072, 1, 10),
    new Requirement(4000062, 1, 10)],

    [new Requirement(4000143, 1, 10), // wednesday
    new Requirement(4000109, 1, 10),
    new Requirement(4000130, 1, 10),
    new Requirement(4000114, 1, 10),
    new Requirement(4000144, 1, 10)],

    [new Requirement(4000229, 1, 10), // thursday
    new Requirement(4000231, 1, 10),
    new Requirement(4000242, 1, 10),
    new Requirement(4000266, 1, 10),
    new Requirement(4000276, 1, 10)],

    [new Requirement(4000280, 1, 10), // friday
    new Requirement(4000282, 1, 10),
    new Requirement(4000288, 1, 10),
    new Requirement(4000287, 1, 10),
    new Requirement(4000166, 1, 10)],

    [new Requirement(4000164, 1, 10), // saturday
    new Requirement(4000154, 1, 10),
    new Requirement(4000157, 1, 10),
    new Requirement(4000179, 1, 10)]
];
let ItemRequirements = WeeklyRequirement[new Date().getDay()];
let status = 0;

for (let i = 0; i < ItemRequirements.length; i++) {
    let req = ItemRequirements[i];
    req.quantity = (player.getMsiCreations() + 1) * req.modifier;
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("As you become stronger, your equips will need to match your power. I can help you upgrade your wares so you may achieve greater heights.\r\n#b"
            + "\r\n#L0#What do I need to bring you?#l"
            + "\r\n#L1#I'm ready to upgrade#l");
    } else if (status == 2) {
        if (selection == 0) {
            status = 0;
            let content = "You can't become stronger without increasing your stats. This can be achieved by leveling-up via completing quests or slaying monsters.\r\n"
                + "Please make sure all requirements are met\r\n"
                + "\r\n#k#eSTR: " + checkStat(player.getStr()) + " / 32767"
                + "\r\n#k#eDEX: " + checkStat(player.getDex()) + " / 32767"
                + "\r\n#k#eINT: " + checkStat(player.getInt()) + " / 32767"
                + "\r\n#k#eLUK: " + checkStat(player.getLuk()) + " / 32767";
            for (let i = 0; i < ItemRequirements.length; i++) {
                let req = ItemRequirements[i];
                let count = player.getItemQuantity(req.itemID, false);
                let ts = (count < req.quantity) ? "#r" : "#b";
                content += `\r\n${ts}#v${req.itemID}# #z${req.itemID}# [ ${count} / ${req.quantity} ]`;
            }
            cm.sendNext(content);
        } else if (selection == 1) {
            let content = "What item would you like to make an MSI?"
                + "\r\nEquips you are currently wearing:\r\n\r\n";
            let items = player.getInventory(InventoryType.EQUIPPED).list();
            items.forEach(item => {
                content += `#L${item.getPosition()}##v${item.getItemId()}##l\t`;
            });
            content += "\r\n\r\nEquips currently in your inventory:\r\n\r\n";
            items = player.getInventory(InventoryType.EQUIP).list();
            items.forEach(item => {
                content += `#L${item.getPosition()}##v${item.getItemId()}##l\t`;
            });
            cm.sendSimple(content);
        }
    } else if (status == 3) {
        let inventory = player.getInventory(selection < 0 ? InventoryType.EQUIPPED : InventoryType.EQUIP);
        let item = inventory.getItem(selection);
        if (item == null) {
            cm.dispose();
            return;
        }
        cm.vars = { _item: item };
        let content = `You haven chosen: #v${item.getItemId()}# #b#z${item.getItemId()}##k\r\n Are you certain you want to create an MSI with this?`;
        cm.sendYesNo(content);
    } else if (status == 4) {
        let canCreate = player.getStr() == STAT_MAX &&
            player.getStr() == STAT_MAX &&
            player.getInt() == STAT_MAX &&
            player.getLuk() == STAT_MAX;
        for (let i = 0; i < ItemRequirements.length; i++) {
            let req = ItemRequirements[i];
            if (player.getItemQuantity(req.itemID, false) < req.quantity) {
                canCreate = false;
                break;
            }
        }
        if (canCreate) {
            let selectedItem = cm.vars._item;

            player.setMsiCreations(player.getMsiCreations() + 1);

            if (!player.isDebug()) {
                for (let i = 0; i < ItemRequirements.length; i++) {
                    let req = ItemRequirements[i];
                    cm.gainItem(req.itemID, -req.quantity, true);
                }
                player.setStr(STAT_RESET);
                player.setDex(STAT_RESET);
                player.setInt(STAT_RESET);
                player.setLuk(STAT_RESET);
                player.updateSingleStat(MapleStat.STR, STAT_RESET);
                player.updateSingleStat(MapleStat.DEX, STAT_RESET);
                player.updateSingleStat(MapleStat.INT, STAT_RESET);
                player.updateSingleStat(MapleStat.LUK, STAT_RESET);
            }

            selectedItem.setStr(STAT_MAX);
            selectedItem.setDex(STAT_MAX);
            selectedItem.setInt(STAT_MAX);
            selectedItem.setLuk(STAT_MAX);

            let mods = new java.util.ArrayList();
            mods.add(new ModifyInventory(3, selectedItem));
            mods.add(new ModifyInventory(0, selectedItem));
            client.announce(MaplePacketCreator.modifyInventory(true, mods));
            mods.clear();
            if (selectedItem.getPosition() < 0) {
                player.equipChanged();
            }
            cm.sendOk("Magnificent! I've finished upgrading your equip.\r\nThe next time you make an MSI you will need #btwice the amount of resources#k.\r\nPlease remember that and do come back soon~");
        } else {
            cm.sendOk("You cannot make an MSI right now.");
        }
        cm.dispose();
    }
}

function Requirement(itemID, quantity, modifier) {
    this.itemID = itemID;
    this.quantity = quantity;
    this.modifier = modifier;
}

function checkStat(stat) {
    return (stat == STAT_MAX) ? `#b${stat}` : `#r${stat}`;
}