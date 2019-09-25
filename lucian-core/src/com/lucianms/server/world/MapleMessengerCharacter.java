package com.lucianms.server.world;

import com.lucianms.client.MapleCharacter;

/**
 * @author izarooni
 */
public class MapleMessengerCharacter extends SocialMember {

    private int position;

    public MapleMessengerCharacter(MapleCharacter player, int position) {
        super(player);

        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
