const Equip = Java.type("client.inventory.Equip");
const MaplePacketCreator = Java.type("tools.MaplePacketCreator");
const MapleInventoryType = Java.type("client.inventory.MapleInventoryType");
const MapleInventoryManipulator = Java.type("server.MapleInventoryManipulator");
/* izarooni */
let status = 0;
const features = [
    ListGEvents,
    null,
    ListEquips,
    NewEquip,
    CreateRing,
    EquipRing,
    null,
    ListPortals,
    SpawnPoints,
    null,
    OuterSpace,
    CarnivalStart,
    null,
    TriggerReactors, DespawnReactors, LocateReactors,
    null,
    MonsterData,
    SummonMonster,
    ListEventInstances,
    VisibleMapObjects];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (this.feature == null) {
        if (status === 1) {
            let text = "What can I help you with?\r\n#b";
            for (let i = 0; i < features.length; i++) {
                if (features[i] == null) {
                    text += "\r\n";
                    continue;
                }
                text += "\r\n#L" + i + "#" + features[i].name + "#l";
            }
            cm.sendSimple(text, 2);
        } else if (status === 2) {
            this.feature = features[selection];
            status = 0;
            action(1, 0, 0);
        }
    } else {
        this.feature(selection);
    }
}

function ListGEvents(selection) {
    let content = "";
    player.getGenericEvents().forEach(function(e) {
        content += "\r\n" + e.getClass().getSimpleName();
    });
    cm.sendOk(content);
    cm.dispose();
}

function VisibleMapObjects(selection) {
    let content = "";
    player.getVisibleMapObjects().forEach(function(m) {
            content += "\r\n" + m.getType() + " : " + m.getId();
    });
    cm.sendOk(content);
    cm.dispose();
}

function ListEquips(selection) {
    let equips = player.getInventory(MapleInventoryType.EQUIPPED).list();
    let content = "\r\n";
    equips.forEach(item => {
        content += "\r\n" + item.toString() + "\t#v" + item.getItemId() + "#";
    });
    cm.sendOk(content);
    cm.dispose();
}

function NewEquip(selection) {
    let eq = new Equip(1802056, -149);
    player.getInventory(MapleInventoryType.EQUIPPED).addFromDB(eq);
    player.equipChanged();
    cm.dispose();
}

function CreateRing(selection) {
    if (status === 1) {
        cm.sendGetText("ID of the ring you want to create?");
    } else if (status === 2) {
        this.itemID = parseInt(cm.getText());
        if (isNaN(this.itemID)) {
            status = 0;
            action(1, 0, 0);
        } else {
            cm.sendNext("Are you sure you want to create a ring with this item?\r\n#b#z" + this.itemID + "#");
        }
    } else if (status === 3) {
        cm.sendGetText("Who will be your partner for this ring?");
    } else if (status === 4) {
        this.partner = ch.getPlayerStorage().getCharacterByName(cm.getText());
        if (this.partner != null) {
            let MapleInventoryManipulator = Java.type("server.MapleInventoryManipulator");
            let Equip = Java.type("client.inventory.Equip");
            let MapleRing = Java.type("client.MapleRing");

            let ringID = MapleRing.createRing(this.itemID, player, partner);

            let eq = new Equip(this.itemID, 0);
            eq.setRingId(ringID);
            MapleInventoryManipulator.addFromDrop(partner.getClient(), eq, true);

            eq = new Equip(this.itemID, 0);
            eq.setRingId(ringID + 1);
            MapleInventoryManipulator.addFromDrop(client, eq, true);

            if (this.itemID > 1112012) {
                partner.addFriendshipRing(MapleRing.loadFromDb(ringID));
                player.addFriendshipRing(MapleRing.loadFromDb(ringId + 1));
            } else {
                partner.addCrushRing(MapleRing.loadFromDb(ringID));
                player.addCrushRing(MapleRing.loadFromDb(ringId + 1));
            }

            cm.sendOk("Complete!");
            cm.dispose();
        } else {
            cm.sendOk("The player could not be found.");
            cm.dispose();
        }
    }
}

