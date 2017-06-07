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
        handlers.set(RecvOpcode.MOVE_PLAYER.getValue(), MovePlayerHandler.class);
        handlers.set(RecvOpcode.MOVE_LIFE.getValue(), MoveLifeHandler.class);
        handlers.set(RecvOpcode.MOVE_SUMMON.getValue(), MoveSummonHandler.class);
        handlers.set(RecvOpcode.MOVE_PET.getValue(), MovePetHandler.class);
        handlers.set(RecvOpcode.MOVE_DRAGON.getValue(), MoveDragonHandler.class);

        handlers.set(RecvOpcode.CLOSE_RANGE_ATTACK.getValue(), CloseRangeDamageHandler.class);
        handlers.set(RecvOpcode.RANGED_ATTACK.getValue(), RangedAttackHandler.class);
        handlers.set(RecvOpcode.MAGIC_ATTACK.getValue(), MagicDamageHandler.class);
        handlers.set(RecvOpcode.TOUCH_MONSTER_ATTACK.getValue(), TouchMonsterDamageHandler.class);
        handlers.set(RecvOpcode.TAKE_DAMAGE.getValue(), TakeDamageHandler.class);
        handlers.set(RecvOpcode.MOB_DAMAGE_MOB_FRIENDLY.getValue(), MobDamageMobFriendlyHandler.class);

        handlers.set(RecvOpcode.USE_ITEM.getValue(), UseItemHandler.class);
        handlers.set(RecvOpcode.USE_RETURN_SCROLL.getValue(), UseItemHandler.class);

        handlers.set(RecvOpcode.CHAR_INFO_REQUEST.getValue(), ViewCharacterInfoHandler.class);

        handlers.set(RecvOpcode.ITEM_SORT.getValue(), ItemSortHandler.class);
    }
}
