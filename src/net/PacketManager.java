package net;

import net.server.channel.handlers.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Store packet handler classes that can be indexed in constant time
 * <p>
 * Instead of storing {@code PacketHandler} objects, classes need to tbe stored for data
 * to be used outside of server handlers. See {@link PacketHandler}
 * </p>
 *
 * @author izarooni
 */
public final class PacketManager {

    private static List<Class<? extends PacketHandler>> handlers;

    static {
        int pLargest = 0;
        for (RecvOpcode op : RecvOpcode.values()) {
            if (op.getValue() > pLargest) {
                pLargest = op.getValue();
            }
        }
        handlers = new ArrayList<>(pLargest); // set capacity
        handlers.addAll(Collections.nCopies(pLargest, null));
        addPacketHandlers();
    }

    public static Class<? extends PacketHandler> getHandler(int op) {
        return handlers.get(op);
    }

    private static void addPacketHandlers() {
        handlers.set(RecvOpcode.CHANGE_MAP.getValue(), ChangeMapHandler.class);
        handlers.set(RecvOpcode.CHANGE_MAP_SPECIAL.getValue(), ChangeMapSpecialHandler.class);

        handlers.set(RecvOpcode.CHAR_INFO_REQUEST.getValue(), ViewCharacterInfoHandler.class);

        handlers.set(RecvOpcode.NPC_TALK.getValue(), NPCTalkHandler.class);

        //region movement handlers
        handlers.set(RecvOpcode.MOVE_PLAYER.getValue(), PlayerMoveHandler.class);
        handlers.set(RecvOpcode.MOVE_LIFE.getValue(), MoveLifeHandler.class);
        handlers.set(RecvOpcode.MOVE_SUMMON.getValue(), MoveSummonHandler.class);
        handlers.set(RecvOpcode.MOVE_PET.getValue(), MovePetHandler.class);
        handlers.set(RecvOpcode.MOVE_DRAGON.getValue(), MoveDragonHandler.class);
        //endregion

        //region attack & damage handlers
        handlers.set(RecvOpcode.CLOSE_RANGE_ATTACK.getValue(), CloseRangeDamageHandler.class);
        handlers.set(RecvOpcode.RANGED_ATTACK.getValue(), RangedAttackHandler.class);
        handlers.set(RecvOpcode.MAGIC_ATTACK.getValue(), MagicDamageHandler.class);
        handlers.set(RecvOpcode.TOUCH_MONSTER_ATTACK.getValue(), TouchMonsterDamageHandler.class);
        handlers.set(RecvOpcode.TAKE_DAMAGE.getValue(), TakeDamageHandler.class);
        handlers.set(RecvOpcode.MOB_DAMAGE_MOB_FRIENDLY.getValue(), MobDamageMobFriendlyHandler.class);
        //endregion

        //region item mod handlers
        handlers.set(RecvOpcode.USE_ITEM.getValue(), UseItemHandler.class);
        handlers.set(RecvOpcode.USE_RETURN_SCROLL.getValue(), UseItemHandler.class);
        handlers.set(RecvOpcode.USE_UPGRADE_SCROLL.getValue(), ScrollHandler.class);
        //endregion

        //region inventory handlers
        handlers.set(RecvOpcode.ITEM_SORT.getValue(), ItemSortHandler.class);
        handlers.set(RecvOpcode.ITEM_MOVE.getValue(), ItemMoveHandler.class);
        handlers.set(RecvOpcode.ITEM_PICKUP.getValue(), ItemPickupHandler.class);
        //endregion
    }
}
