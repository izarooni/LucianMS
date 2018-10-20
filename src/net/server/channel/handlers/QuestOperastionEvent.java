package net.server.channel.handlers;

import client.MapleCharacter;
import com.lucianms.io.scripting.quest.QuestScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import server.quest.MapleQuest;

/**
 * @author izarooni
 */
public class QuestOperastionEvent extends PacketEvent {

    private byte action;
    private short questID;
    private int npcID;
    private int selection = -1;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        questID = reader.readShort();
        switch (action) {
            case 1:
                npcID = reader.readInt();
                if (reader.available() >= 4) {
                    reader.readInt();
                }
                break;
            case 2:
                npcID = reader.readInt();
                reader.readInt();
                if (reader.available() >= 2) {
                    selection = reader.readShort();
                }
                break;
            case 4:
                npcID = reader.readInt();
                reader.readInt();
                break;
            case 5:
                npcID = reader.readInt();
                reader.readInt();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleQuest quest = MapleQuest.getInstance(questID);
        switch (action) {
            case 1:
                quest.start(player, npcID);
                break;
            case 2:
                if (selection > -1) {
                    quest.complete(player, npcID, selection);
                } else {
                    quest.complete(player, npcID);
                }
                break;
            case 3:
                quest.forfeit(player);
                break;
            case 4:
                if (quest.canStart(player, npcID)) {
                    QuestScriptManager.start(getClient(), questID, npcID);
                }
                break;
            case 5:
                if (quest.canComplete(player, npcID)) {
                    QuestScriptManager.end(getClient(), questID, npcID);
                }
                break;
        }
        return null;
    }
}
