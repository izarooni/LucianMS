load('scripts/util_imports.js');
load('scripts/util_wedding.js');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleRing = Java.type('com.lucianms.client.MapleRing');
/* izarooni */
const nFieldExit   = 680000500;
const nFieldAltar  = 680000210;
const nFieldPhoto  = 680000300;
const nFieldLounge = 680000200;

function init() {}

function setup() {
    let name = "SaintAltar-" + em.getChannel().getId();
    let eim = em.newInstance(name);

    let delay = 1000 * 10;
    eim.setProperty("timestamp", Date.now() + delay);
    eim.schedule("exchangeRings", delay);
    return eim;
}

function playerEntry(eim, player) {
    let sn = eim.getProperty("s"),
        on = eim.getProperty("o");
    let name = player.getName();
    let portalID = (name.equals(sn) || name.equals(on)) ? 2 : 1;
    player.changeMap(nFieldAltar, portalID);

    let timestamp = eim.getProperty("timestamp");
    let elapsed = (timestamp - Date.now()) / 1000;
    player.getClient().announce(MaplePacketCreator.getClock(elapsed));
}

function moveMap(eim, player, map) {
    return true;
}

function playerDisconnected(eim, player) {
    eim.removePlayer(eim, player);
    player.setMap(nFieldExit);
}

function playerExit(eim, player) {
    player.changeMap(nFieldExit);
}

// ==== utility ====

function exchangeRings(eim) {
    let sn = eim.getProperty("s"),
        on = eim.getProperty("o");
    let map = em.getChannel().getMap(nFieldAltar);
    let s = map.getCharacterByName(sn),
        o = map.getCharacterByName(on);
    if (s != null && o != null) {
        let nEngItemID, isBox;
        for (let i = 0; i < ENG_ETC.length; i++) {
            nEngItemID = ENG_ETC[i];
            let itemCount = s.getItemQuantity(nEngItemID, false);
            if (itemCount > 0) {
                isBox = (nEngItemID % 2 == 0) ? false : true;
                break;
            }
        }
        let nWRingID = GetRingFromEtc(nEngItemID);
        let nRingID = MapleRing.create(nWRingID, s, o);

        if (nRingID > 0) {
            
            let sr = s.getRelationship(),
                or = o.getRelationship();
            
            let engagementBoxID = GetEngagementBoxFromEtc(nEngItemID);            
            updateRelationship(s, o, engagementBoxID);
            
            exchangeEngagementItems(s, nWRingID, nEngItemID, nRingID);
            exchangeEngagementItems(o, nWRingID, nEngItemID + (isBox ? 1 : -1), nRingID + 1);
            
            s.saveToDB();
            o.saveToDB();

            s.getWeddingRings().add(MapleRing.load(nRingID));
            o.getWeddingRings().add(MapleRing.load(nRingID + 1));
            
            map.broadcastMessage(6, "The weddings rings will now be exchanged.");
            eim.schedule("transferVisagePhotos", (1000 * 20));
            map.broadcastMessage(MaplePacketCreator.getClock(20));
        } else {
            eim.schedule("dispose", 1000 * 5);
            map.broadcastMessage(1, "[Error]\r\nFailed to create wedding rings."); 
        }
    } else {
        map.broadcastMessage(MaplePacketCreator.getClock(60));
        eim.schedule("dispose", 1000 * 60);
        map.broadcastMessage(6, "Due to the members of the wedding being absent from the room, the wedding will be cancelled.");
    }
}

function updateRelationship(p1, p2, engagementBoxID) {
    let p1r = p1.getRelationship(),
        p2r = p2.getRelationship();
    
    p1r.setGroomId(p1.getId());
    p1r.setBrideId(p2.getId());
    p1r.setStatus(Relationship.Status.Married);
    p1r.setEngagementBoxId(engagementBoxID);
    
    p2r.setGroomId(p1.getId());
    p2r.setBrideId(p2.getId());
    p2r.setStatus(Relationship.Status.Married);
    p2r.setEngagementBoxId(engagementBoxID);
}

function exchangeEngagementItems(player, nWRingID, nEngItemID, nRingID) {
    InventoryModifier.removeById(player.getClient(), InventoryType.ETC, nEngItemID, 1, false, false);
    let eq = new Equip(nWRingID);
    eq.setRingId(nRingID);
    InventoryModifier.addFromDrop(player.getClient(), eq, true);
}

function transferVisagePhotos(eim) {
    let map = em.getChannel().getMap(nFieldAltar);
    map.getCharacters().forEach(p => p.changeMap(nFieldPhoto));
    eim.schedule("dispose", 1000 * 60);
    map = em.getChannel().getMap(nFieldPhoto);
    map.broadcastMessage(MaplePacketCreator.getClock(60));
}

// ==== not used ====

function playerDead(eim, player) {
}

function playerRevive(eim, player) {
}

function leftParty(eim, player) {
}

function disbandParty(eim) {
}

function cancelSchedule() {
    let iter = em.getInstances().iterator();
    while (iter.hasNext()) {
        let eim = iter.next();
        eim.dispose();
        print(`Disposed event script instance ${eim.getName()}`);
    }
}

function dispose(eim) {
    let map = em.getChannel().getMap(nFieldAltar);
    map.getCharacters().forEach(p => eim.removePlayer(p));

    map = em.getChannel().getMap(nFieldPhoto);
    map.getCharacters().forEach(p => eim.removePlayer(p));
    em.removeInstance(eim.getName());
}
