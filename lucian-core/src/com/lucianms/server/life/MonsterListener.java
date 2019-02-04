package com.lucianms.server.life;

import com.lucianms.client.MapleCharacter;

public interface MonsterListener {

    void monsterKilled(MapleCharacter player, int aniTime);
}
