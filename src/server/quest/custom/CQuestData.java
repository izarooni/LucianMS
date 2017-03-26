package server.quest.custom;

import client.MapleCharacter;
import server.quest.custom.requirement.CQuestItemRequirement;
import server.quest.custom.requirement.CQuestKillRequirement;
import server.quest.custom.reward.CQuestReward;
import tools.Pair;

import java.util.*;

/**
 * Holds information for a quest (i.e. killed monsters, items looted)
 *
 * @author izarooni
 */
public class CQuestData {

    private final int id;
    private final String name;
    private boolean completed = false;

    /**
     * Map of monster ID to kill requirement and kill progress
     */
    final CQuestKillRequirement toKill = new CQuestKillRequirement();

    /**
     * Map of item ID to quantity requirement and quantity progress
     */
    final CQuestItemRequirement toCollect = new CQuestItemRequirement();

    ArrayList<CQuestReward> rewards = new ArrayList<>();

    CQuestData(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Creates a copy of this quest objects with 0 progress on all requirements
     * <p>
     * Prevents overhead of reading and obtaining data from XML files
     * </p>
     *
     * @param player A player to begin the quest
     */
    CQuestData beginNew(MapleCharacter player) {
        CQuestData ret = new CQuestData(id, name);
        ret.rewards.addAll(rewards); // rewards don't have any changeable variables so we can use the same Objects
        // ID and requirement don't change but reset progress then add to new QuestData
        toKill.forEach((e, v) -> ret.toKill.put(e, new Pair<>(v.left, 0)));
        toCollect.forEach((e, v) -> ret.toCollect.put(e, new Pair<>(v.left, 0)));

        player.getCustomQuests().put(ret.getId(), ret);
        return ret;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * @return true if the quest is completed (i.e. completed and no longer in a state of "ongoing")
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Should be set true if the quest is "submited" to the quest provider and the quest rewards are given
     *
     * @param completed true or false if the quest is completed
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public CQuestKillRequirement getToKill() {
        return toKill;
    }

    public CQuestItemRequirement getToCollect() {
        return toCollect;
    }

    /**
     * Iterate each quest requirement and ensures all progress variables meet the paired requirement variable
     *
     * @return true if all progress variables meet their requirement, false otherwise
     */
    public boolean checkRequirements() {
        // all progress values must be larger than or equal to their paired requirement value
        boolean tcc = toCollect.values().stream().allMatch(p -> p.right >= p.left); // toCollect check
        boolean tkc = toKill.values().stream().allMatch(p -> p.right >= p.left); // toKill check
        toCollect.setFinished(tcc);
        toKill.setFinished(tkc);
        return tcc && tkc;
    }

    /**
     * Complete the quest and give rewards to the specified player
     *
     * @param player player to give the quest rewards to
     * @return true if all rewards can be given at once, false otherwise
     */
    public boolean complete(MapleCharacter player) {
        if (player == null) {
            throw new IllegalArgumentException("Can't given rewards to nobody!");
        } else if (completed) {
            return false;
        }
        if (rewards.stream().allMatch(r -> r.canAccept(player))) {
            completed = true;
            rewards.forEach(r -> r.give(player));
            return true;
        }
        return false;
    }
}
