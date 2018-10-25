package com.lucianms.server.world;

import java.util.Collection;
import java.util.HashMap;

/**
 * @author izarooni
 */
public class MapleParty {

    private int id;
    private MaplePartyCharacter leader;
    private HashMap<Integer, MaplePartyCharacter> members = new HashMap<>(7);

    public MapleParty(int id, MaplePartyCharacter leader) {
        this.leader = leader;
        this.members.put(this.leader.getId(), this.leader);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MaplePartyCharacter getLeader() {
        return leader;
    }

    public void setLeader(MaplePartyCharacter leader) {
        this.leader = leader;
    }

    public boolean containsMembers(MaplePartyCharacter member) {
        return members.get(member.getId()) != null;
    }

    public void addMember(MaplePartyCharacter member) {
        members.put(member.getId(), member);
    }

    public void removeMember(MaplePartyCharacter member) {
        members.remove(member.getId());
    }


    public void updateMember(MaplePartyCharacter member) {
        if (member.getId() == leader.getId()) {
            leader = member;
        }
        members.put(member.getId(), member);
    }

    public MaplePartyCharacter getMemberById(int id) {
        return members.values().stream().filter(m -> m.getId() == id).findFirst().orElse(null);
    }

    public Collection<MaplePartyCharacter> getMembers() {
        return members.values();
    }

    public void broadcastPacket(byte[] packet) {
        members.values().stream().filter(MaplePartyCharacter::isOnline).map(MaplePartyCharacter::getPlayer).forEach(m -> m.announce(packet));
    }

}
