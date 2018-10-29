package com.lucianms.client;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author izarooni
 */
public class SpamTracker {

    public class SpamData {
        private long timestamp = 0;
        private int triggers = 0;

        /**
         * @param cooldown the operation cooldown time
         * @return true if the elapsed time since last execution has exceed the cooldown argument
         */
        public boolean testFor(long cooldown) {
            if (System.currentTimeMillis() - timestamp > cooldown) {
                triggers = 0;
                return false;
            }
            triggers++;
            return true;
        }

        public int getTriggers() {
            return triggers;
        }

        public void record() {
            timestamp = System.currentTimeMillis();
        }
    }

    public enum SpamOperation {
        PlayerCommands, NpcTalk, PortalScripts, SkillUsage, MoneyDrop, CashItemUse, PetFeeding, InventorySort, ChangeChannel,
        ItemUse, InventoryMove, IdleHeal, CatchItem
    }

    private final ConcurrentHashMap<SpamOperation, SpamData> tracker = new ConcurrentHashMap<>();

    public SpamData getData(SpamOperation operation) {
        return tracker.computeIfAbsent(operation, op -> new SpamData());
    }
}