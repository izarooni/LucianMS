load("scripts/util_imports.js");
const ShenronSummon = Java.type("com.lucianms.features.summoning.ShenronSummoner");
const MapleStat = Java.type("com.lucianms.client.MapleStat");
const FakePlayer = Java.type("com.lucianms.server.life.FakePlayer");
const ExpTable = Java.type("com.lucianms.constants.ExpTable");
/* izarooni */
let status = 0;
let usernameError = "";

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    let shenron = player.getGenericEvents().stream().filter(e => e instanceof ShenronSummon).findFirst().orElse(null);
    if (!player.isDebug()) {
        if (shenron == null) {
            cm.sendOk("Where am I? Why did you bring me here?");
            return cm.dispose();
        } else if (!player.isGM() && !shenron.isWishing()) {
            cm.sendOk("Who are you? You didn't summon me");
            return cm.dispose();
        }
    }
    if (status == 1) {
        cm.sendSimple("I am Shenron, I shall grant you any wish. Now speak!\r\n#b"
           // + "\r\n#L1#Kill somebody#l"
            + `\r\n#L1#Give me #z${ServerConstants.CURRENCY}##l`
            + "\r\n#L2#Give me more health#l"
            //+ "\r\n#L3#Give me vote points#l"
            + "\r\n#L3#Make me immortal#l"
            + "\r\n#L4#Give me NX#l"
            + "\r\n#L5#Clone me#l");
    } else if (status == 2) {
        switch (selection) {
        //    case 1:
        //        cm.sendGetText(usernameError + "\r\nWho is it that you wish to kill?");
         //       return;
            case 1: {
                let amount = Packages.tools.Randomizer.rand(5, 12);
                if (InventoryModifier.checkSpace(client, ServerConstants.CURRENCY, amount, "")) {
                    cm.gainItem(ServerConstants.CURRENCY, amount, true);
                    cm.sendOk(`Wish granted. I shall give you #b${amount} #z${ServerConstants.CURRENCY}#`);
                } else {
                    cm.sendOk("You currently do not have enough space in your #b" + ItemConstants.getInventoryType(crystal).name() + "#k inventory");
                    return cm.dispose();
                }
                break;
            }
            case 2:
                player.setMaxHp(player.getMaxHp() + 350);
                player.updateSingleStat(MapleStat.MAXHP, player.getMaxHp());
                cm.sendOk("Wish granted. I shall increase your health by 350 points#k");
                break;
            /*case 3:
                player.addPoints("vp", 2);
                cm.sendOk("Wish granted. I shall give you #b2 vote points#k");
                player.dropMessage("You now have " + StringUtil.formatNumber(client.getVotePoints()) + " vote points");
                break;
                */
            case 3:
                player.setImmortalTimestamp(Date.now());
                cm.sendOk("Wish granted. For 60 minutes, you shall be invincible");
                break;
            case 4:
                player.addPoints("nx", 50000);
                cm.sendOk("Wish granted. I shall give you #b50,000 NX#k");
                player.dropMessage("You now have " + StringUtil.formatNumber(player.getCashShop().getCash(1)) + " NX");
                break;
            case 5:
                if (player.getFakePlayer() == null) {
                    let fake = new FakePlayer(player.getName() +"'s Clone");
                    fake.setMap(player.getMap());
                    fake.clonePlayer(player);
                    fake.setFollowing(true);
                    fake.setExpiration(Date.now() + (1000 * 60 * 60));
                    player.setFakePlayer(fake);
                    player.getMap().addFakePlayer(fake);
                    cm.sendOk("Wish granted. I shall give you a clone that will battle with you for 60 minutes");
                } else {
                    cm.sendOk("You already have a clone!");
                    return cm.dispose();
                }
                break;
        }
        if (shenron != null && !player.isDebug()) {
            shenron.wish(player);
        }
        cm.dispose();
    } /*else if (status == 3) {
        let username = cm.getText();
        if (username == null || username.length == 0) {
            usernameError = "#r#eYou must specify a username!#k#n";
        } else {
            let target = cm.findPlayer(username);
            if (target != null && !target.isGM()) {
                target.setHp(0);
                target.updateSingleStat(MapleStat.HP, 0);
                target.sendMessage(5, "'{}' decided to kill you with their Shenron wish!", player.getName());
                cm.sendOk("");
                cm.dispose();
                shenron.wish(player);
                return;
            } else {
                usernameError = "#r#eCould not find any player named \"#b" + username + "#r#e\"#k#n";
            }
        }
        status = 1;
        action(1, 0, 1);*/
}
