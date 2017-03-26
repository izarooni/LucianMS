package server.quest.custom;

import client.MapleCharacter;
import provider.MapleData;
import provider.MapleDataTool;
import provider.wz.XMLDomMapleData;
import server.quest.custom.reward.CQuestExpReward;
import server.quest.custom.reward.CQuestItemReward;
import server.quest.custom.reward.CQuestMesoReward;
import tools.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Collect quest information from an XML file
 *
 * @author izarooni
 */
public class CQuestBuilder {

    private static final HashMap<Integer, CQuestData> quests = new HashMap<>();

    private CQuestBuilder() {
    }

    public static void loadAllQuests() {
        File file = new File("quests");
        File[] files = file.listFiles();
        if (files != null) {
            for (File qFile : files) {
                try {
                    CQuestData qData = parseFile(qFile);
                    quests.putIfAbsent(qData.getId(), qData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println(quests.size() + " custom quests found and loaded");
    }

    /**
     * Initiate a new quest for the specified player
     *
     * @param player  A player that is beginning the specified quest
     * @param questId ID of the quest to begin
     * @return the data of the quest if it exists, null otherewise
     */
    public static CQuestData beginQuest(MapleCharacter player, int questId) {
        CQuestData qData = quests.get(questId);
        if (qData != null) {
            qData = qData.beginNew(player);
        }
        return qData;
    }

    private static CQuestData parseFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            XMLDomMapleData xml = new XMLDomMapleData(fis, file);
            int questId = MapleDataTool.getInt(xml.getChildByPath("questid"));
            // begin constructing custom quest data
            CQuestData qData = new CQuestData(questId, xml.getName());
            // iterate through monsters to kill, setting all progress to 0
            for (MapleData toKill : xml.getChildByPath("toKill").getChildren()) {
                qData.toKill.putIfAbsent(Integer.parseInt(toKill.getName()), new Pair<>((int) toKill.getData(), 0));
            }
            // iterate through items that are to be collected, setting all progress to 0
            for (MapleData toCollect : xml.getChildByPath("toCollect").getChildren()) {
                qData.toCollect.putIfAbsent(Integer.parseInt(toCollect.getName()), new Pair<>((int) toCollect.getData(), 0));
            }
            // iterate through each reward imgdir child
            for (MapleData rewards : xml.getChildByPath("rewards").getChildren()) {
                switch (rewards.getName()) {
                    case "item": {
                        // equips & loots
                        for (MapleData mapleData : rewards.getChildren()) {
                            // when given, equips will have a quantity of 1 regardless of data retrieved here
                            qData.rewards.add(new CQuestItemReward(Integer.parseInt(mapleData.getName()), (short) mapleData.getData()));
                        }
                        break;
                    }
                    case "meso": {
                        rewards.getChildren().forEach(e -> qData.rewards.add(new CQuestMesoReward((int) e.getData())));
                        break;
                    }
                    case "exp": {
                        rewards.getChildren().forEach(e -> qData.rewards.add(new CQuestExpReward((int) e.getData())));
                        break;
                    }
                }
            }
            return qData;
        }
    }
}
