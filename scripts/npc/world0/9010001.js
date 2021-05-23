const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const rewards = [
    new Reward(3012030, 50),
    new Reward(3010690, 50),
    new Reward(3010272, 50),
    new Reward(3010273, 50),
    new Reward(3010055, 50),
    new Reward(3010068, 50),
    new Reward(3015089, 50),
    new Reward(1142559, 50),
    new Reward(2022070, 100)
];
let status = 0;
let Requirement = 4140902;
let selected = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("Hi there. My name is Summer!  I am currently very, very hungry after all the swimming I've done. Can you help get me some #z" + Requirement + "#s? I'll make sure you reward you.\r\n\r\n#eWhere can I find you #z" + Requirement + "#s?#n\r\nI heard some bosses like #rZakum#k or the monsters at the #bFlorina Beach#k might have some on them."
            + "\r\n#r#L0#Rewards for #z" + Requirement + "#s#l#k");
    } else if (status == 2) {
        text = "Item list:";
        for (var i = 0; i < rewards.length; i++) {
            text += "\r\n #L" + i + "##i" + rewards[i].itemID + "# #z" + rewards[i].itemID + "#\t#" + (player.getItemQuantity(Requirement, false) < rewards[i].price ? "r" : "b") + player.getItemQuantity(Requirement, false) + "/" + rewards[i].price + "#i" + Requirement + "##l";
        }
        cm.sendSimple(text);
    } else if (status == 3) {
        if (player.getItemQuantity(Requirement, false) < rewards[selected].price) {
            cm.sendOk("Sorry but it looks like you dont have the requirements for this item!");
            cm.dispose();
        } else {
            selected = selection;
            cm.sendYesNo("Are you sure this is #i" + rewards[selection].itemID + "#\tthe item you want to get?");
        }
    } else if (status == 4) {
        cm.gainItem(Requirement, -rewards[selected].price, true);
        cm.gainItem(rewards[selected].itemID, 1, true);
    } else {
        cm.dispose();
    }
}

function Reward(itemID, price) {
    this.itemID = itemID;
    this.price = price;
}