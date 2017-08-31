package server.quest.custom;

import client.MapleCharacter;
import lang.DuplicateEntryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.MapleData;
import provider.MapleDataTool;
import provider.wz.MapleDataType;
import provider.wz.XMLDomMapleData;
import server.quest.custom.requirement.CQuestItemRequirement;
import server.quest.custom.reward.CQuestExpReward;
import server.quest.custom.reward.CQuestItemReward;
import server.quest.custom.reward.CQuestMesoReward;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Collect quest information from an XML file
 *
 * @author izarooni
 */
public class CQuestBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CQuestBuilder.class);

    private static HashMap<Integer, CQuestData> quests = new HashMap<>();

    private CQuestBuilder() {
    }

    public static void loadAllQuests() {
        if (!quests.isEmpty()) {
            quests.clear();
            quests = new HashMap<>();
        }
        File file = new File("quests");
        File[] files = file.listFiles();
        if (files != null) {
            for (File qFile : files) {
                try {
                    CQuestData qData = parseFile(qFile);
                    if (quests.containsKey(qData.getId())) {
                        throw new DuplicateEntryException(String.format("Custom quest with ID %d already exists", qData.getId()));
                    }
                    quests.put(qData.getId(), qData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        LOGGER.info("{} custom quests loaded", quests.size());
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
            return qData.beginNew(player);
        }
        throw new NullPointerException(String.format("Unable to begin quest; Invalid quest ID specified '%d'", questId));
    }

    /**
     * Obtain the prerequisite quest ID that's needed to begin the specified quest
     *
     * @param questId ID of the quest to check
     * @return the quest ID of the prerequisite quest
     */
    public static int getPreQuest(int questId) {
        CQuestData qData = quests.get(questId);
        if (qData != null) {
            return qData.getPreQuestId();
        }
        throw new NullPointerException(String.format("Unable to obtain pre-quest; Invalid quest ID specified '%d'", questId));
    }

    private static CQuestData parseFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            XMLDomMapleData xml = new XMLDomMapleData(fis, file);
            int questId = MapleDataTool.getInt(xml.getChildByPath("info/questId"));
            int pId = MapleDataTool.getInt(xml.getChildByPath("info/preQuest"), -1);
            // begin constructing custom quest data
            CQuestData qData = new CQuestData(questId, xml.getName());
            qData.setPreQuestId(pId);
            // iterate through monsters to kill, setting all progress to 0
            if (xml.getChildByPath("toKill") != null) {
                for (MapleData toKill : xml.getChildByPath("toKill").getChildren()) {
                    int monsterId = MapleDataTool.getInt(toKill.getChildByPath("monsterId"));
                    int amount = MapleDataTool.getInt(toKill.getChildByPath("amount"), -1);
                    if (monsterId == 0 || amount == -1) {
                        LOGGER.warn("Invalid monster kill requirement for quest {}", qData.getName());
                        continue;
                    }
                    qData.getToKill().add(monsterId, amount);
                }
            }

            if (xml.getChildByPath("toCollect") != null) {
                // iterate through items that are to be collected, setting all progress to 0
                for (MapleData toCollect : xml.getChildByPath("toCollect").getChildren()) {
                    // regular item drops
                    int reactorId = MapleDataTool.getInt(toCollect.getChildByPath("reactorId"));
                    int monsterId = MapleDataTool.getInt(toCollect.getChildByPath("monsterId"));
                    int itemId = MapleDataTool.getInt(toCollect.getChildByPath("itemId"));
                    int quantity = MapleDataTool.getInt(toCollect.getChildByPath("quantity"));
                    int chance = MapleDataTool.getInt(toCollect.getChildByPath("chance"));
                    int minQuantity = MapleDataTool.getInt(toCollect.getChildByPath("minDrop"));
                    int maxQuantity = MapleDataTool.getInt(toCollect.getChildByPath("maxDrop"));
                    if (itemId == 0 || quantity == 0) {
                        LOGGER.warn("Invalid item requirement for quest {}", qData.getName());
                        continue;
                    }
                    CQuestItemRequirement.CQuestItem qItem = new CQuestItemRequirement.CQuestItem(itemId, quantity, false);
                    qItem.setMonsterId(monsterId);
                    qItem.setReactorId(reactorId);
                    qItem.setChance(chance);
                    qItem.setMinQuantity(minQuantity);
                    qItem.setMaxQuantity(maxQuantity);
                    qData.toCollect.add(qItem);
                }
            }
            if (xml.getChildByPath("rewards") != null) {
                for (MapleData rewards : xml.getChildByPath("rewards").getChildren()) {
                    if (rewards.getType() == MapleDataType.INT) {
                        if (rewards.getName().equalsIgnoreCase("meso")) {
                            qData.rewards.add(new CQuestMesoReward(MapleDataTool.getInt(rewards)));
                        } else if (rewards.getName().equalsIgnoreCase("exp")) {
                            qData.rewards.add(new CQuestExpReward(MapleDataTool.getInt(rewards)));
                        }
                    }
                }
            }
            if (xml.getChildByPath("rewards/items") != null) {
                for (MapleData items : xml.getChildByPath("rewards/items").getChildren()) {
                    int itemId = MapleDataTool.getInt(items.getChildByPath("itemId"));
                    short quantity = (short) MapleDataTool.getInt(items.getChildByPath("quantity"));
                    if (itemId == 0 || quantity == 0) {
                        LOGGER.warn("Invalid reward item for quest {}", qData.getName());
                        continue;
                    }
                    qData.rewards.add(new CQuestItemReward(itemId, quantity));
                }
            }
            return qData;
        }
    }
}
