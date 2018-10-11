package com.lucianms.cquest;

import client.MapleCharacter;
import constants.ItemConstants;
import server.MapleInventoryManipulator;
import com.lucianms.cquest.requirement.CQuestItemRequirement;
import com.lucianms.cquest.requirement.CQuestKillRequirement;
import com.lucianms.cquest.reward.CQuestReward;
import tools.MaplePacketCreator;

import java.util.ArrayList;

/**
 * Holds information for a quest (i.e. killed monsters, items looted)
 *
 * @author izarooni
 */
public class CQuestData {

    private final int id;
    private final boolean daily;
    private final String name;
    private boolean completed = false;
    private long completion = -1; // timestamp
    private int preQuestId = -1;
    private int[] preQuestIds = null; // backwards compatibility i want to die
    private int minimumLevel = 0;

    private final CQuestKillRequirement toKill = new CQuestKillRequirement(); // monster kill requirements
    final CQuestItemRequirement toCollect = new CQuestItemRequirement(); // item collect requirements
    ArrayList<CQuestReward> rewards = new ArrayList<>();
    CQuestMetaData metadata;

    CQuestData(int id, String name, boolean daily) {
        this.id = id;
        this.name = name;
        this.daily = daily;
    }

    CQuestMetaData getMetaData() {
        return metadata;
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
        CQuestData ret = new CQuestData(id, name, daily);
        ret.rewards.addAll(rewards); // rewards don't have any changeable variables so we can use the same Objects
        // ID and requirement don't change but reset progress then add to new QuestData
        toKill.getKills().forEach((e, v) -> ret.toKill.add(e, v.left));
        toCollect.getItems().forEach((e, v) -> ret.toCollect.add(new CQuestItemRequirement.CQuestItem(v.getItemId(), v.getRequirement(), v.isUnique())));

        player.getCustomQuests().put(ret.getId(), ret);
        player.dropMessage(5, String.format("New quest started : '%s'", ret.getName()));
        player.announce(MaplePacketCreator.getShowQuestCompletion(0));

        // update progress of item requirements using the player's inventory
        for (Integer itemId : ret.toCollect.getItems().keySet()) {
            int quantity = player.getItemQuantity(itemId, false);
            ret.toCollect.incrementRequirement(itemId, quantity);
        }
        /*
        For quests that have only item retrieval requirements, it's possible the player will already have
        the needed items. It's only worth checking if there are no monster kill requirements because in this case,
        the quest can be finished as soon as it's started
         */
        if (toKill.isEmpty() && !toCollect.isEmpty()) {
            if (checkRequirements()) { // requirements are met
                player.announce(MaplePacketCreator.getShowQuestCompletion(1));
                player.announce(MaplePacketCreator.earnTitleMessage(String.format("Quest '%s' completed!", getName())));
                player.announce(MaplePacketCreator.serverNotice(5, String.format("Quest '%s' completed!", getName())));
            }
        }

        return ret;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Quest needed to be completed before a player may begin this current quest
     *
     * @return id of pre-quest
     */
    public int getPreQuestId() {
        return preQuestId;
    }

    /**
     * @param preQuestId id of pre-quest
     */
    void setPreQuestId(int preQuestId) {
        this.preQuestId = preQuestId;
    }

    public int[] getPreQuestIds() {
        return preQuestIds;
    }

    void setPreQuestIds(int[] preQuestIds) {
        this.preQuestIds = preQuestIds;
    }

    /**
     * @return minimum level requirement to accept the quest
     */
    public int getMinimumLevel() {
        return minimumLevel;
    }

    void setMinimumLevel(int minimumLevel) {
        this.minimumLevel = minimumLevel;
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

    /**
     * @return Timestamp in milliseconds of when the quest was completed
     */
    public long getCompletion() {
        return completion;
    }

    /**
     * @param completion Timestamp in milliseconds of when the quest was completed
     */
    public void setCompletion(long completion) {
        this.completion = completion;
    }

    public boolean isDaily() {
        return daily;
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
        boolean tcc = toCollect.getItems().values().stream().allMatch(p -> p.getProgress() >= p.getRequirement()); // toCollect check
        boolean tkc = toKill.getKills().values().stream().allMatch(p -> p.right >= p.left); // toKill check
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
            throw new IllegalArgumentException("Can't give rewards to nobody!");
        } else if (completed) {
            return false;
        }
        if (rewards.stream().allMatch(r -> r.canAccept(player))) {
            setCompletion(System.currentTimeMillis());
            completed = true;
            toCollect.getItems().forEach((key, value) -> MapleInventoryManipulator.removeById(player.getClient(), ItemConstants.getInventoryType(key), key, value.getRequirement(), false, false));
            rewards.forEach(r -> r.give(player));
            return true;
        }
        return false;
    }
}
