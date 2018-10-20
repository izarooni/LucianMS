package com.lucianms.features;

import com.lucianms.client.MapleCharacter;

public class PlayerTitles {

    private MapleCharacter player;

    public PlayerTitles(MapleCharacter player) {
        this.player = player;
    }


    public enum Title {
        MASTER_OF_NONE(0),
        LOVED_ONCE(1),
        SENSEI(2),
        BRUTAL(3),
        ANGELIC(4),
        GODLY(5),
        MAJESTIC(6),
        THE_GREATEST(7),
        LOSER(8);

        private int id;

        Title(int id) {
            this.id = id;
        }
    }

    public void unlockTitle(Title title) {
        //player.addTitle(title);
    }

    public void lockTitle(Title title) {

    }

    public void showUnlocked() {

    }

    public void showLocked() {

    }


}
