package server.quest.custom.reward;

import client.MapleCharacter;

/**
 * @author izarooni
 */
public interface CQuestReward {

    boolean canAccept(MapleCharacter player);

    void give(MapleCharacter player);
}
