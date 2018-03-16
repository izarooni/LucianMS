/* izarooni */
var status = 0;
var features = [
    ListPortals,
    SpawnPoints,
    MonsterData,
    OuterSpace,
    CarnivalStart,
    TriggerReactors, DespawnReactors, LocateReactors,
    SummonMonster,
    ListEventInstances];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (this.feature == null) {
        if (status == 1) {
            var text = "What can I help you with?\r\n#b";
            for (var i = 0; i < features.length; i++) {
                text += "\r\n#L" + i + "#" + features[i].name + "#l";
            }
            cm.sendSimple(text, 2);
        } else if (status == 2) {
            this.feature = features[selection];
            status = 0;
            action(1, 0, 0);
        }
    } else {
        this.feature(selection);
    }
}

function ListEventInstances(selection) {
    let content = "A list of event instances:\r\n";
    let manager = ch.getEventScriptManager().getManager("KerningTrain");
    manager.getInstances().forEach((as) => {
        content += "\r\n" + as.getName();
    });
    cm.sendOk(content);
    cm.dispose();
}

function TriggerReactors(selection) {
    player.getMap().getReactors().forEach(function(reactor) {
        player.announce(Packages.tools.MaplePacketCreator.triggerReactor(reactor, 0));
    });
    player.sendMessage("Done!");
    cm.dispose();
}

function DespawnReactors(selection) {
    player.getMap().getReactors().forEach(function(reactor) {
        reactor.sendDestroyData(client);
        player.sendMessage("{} {} {}", reactor.getObjectId(), reactor.getState(), reactor.getPosition());
    });
    cm.dispose();
}

function ListPortals(selection) {
    if (status == 1) {
        var content = "";
        player.getMap().getPortals().forEach(function(p) {
            content += "\r\n#L" + p.getId() + "#" + p.getName() + "#l";
        });
        cm.sendSimple(content);
        cm.dispose();
    }
}

function SpawnPoints(seleciton) {
    var content = "";
    player.getMap().getMonsterSpawnPoints().forEach(function(sp) {
        content += "\r\n" + sp.getMonster().getId() + ", " + sp.shouldSpawn() + ", " + sp.getMonster().isMobile();
    });
    cm.sendOk(content);
    cm.dispose();
}

function MonsterData(selection) {
    if (status == 1) {
        var content = "";
        player.getMap().getMonsters().forEach(function(m){
            content += "\r\nID: " + m.getId()+ ", Name: " + m.getName() + ", HP: " + m.getHp();
            if (m.getHp() < 1) {
                player.getMap().killMonster(m, player, true);
            }
        });
        cm.sendOk(content);
        cm.dispose();
    }
}

function OuterSpace(selection) {
    var event = client.getWorldServer().getScheduledEvent("SOuterSpace");
    if (event != null) {
        event.run();
    } else {
        cm.sendOk("Unable to find event");
    }
    cm.dispose();
}

function CarnivalStart(selection) {
    if (status == 1) {
        var mplew = new Packages.tools.data.output.MaplePacketLittleEndianWriter(25);
        mplew.writeShort(Packages.net.SendOpcode.MONSTER_CARNIVAL_START.getValue());
        mplew.write(player.getTeam()); //team
        mplew.writeShort(player.getCP()); //Obtained CP - Used CP
        mplew.writeShort(player.getObtainedCP()); //Total Obtained CP

        mplew.writeShort(0); //Obtained CP - Used CP of the team
        mplew.writeShort(0); //Total Obtained CP of the team
        mplew.writeShort(0); //Obtained CP - Used CP of the team
        mplew.writeShort(0); //Total Obtained CP of the team
        mplew.writeShort(0); //Probably useless nexon shit
        mplew.writeLong(0); //Probably useless nexon shit
        player.announce(mplew.getPacket());
        cm.sendNext("Enabled~\r\n\r\nWant to disable?");
    } else if (status == 2) {
        var mplew = new Packages.tools.data.output.MaplePacketLittleEndianWriter();
        mplew.writeShort(Packages.net.SendOpcode.MONSTER_CARNIVAL_LEAVE.getValue());
        mplew.write(0); //Something
        mplew.write(player.getTeam()); //Team
        mplew.writeMapleAsciiString(player.getName()); //Player name
        player.announce(mplew.getPacket());
        cm.sendOk("Disabled~");
        cm.dispose();
    }
}

function SummonMonster(selection) {
    if (status == 1) {
        cm.sendGetText("What monster are you spawning?");
    } else if (status == 2) {
        this.monster = java.lang.Integer.parseInt(cm.getText());
        if (this.monster != null) {
            cm.sendGetText("Where would you like to spawn this monster?\r\nEnter #bx,y#k position (no spaces).\r\n#bExmaple: 59,20");
        } else {
            cm.sendOk("This monster does not exist");
            cm.dispose();
        }
    } else if (status == 3) {
        var split = cm.getText().split(",");
        var x = java.lang.Integer.parseInt(split[0]);
        var y = java.lang.Integer.parseInt(split[1]);
        player.getMap().spawnMonsterOnGroudBelow(Packages.server.life.MapleLifeFactory.getMonster(this.monster), new java.awt.Point(x, y));
        cm.dispose();
    }
}

function LocateReactors(selection) {
    var Locate = function(reactor) {
        var pair = reactor.getReactItem(0);
        if (pair != null) {
            var item = new Packages.client.inventory.Item(pair.getLeft(), 0, pair.getRight());
            player.getMap().spawnItemDrop(player, player, item, reactor.getPosition(), false, true);
        }
    };
    if (status == 1) {
        var text ="\r\n#b";
        text += "\r\n#L" + (player.getMap().getReactors().size() + 1) + "# Activate all #l\r\n"; player.getMap().getReactors().stream().forEach(function(r) {
            text += "\r\n#L" + r.getId() + "#" + r.getName() + " - " + r.getId() + "#l";
        });
        cm.sendSimple(text);
    } else if (status == 2) {
        if (selection == player.getMap().getReactors().size() + 1) {
            var delay = 500;
            player.getMap().getReactors().stream().forEach(function(r) {
                cm.delayCall(function() {
                    Locate(r);
                }, (delay += 300));
            });
        } else {
            var reactor = player.getMap().getReactorById(selection);
            if (reactor != null) {

                Locate(reactor);

                var information = "Located!\r\n";
                information += "\r\nState: " + reactor.getState();
                information += "\r\nAlive: " + reactor.isAlive();
                information += "\r\nReactItem (Left): " + reactor.getReactItem(0).getLeft();
                information += "\r\nReactItem (Right): " + reactor.getReactItem(0).getRight();
                cm.sendOk(information);
            } else {
                cm.sendOk("No such reactor");
            }
        }
        cm.dispose();
    }
}
