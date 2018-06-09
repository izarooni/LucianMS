const HouseManager = Java.type("com.lucianms.helpers.HouseManager");
const MapleCharacter = Java.type("client.MapleCharacter");
const StringUtil = Java.type("tools.StringUtil");

const FEE_INITIAL = 3000000;
const FEE_RENT = 2100000;

/* izarooni */
let status = 0;
let house = HouseManager.getHouse(player.getId());
let errorMsg = "";

if (house != null) {
    let future = house.getBillDate();
    future += 1000 * 60 * 60 * 24 * 7;
    if (Date.now() >= future) {
        house = null;
        HouseManager.removeHouse(player.getId());
        status = -1;
    }
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 0) {
        cm.sendNext("Due to being unable to pay rent after the 1 week grace period, your home has been removed and is now available for other players to purchase", 1);
    } else if (status == 1) {
        let content = "Hey #b#h ##k, I am the house manager.\r\nWhat can I help you with?\r\n#b";
        content += "\r\n#L0#Enter someone's home#l";
        if (house == null) content += "\r\n#L1#Purchase a house#l";
        if (house != null) {
            content += "\r\n#L2#Enter my home#l";
            content += "\r\n#L3#Sell my home#l";
            content += "\r\n#L4#Pay rent#l";
        }
        content += "\r\n#L5#What are homes for?#l";
        cm.sendSimple(content);
    } else if (status == 2) {
        this.operation = selection;
        if (this.operation == 0) {
            cm.sendGetText("Enter the username of the person's home you want to enter.");
        } else if (this.operation == 2) {
            if (house != null) {
                player.changeMap(house.getMap());
                cm.dispose();
            } else {
                cm.sendOk("You do not have a home to return to!");
                cm.dispose();
            }
        } else if (operation == 3) {
            cm.sendOk("Selling houses is currently unavailable.");
            cm.dispose();
        } else if (operation == 4) {
            let rent = house.getBillDate();
            if (rent > Date.now()) {
                let rentDate = new Date(rent).toDateString();
                let elapse = StringUtil.getTimeElapse(rent - Date.now());
                cm.sendOk(`You do not have to pay rent until #b${rentDate}#k\r\nThat's in #b${elapse}#l`);
                cm.dispose();
            } else {
                cm.sendNext("The fee will be exactly #b" + StringUtil.formatNumber(FEE_RENT) + "#k mesos.\r\nAre you ready to pay the rent for this month?");
            }
        } else if (this.operation == 5) {
            cm.sendNext("Homes are unique maps that are separated from the server map loader. You will not see monsters, NPCs or players regardless of the map you select although any person may enter should they know your security password that is also provided upon purchasing.");
        } else if (this.operation == 1) {
            if (house == null && cm.getMeso() >= FEE_INITIAL) {
                cm.sendGetText(errorMsg + "\r\nPlease provide the ID of the map you wish to use for your home.");
            } else {
                cm.sendOk("You do not have enough mesos to purchase a home.");
                cm.dispose();
            }
        }
    } else if (status == 3) {
        if (this.operation == 0) {
            let playerID = MapleCharacter.getIdByName(cm.getText());
            if (playerID > -1) {
                house = HouseManager.getHouse(playerID);
                if (house != null) {
                    if (Date.now() < house.getBillDate()) {
                        cm.sendGetText(`Enter the password for #b${cm.getText()}#k's home
                        `);
                    } else {
                        cm.sendOk(`#b${cm.getText()}#k has not paid rent and therefore entry has been disabled`);
                    }
                } else {
                    cm.sendOk(`#b${cm.getText()}#k does not own a house`);
                    cm.dispose();                    
                }
            } else {
                cm.sendOk(`Unable to find any player with the username #b${cm.getText()}#k`);
                cm.dispose();
            }
        } else if (this.operation == 1) {
            let mapID = parseInt(cm.getText());
            if (isNaN(mapID)) {
                errorMsg = `#r'${cm.getText()}' is not a number#k`;
                status = 1;
                action(1, 0, 1);
            } else {
                this.mapID = mapID;
                cm.sendNext("Are you sure you want to use #b#m" + mapID + "##k for your home?");
            }
        } else if (this.operation == 4) {
            if (cm.getMeso() >= FEE_RENT) {
                cm.gainMeso(-FEE_RENT);
                let rent = HouseManager.updateRent(player.getId());
                let readable = new Date(rent).toDateString();
                cm.sendOk(`Success! Your next bill date will be ${readable}`);
                cm.dispose();
            } else {
                let future = house.getBillDate();
                future += 1000 * 60 * 60 * 24 * 7; // 1 week notice
                cm.sendOk("You do not have enough mesos to pay rent.\r\nFailure to pay rent within #b" + StringUtil.getTimeElapse(future - Date.now()) + "#k will cause you to lose your home");
                cm.dispose();
            }
        } else if (this.operation == 5) {
            cm.sendNext("You may purchase NPCs for your homes with set functions such as private minigames that you can play with party members who are also in your home");
        }
    } else if (status == 4) {
        if (this.operation == 0) {
            if (house.getPassword() == cm.getText()) {
                player.changeMap(house.getMap());
            } else {
                cm.sendOk("The password is incorrect");
            }
            cm.dispose();
        } else if (this.operation == 1) {
            cm.sendGetText("What will be the password for your home?");
        } else if (this.operation == 5) {
            cm.sendNext("There is an initial fee of #b" + StringUtil.formatNumber(FEE_INITIAL) + "#k mesos when purchasing a home.\r\nA recurring monthly fee of #b" + StringUtil.formatNumber(FEE_RENT) + "#k mesos that requires you to be online to pay. Failure to pay after the grace period of #b1 week#k will cause you to lose your home.\r\nIf you no longer wish to own a home, you may choose to sell it, but will not include any internal features that you may have purchased (i.e NPCs).");
            status = 0;
        }
    } else if (status == 5) {
        if (this.operation == 1) {
            this.password = cm.getText();
            cm.sendGetText("Now confirm your password");
        }
    } else if (status == 6) {
        if (cm.getText() == password) {
            house = HouseManager.createHouse(player.getId(), this.mapID, this.password);
            cm.sendNext("Success! Would you like to go to your new home?");
        }
    } else if (status == 7) {
        player.changeMap(house.getMap());
        cm.dispose();
    }
}