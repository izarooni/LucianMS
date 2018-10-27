package com.lucianms.cquest;

import com.lucianms.cquest.requirement.CQuestItemRequirement;
import com.lucianms.cquest.reward.*;
import tools.Pair;

import java.util.*;

/**
 * @author izarooni
 */
public final class CQuestMetaData {

    private final CQuestData quest;

    CQuestMetaData(CQuestData quest) {
        this.quest = quest;
    }

    @Override
    public String toString() {
        return String.format("CQuestMetaData{ID=%d, Name=%s}", quest.getId(), quest.getName());
    }

    public int getQuestId() {
        return quest.getId();
    }

    /**
     * Obtain the prerequisite quest ID that's needed to begin the specified quest
     *
     * @return the quest ID of the prerequisite quest
     */
    public int getPreQuestId() {
        return quest.getPreQuestId();
    }

    public int[] getPreQuestIDs() {
        return quest.getPreQuestIds();
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
    public Map<Integer, CQuestItemRequirement.CQuestItem> getToCollect() {
        return Collections.unmodifiableMap(quest.getToCollect().getItems());
    }

    public HashMap<String, List<CQuestReward>> getRewards() {
        HashMap<String, List<CQuestReward>> rewards = new HashMap<>(quest.rewards.size());
        rewards.put("exp", new ArrayList<>());
        rewards.put("expp", new ArrayList<>());
        rewards.put("meso", new ArrayList<>());
        rewards.put("items", new ArrayList<>());
        for (CQuestReward reward : quest.rewards) {
            if (reward instanceof CQuestExpReward) {
                rewards.get("exp").add(reward);
            } else if (reward instanceof CQuestExpPercentageReward) {
                rewards.get("expp").add(reward);
            } else if (reward instanceof CQuestMesoReward) {
                rewards.get("meso").add(reward);
            } else if (reward instanceof CQuestItemReward) {
                rewards.get("items").add(reward);
            }
        }
        return rewards;
    }
}
