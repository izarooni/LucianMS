java
const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
let status = 0;
let waterMelon=2001000;
 
 
function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendOk("Hi. My name is Summer and I am currently very, very thirsthy after all the swimming I've done. Can you help get me some watermelons? I'll make sure you reward you."
        +"\r\nItem list:"
        +"\r\n#i2001000# "+player.getItemQuantity(waterMelon,false)+" / 70 watermelons"
        +"\r\n#i2001000# "+player.getItemQuantity(waterMelon,false)+" / 50 watermelons");
    } 
        cm.dispose();
}