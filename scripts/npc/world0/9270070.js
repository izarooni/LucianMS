load("scripts/util_imports.js");
/* izarooni */
var status = 0;
var usernameError = "";

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("I am Shenron, I shall grant you any wish. Now speak!\r\n#b"
            + "\r\n#L0#Make me rich#l"
            + "\r\n#L1#Kill somebody#l"
            + "\r\n#L2#Give me Crystals#l"
            + "\r\n#L3#Level me#l"
            + "\r\n#L4#Give me vote points#l"
            + "\r\n#L5#Make me immortal#l"
            + "\r\n#L6#Give me NX#l"
            + "\r\n#L7#Clone me#l");
    } else if (status == 2) {
        switch (selection) {
            case 0:
                if (player.getMeso() <= 147483647) {
                    cm.gainMeso(5000000);
                    cm.sendOk("Wish granted. I shall give you #b5 million#k mesos");
                } else {
                    cm.sendOk("You are currently holding too many mesos.");
                }
                break;
            case 1: 
                cm.sendGetText(usernameError + "\r\nWho is it that you wish to kill?");
                return;
            case 2: {
                var crystal = 4260002;
                var amount = Packages.tools.Randomizer.rand(2, 5);
                if (InventoryModifier.checkSpace(client, crystal, amount, "")) {
                    cm.gainItem(crystal, amount, true);
                    cm.sendOk("Wish granted. I shall give you #b" + amount + " #z" + crystal + "# crystals");
                } else {
                    cm.sendOk("You currently do not have enough space in your #b" + ItemConstants.getInventoryType(crystal).name() + "#k inventory");
                }
                break;
            }
            case 3:
                player.levelUp(true);
                cm.sendOk("Wish granted. I shall level you #bonce#k");
                break;
            case 4:
                player.addPoints("vp", 2);
                cm.sendOk("Wish granted. I shall give you #b2 vote points#k");
                player.dropMessage("You now have " + StringUtil.formatNumber(client.getVotePoints()) + " vote points");
                break;
            case 5:
                player.setImmortalTimestamp(Date.now());
                cm.sendOk("Wish granted. For 60 minutes, you shall be invincible");
                break;
            case 6:
                player.addPoints("nx", 50000);
                cm.sendOk("Wish granted. I shall give you #b50,000 NX#k");
                player.dropMessage("You now have " + StringUtil.formatNumber(player.getCashShop().getCash(1)) + " NX");
                break;
            case 7:
                if (player.getFakePlayer() == null) {
                    var fake = new Packages.server.life.FakePlayer(player.getName() +"'s Toy");
                    fake.setMap(player.getMap());
                    fake.clonePlayer(player);
                    player.setFakePlayer(fake);
                    player.getMap().addFakePlayer(fake);
                    fake.setFollowing(true);
                    cm.delayCall(function() {
                        var remove = player.getFakePlayer();
                        if (remove != null) {
                            remove.setFollowing(false);
                            player.setFakePlayer(null);
                            player.getMap().removeFakePlayer(remove);
                        }
                    }, 1000 * 60 * 60);
                    cm.sendOk("Wish granted. I shall give you a clone that will battle with you for 60 minutes");
                } else {
                    cm.sendOk("You already have a clone!");
                }
                break;
        }
        cm.dispose();
    } else if (status == 3) {
        var username = cm.getText();
        if (username == null || username.length == 0) {
            usernameError = "#r#eYou must specify a username!#k#n";
        } else {
            var target = ch.getPlayerStorage().getCharacterByName(username);
            if (target != null && !target.isGM()) {
                target.setHp(0);
                target.updateSingleStat(Packages.client.MapleStat.HP, 0);
                cm.dispose();
                return;
            } else {
                usernameError = "#r#eCould not find any player named \"#b" + username + "#r#e\"#k#n";
            }
        }
        status = 1;
        action(1, 0, 1);
    }
}