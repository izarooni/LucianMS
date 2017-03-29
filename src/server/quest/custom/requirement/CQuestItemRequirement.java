package server.quest.custom.requirement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class CQuestItemRequirement extends CQuestRequirement {

    public static class CQuestItem {

        //region fields
        private final int itemId;

        /**
         * quantity of item the player needs to at least have to finish this requirement
         */
        private final int requirement;

        /**
         * the quantity of item the player has of
         * this item can't be more than the requirement amount
         */
        private final boolean unique;

        /**
         * how many of this item the player has collected
         */
        private int progress = 0;

        /**
         * the quest item may only be dropped by this monster if this value is not 0
         */
        private int monsterId = 0;

        /**
         * the quest item may only be dropped by this reactor if this value is not 0
         */
        private int reactorId = 0;

        /**
         * a value of 0 means don't use this variable in drop chance calculations
         */
        private int chance = 0;

        /**
         * if {@link #monsterId} or {@link #reactorId} is not 0, these variables
         * will be used to create drop chance information
         */
        private int minQuantity = 0;
        private int maxQuantity = 0;
        //endregion

        public CQuestItem(int itemId, int requirement, boolean unique) {
            this.itemId = itemId;
            this.requirement = requirement;
            this.unique = unique;
        }

        //region methods
        @Override
        public String toString() {
            // hmmm -- debugging purposes
            return "CQuestItem{" + "itemId=" + itemId + ", requirement=" + requirement + ", unique=" + unique + ", progress=" + progress + ", monsterId=" + monsterId + ", reactorId=" + reactorId + ", chance=" + chance + ", minQuantity=" + minQuantity + ", maxQuantity=" + maxQuantity + '}';
        }

        public boolean isUnique() {
            return unique;
        }

        public int getItemId() {
            return itemId;
        }

        public int getRequirement() {
            return requirement;
        }

        public int getProgress() {
            return progress;
        }

        public int getMonsterId() {
            return monsterId;
        }

        public void setMonsterId(int monsterId) {
            this.monsterId = monsterId;
        }

        public int getChance() {
            return chance;
        }

        public void setChance(int chance) {
            this.chance = chance;
        }

        public int getReactorId() {
            return reactorId;
        }

        public void setReactorId(int reactorId) {
            this.reactorId = reactorId;
        }

        public int getMinQuantity() {
            return minQuantity;
        }

        public void setMinQuantity(int minQuantity) {
            this.minQuantity = minQuantity;
        }

        public int getMaxQuantity() {
            return maxQuantity;
        }

        public void setMaxQuantity(int maxQuantity) {
            this.maxQuantity = maxQuantity;
        }
        //endregion
    }

    private HashMap<Integer, CQuestItem> items = new HashMap<>();

    public Map<Integer, CQuestItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public CQuestItem get(int itemId) {
        return items.get(itemId);
    }

    public void add(CQuestItem item) {
        if (items.containsKey(item.itemId)) {
            throw new RuntimeException(String.format("An item (%d) requirement already exists", item.itemId));
        }
        items.put(item.itemId, item);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Increment an item collection progress.
     * <p>
     * Progress will never decrease below 0 but may increase above the requirement
     * </p>
     *
     * @param itemId ID of item to progress
     * @param inc    amount to increment progress
     */
    public void incrementRequirement(int itemId, int inc) {
        CQuestItem d = items.get(itemId);
        if (d == null) {
            return;
        }
        if (d.progress + inc < 0) {
            // don't go negative
            d.progress = 0;
        } else {
            // going over requirement is ok
            d.progress += inc;
        }
    }
}