function EquipRing(selection) {
    let equip =  player.getInventory(MapleInventoryType.EQUIP);
    let equipped = player.getInventory(MapleInventoryType.EQUIPPED);
    if (status === 1) {
        let content = "";
        equip.list().forEach((e) => {
            let itemID = e.getItemId();
            if (itemID >= 1112000 && itemID < 1113000) {
                content += "#L" + e.getPosition() + "##v" + e.getItemId() + "##l\t";
            }
        });
        if (content.length > 0) {
            cm.sendSimple(content);
        } else {
            cm.sendOk("You have no rings to equip!");
            cm.dispose();
        }
    } else if (status === 2) {
        this.ring = equip.getItem(selection);
        let text = "Which slot would you like to place this ring in?\r\n";
        [[-12, "bottom-left (1)"], [-112, "bottom-left (2)"], [-113, "bottom-right"], [-115, "top-left"], [-116, "top-right"]].forEach((n) => {
            if (equipped.getItem(n[0]) == null) {
                text += "\r\n#L" + n[0] + "#" + n[1] + "#l";
            }
        });
        cm.sendSimple(text);
    } else if (status === 3) {
        MapleInventoryManipulator.equip(client, this.ring.getPosition(), selection);
        cm.sendOk("Success!");
        cm.dispose();
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
    if (status === 1) {
        let content = "";
        player.getMap().getPortals().forEach(function(p) {
            content += "\r\n#L" + p.getId() + "#" + p.getName() + "#l";
        });
        cm.sendSimple(content);
        cm.dispose();
    }
}

function SpawnPoints(selection) {
    let content = "";
    player.getMap().getMonsterSpawnPoints().forEach(function(sp) {
        sp.getMonster();
        sp.summonMonster();
        content += "\r\nID:" + sp.getMonster().getId() + ", canSpawn:" + sp.canSpawn(false) + ", Mobile:" + sp.getMonster().isMobile();
    });
    cm.sendOk(content);
    cm.dispose();
}

function MonsterData(selection) {
    if (status === 1) {
        let content = "";
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
    let event = client.getWorldServer().getScheduledEvent("SOuterSpace");
    if (event != null) {
        event.run();
    } else {
        cm.sendOk("Unable to find event");
    }
    cm.dispose();
}

function CarnivalStart(selection) {
    if (status === 1) {
        let mplew = new Packages.tools.data.output.MaplePacketLittleEndianWriter(25);
        mplew.writeShort(Packages.net.SendOpcode.MONSTER_CARNIVAL_DIED.getValue());
        mplew.write(1); //  v1 = (unsigned __int8)CInPacket::Decode1(a1);
        mplew.writeMapleAsciiString("izarooni"); //  CInPacket::DecodeStr(a1, (int)Args);
        mplew.write(1); //  v2 = (unsigned __int8)CInPacket::Decode1(a1);
        player.announce(mplew.getPacket());
        cm.dispose();
    }
}

function SummonMonster(selection) {
    if (status === 1) {
        cm.sendGetText("What monster are you spawning?");
    } else if (status === 2) {
        this.monster = java.lang.Integer.parseInt(cm.getText());
        if (this.monster != null) {
            cm.sendGetText("Where would you like to spawn this monster?\r\nEnter #bx,y#k position (no spaces).\r\n#bExmaple: 59,20");
        } else {
            cm.sendOk("This monster does not exist");
            cm.dispose();
        }
    } else if (status === 3) {
        let split = cm.getText().split(",");
        let x = java.lang.Integer.parseInt(split[0]);
        let y = java.lang.Integer.parseInt(split[1]);
        player.getMap().spawnMonsterOnGroudBelow(Packages.server.life.MapleLifeFactory.getMonster(this.monster), new java.awt.Point(x, y));
        cm.dispose();
    }
}

function LocateReactors(selection) {
    let Locate = function(reactor) {
        let pair = reactor.getReactItem(0);
        if (pair != null) {
            let item = new Packages.client.inventory.Item(pair.getLeft(), 0, pair.getRight());
            player.getMap().spawnItemDrop(player, player, item, reactor.getPosition(), false, true);
        }
    };
    if (status === 1) {
        let text ="\r\n#b";
        text += "\r\n#L" + (player.getMap().getReactors().size() + 1) + "# Activate all #l\r\n"; player.getMap().getReactors().stream().forEach(function(r) {
            text += "\r\n#L" + r.getId() + "#" + r.getName() + " - " + r.getId() + "#l";
        });
        cm.sendSimple(text);
    } else if (status === 2) {
        if (selection === player.getMap().getReactors().size() + 1) {
            let delay = 500;
            player.getMap().getReactors().stream().forEach(function(r) {
                cm.delayCall(function() {
                    Locate(r);
                }, (delay += 300));
            });
        } else {
            let reactor = player.getMap().getReactorById(selection);
            if (reactor != null) {

                Locate(reactor);

                let rItem = reactor.getReactItem(0);
                let information = "Located!\r\n";
                information += "\r\nState: " + reactor.getState();
                information += "\r\nAlive: " + reactor.isAlive();
                information += "\r\nReactItem (Left): " + ((rItem == null) ? "N/A" : rItem.getLeft());
                information += "\r\nReactItem (Right): " + ((rItem == null) ? "N/A" : rItem.getRight());
                cm.sendOk(information);
            } else {
                cm.sendOk("No such reactor");
            }
        }
        cm.dispose();
    }
}
