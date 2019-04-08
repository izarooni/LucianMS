const MapleShopFactory = Java.type('com.lucianms.server.MapleShopFactory');
/* izarooni */
const Shops = {
    "Magician": [new Shop("Mage Shoes", 10000), new Shop("Mage Overalls", 10001), new Shop("Mage Gloves", 10002), new Shop("Mage Hats", 10003), new Shop("Mage Shield", 10004), new Shop("Wand", 10005), new Shop("Staff", 10006)],
    
    "Thief": [new Shop("Thief Shoes", 10007), new Shop("Thief Pants", 10008), new Shop("Thief Top", 10009), new Shop("Thief Overalls", 10010), new Shop("Thief Gloves", 10011), new Shop("Thief Hats", 10012), new Shop("Thief Shield", 10013), new Shop("Dagger", 10014), new Shop("Claw", 10015), new Shop("Throwing Stars", 10038)],
    
    "Warrior": [new Shop("Warrior Shoes", 10016), new Shop("Warrior Pants", 10017), new Shop("Warrior Top", 10018), new Shop("Warrior Overalls", 10019), new Shop("Warrior Gloves", 10020), new Shop("Warrior Hats", 10021), new Shop("Warrior Shield", 10022), new Shop("One-Handed Axe", 10023), new Shop("Two-Handed Axe", 10024), new Shop("One-Handed Mace", 10025), new Shop("Two-Handed Mace", 10026), new Shop("One-Handed Sword", 10027), new Shop("Two-Handed Sword", 10028), new Shop("Spear", 10029), new Shop("Pole Arm", 10030)],
    
    "Archer": [new Shop("Archer Shoes", 10031), new Shop("Archer Overalls", 10032), new Shop("Archer Gloves", 10033), new Shop("Archer Hats", 10034), new Shop("Bow", 10035), new Shop("a", 10036), new Shop("Arrows/Projectiles", 10037)],
    
    "Common": [new Shop("Earrings", 10039), new Shop("Face Accessory", 10040), new Shop("Cape", 10041), new Shop("Shoes", 10042), new Shop("Hats", 10043), new Shop("Gloves", 10044), new Shop("Overalls", 10045), new Shop("Shields", 10046), new Shop("Weapons", 10047), new Shop("Rocks/Super Megaphones", 10048), new Shop("Potions", 10049), new Shop("Boss Pieces", 10050), new Shop("Maple Weapons", 10051), new Shop("Mounts", 10052), new Shop("Scrolls", 10053), new Shop("Pet Equips", 10055), new Shop("Chairs", 10056), new Shop("Summon Sacks", 10057)]
};
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "Everything in one place. Amazing, isn't it? You won't find a place greater than this.\r\nWell, What can I help you with?\r\n#b"
        let i = 0;
        for (let shop in Shops) {
            content += `\r\n#L${i++}#${shop}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        let i = 0;
        for (let shop in Shops) {
            if (selection == i++) {
                cm.vars = { shop: Shops[shop] };
                break;
            }
        }
        let content = "Here are my wares\r\n#b";
        for (let i = 0; i < cm.vars.shop.length; i++) {
            let shop = cm.vars.shop[i];
            content += `\r\n#L${i}#${shop.name}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 3) {
        let shop = cm.vars.shop[selection];
        let mshop = MapleShopFactory.getInstance().getShop(shop.shopID);
        if (mshop != null) {
            mshop.sendShop(client);
        } else {
            cm.sendOk(`Strange, I wasn't able to open the shop #b${shop.name}`);
        }
        cm.dispose();
    }
}

function Shop(name, shopID) {   
    this.name = name;
    this.shopID = shopID;
}