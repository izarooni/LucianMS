package server.quest.custom;

import server.quest.custom.requirement.CQuestItemRequirement;
import server.quest.custom.reward.CQuestExpReward;
import server.quest.custom.reward.CQuestItemReward;
import server.quest.custom.reward.CQuestMesoReward;
import server.quest.custom.reward.CQuestReward;
import tools.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author izarooni
 */
public final class CQuestMetaData {

    private final CQuestData quest;

    CQuestMetaData(CQuestData quest) {
        this.quest = quest;
    }

    /**
     * Obtain the prerequisite quest ID that's needed to begin the specified quest
     *
     * @return the quest ID of the prerequisite quest
     */
    public int getPreQuestId() {
        return quest.getPreQuestId();
    }

    public int getMinimumLevel() {
        return quest.getMinimumLevel();
    }

    public String getName() {
        return quest.getName();
    }

    /**
     * Get a {@code Map} of item IDs to quantity from the kills requirement of a custom quest
     *
     * @return a {@code Map} of item IDs to amount requirement
     */
    public HashMap<Integer, Integer> getToKill() {
        Map<Integer, Pair<Integer, Integer>> to = quest.getToKill().getKills();

        HashMap<Integer, Integer> kills = new HashMap<>(to.size());
        to.forEach((k, v) -> kills.put(k, v.getLeft()));

        return kills; // should this be unmodifiable?
    }

    /**
     * Get a {@code Map} of item IDs to quantity from the collection requirement of a custom quest
     *
     * @return a {@code Map} of item IDs to quantity requirement
     */
    public HashMap<Integer, CQuestItemRequirement.CQuestItem> getToCollect() {
        Map<Integer, CQuestItemRequirement.CQuestItem> to = quest.getToCollect().getItems();
        return new HashMap<>(to); // should this be unmodifiable?
    }

    public HashMap<String, List<CQuestReward>> getRewards() {
        HashMap<String, List<CQuestReward>> rewards = new HashMap<>(quest.rewards.size());
        rewards.put("exp", new ArrayList<>());
        rewards.put("meso", new ArrayList<>());
        rewards.put("items", new ArrayList<>());
        for (CQuestReward reward : quest.rewards) {
            if (reward instanceof CQuestExpReward) {
                rewards.get("exp").add(reward);
            } else if (reward instanceof CQuestMesoReward) {
                rewards.get("meso").add(reward);
            } else if (reward instanceof CQuestItemReward) {
                rewards.get("items").add(reward);
            }
        }
        return rewards;
    }
}
