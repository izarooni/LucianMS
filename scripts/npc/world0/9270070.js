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
            cm.dispose();
            return;
        } else if (!player.isGM() && !shenron.isWishing()) {
            cm.sendOk("Who are you? You didn't summon me");
            cm.dispose();
            return;
        }
    }
    if (status == 1) {
        cm.sendSimple("I am Shenron, I shall grant you any wish. Now speak!\r\n#b"
            // + "\r\n#L0#Make me rich#l"
            + "\r\n#L1#Kill somebody#l"
            + `\r\n#L2#Give me #z${ServerConstants.CURRENCY}##l`
            + "\r\n#L3#Give me more health#l"
            + "\r\n#L4#Give me vote points#l"
            + "\r\n#L5#Make me immortal#l"
            + "\r\n#L6#Give me NX#l"
            + "\r\n#L7#Clone me#l"
            + "\r\n#L8#Give me a rebirth#l");
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
                let amount = Packages.tools.Randomizer.rand(2, 5);
                if (InventoryModifier.checkSpace(client, ServerConstants.CURRENCY, amount, "")) {
                    cm.gainItem(ServerConstants.CURRENCY, amount, true);
                    cm.sendOk(`Wish granted. I shall give you #b${amount} #z${ServerConstants.CURRENCY}#`);
                } else {
                    cm.sendOk("You currently do not have enough space in your #b" + ItemConstants.getInventoryType(crystal).name() + "#k inventory");
                }
                break;
            }
            case 3:
                player.setMaxHp(player.getMaxHp() + 350);
                player.updateSingleStat(MapleStat.MAXHP, player.getMaxHp());
                cm.sendOk("Wish granted. I shall increase your health by 350 points#k");
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
                    let fake = new FakePlayer(player.getName() +"'s Toy");
                    fake.setMap(player.getMap());
                    fake.clonePlayer(player);
                    player.setFakePlayer(fake);
                    player.getMap().addFakePlayer(fake);
                    fake.setFollowing(true);
                    cm.delayCall(function() {
                        let remove = player.getFakePlayer();
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
            case 8: {

                let levels = 200 - player.getLevel();
                if (levels > 0) {
                    let apGain = levels * 5;
                    player.addPoints("ap", apGain);
                    player.sendMessage("You gained {} Ability Points for {} levels", apGain, levels);
                }
                player.doRebirth();
                break;
            }
        }
        if ((!player.isGM() || player.isDebug()) && shenron != null) shenron.wish(player);
        cm.dispose();
    } else if (status == 3) {
        let username = cm.getText();
        if (username == null || username.length == 0) {
            usernameError = "#r#eYou must specify a username!#k#n";
        } else {
            let target = world.getPlayerStorage().find((p) => p.getName().equalsIgnoreCase(username));
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
        action(1, 0, 1);
    }
}
