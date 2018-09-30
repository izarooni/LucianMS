package net;

import com.lucianms.server.events.PongEvent;
import com.lucianms.server.events.channel.*;
import com.lucianms.server.events.login.AccountLoginEvent;
import net.server.channel.handlers.*;
import net.server.handlers.login.SetGenderHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Store packet handler classes that can be indexed in constant time
 * <p>
 * Instead of storing {@code PacketHandler} objects, classes need to tbe stored for data
 * to be used outside of server handlers. See {@link PacketEvent}
 * </p>
 *
 * @author izarooni
 */
public final class PacketManager {

    private static List<Class<? extends PacketEvent>> handlers;

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

    public static Class<? extends PacketEvent> getHandler(int op) {
        return handlers.get(op);
    }

    private static void addPacketHandlers() {
        handlers.set(RecvOpcode.PONG.getValue(), PongEvent.class);
        handlers.set(RecvOpcode.LOGIN_PASSWORD.getValue(), AccountLoginEvent.class);
        handlers.set(RecvOpcode.SET_GENDER.getValue(), SetGenderHandler.class);

        handlers.set(RecvOpcode.CHANGE_MAP.getValue(), ChangeMapEvent.class);
        handlers.set(RecvOpcode.CHANGE_MAP_SPECIAL.getValue(), ChangeMapSpecialEvent.class);

        handlers.set(RecvOpcode.CHAR_INFO_REQUEST.getValue(), ViewCharacterInfoEvent.class);

        handlers.set(RecvOpcode.NPC_TALK.getValue(), NPCTalkEvent.class);
        handlers.set(RecvOpcode.NPC_ACTION.getValue(), NpcMoveEvent.class);

        handlers.set(RecvOpcode.MONSTER_CARNIVAL.getValue(), MonsterCarnivalEvent.class);

        handlers.set(RecvOpcode.DISTRIBUTE_AP.getValue(), DistributeAPEvent.class);
        handlers.set(RecvOpcode.DISTRIBUTE_SP.getValue(), DistributeSPHandler.class);
        handlers.set(RecvOpcode.HEAL_OVER_TIME.getValue(), HealOvertimeHandler.class);

        handlers.set(RecvOpcode.RPS_ACTION.getValue(), RockPaperScissorsEvent.class);

        handlers.set(RecvOpcode.GENERAL_CHAT.getValue(), AllChatEvent.class);
        handlers.set(RecvOpcode.ENTER_CASHSHOP.getValue(), EnterCashShopEvent.class);
        handlers.set(RecvOpcode.CHANGE_CHANNEL.getValue(), ChangeChannelEvent.class);
        handlers.set(RecvOpcode.CHANGE_KEYMAP.getValue(), KeymapChangeHandler.class);
        handlers.set(RecvOpcode.HIRED_MERCHANT_REQUEST.getValue(), HiredMerchantEvent.class);
        handlers.set(RecvOpcode.USE_REMOTE.getValue(), RemoteGachaponHandler.class);
        handlers.set(RecvOpcode.PARTY_SEARCH_START.getValue(), PartySearchStartHandler.class);

        //region movement handlers
        handlers.set(RecvOpcode.MOVE_PLAYER.getValue(), PlayerMoveEvent.class);
        handlers.set(RecvOpcode.MOVE_LIFE.getValue(), MoveLifeEvent.class);
        handlers.set(RecvOpcode.MOVE_SUMMON.getValue(), MoveSummonEvent.class);
        handlers.set(RecvOpcode.MOVE_PET.getValue(), MovePetEvent.class);
        handlers.set(RecvOpcode.MOVE_DRAGON.getValue(), MoveDragonEvent.class);
        //endregion

        //region attack & damage handlers
        handlers.set(RecvOpcode.CLOSE_RANGE_ATTACK.getValue(), CloseRangeDamageEvent.class);
        handlers.set(RecvOpcode.RANGED_ATTACK.getValue(), RangedAttackEvent.class);
        handlers.set(RecvOpcode.MAGIC_ATTACK.getValue(), MagicDamageEvent.class);
        handlers.set(RecvOpcode.TOUCH_MONSTER_ATTACK.getValue(), TouchMonsterDamageEvent.class);
        handlers.set(RecvOpcode.TAKE_DAMAGE.getValue(), TakeDamageEvent.class);
        handlers.set(RecvOpcode.MOB_DAMAGE_MOB_FRIENDLY.getValue(), MobDamageMobFriendlyEvent.class);

        handlers.set(RecvOpcode.DAMAGE_REACTOR.getValue(), ReactorHitEvent.class);
        handlers.set(RecvOpcode.TOUCHING_REACTOR.getValue(), TouchReactorEvent.class);
        //endregion

        //region item mod handlers
        handlers.set(RecvOpcode.USE_ITEM.getValue(), UseItemEvent.class);
        handlers.set(RecvOpcode.USE_RETURN_SCROLL.getValue(), UseItemEvent.class);
        handlers.set(RecvOpcode.USE_UPGRADE_SCROLL.getValue(), ScrollEvent.class);
        handlers.set(RecvOpcode.USE_ITEM_REWARD.getValue(), ItemRewardEvent.class);
        //endregion

        //region inventory handlers
        handlers.set(RecvOpcode.ITEM_SORT.getValue(), ItemSortEvent.class);
        handlers.set(RecvOpcode.ITEM_MOVE.getValue(), ItemMoveEvent.class);
        handlers.set(RecvOpcode.ITEM_PICKUP.getValue(), ItemPickupEvent.class);
        //endregion
    }
}
