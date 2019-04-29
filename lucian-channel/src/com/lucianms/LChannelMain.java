package com.lucianms;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SkillFactory;
import com.lucianms.command.executors.ConsoleCommands;
import com.lucianms.cquest.CQuestBuilder;
import com.lucianms.discord.DiscordConnection;
import com.lucianms.events.*;
import com.lucianms.features.auto.GAutoEventManager;
import com.lucianms.features.scheduled.SOuterSpace;
import com.lucianms.helpers.DailyWorker;
import com.lucianms.helpers.HouseManager;
import com.lucianms.helpers.RankingWorker;
import com.lucianms.io.Config;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.nio.ReceivePacketState;
import com.lucianms.nio.RecvOpcode;
import com.lucianms.nio.server.MapleServerInboundHandler;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.CashShop;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.MapleMonsterInformationProvider;
import com.lucianms.server.maps.tasks.FieldUpdateTask;
import com.lucianms.server.quest.MapleQuest;
import com.lucianms.server.world.MapleWorld;
import com.lucianms.service.InternalChannelCommunicationsHandler;
import com.zaxxer.hikari.HikariDataSource;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Database;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LChannelMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(LChannelMain.class);
    private static InternalChannelCommunicationsHandler communicationsHandler;

    public static void main(String[] args) {
        initReceiveHeaders();
        TaskExecutor.initPoolSize(Runtime.getRuntime().availableProcessors());
        Server.createServer();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Shutting 'er down!");

                System.out.printf("Disposing %d worlds\r\n", Server.getWorlds().size());
                for (MapleWorld world : Server.getWorlds()) {
                    for (MapleChannel channel : world.getChannels()) {
                        if (channel.getServerHandler() != null) {
                            for (MapleCharacter player : channel.getPlayerStorage().values()) {
                                player.saveToDB();
                            }
                            System.out.printf("Saved players in world %d channel %d\r\n", world.getId(), channel.getId());
                            try {
                                channel.getServerHandler().getDiscardServer().close();
                                System.out.printf("World %d channel %d discard server closed\r\n", world.getId(), channel.getId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                try {
                    communicationsHandler.close();
                    System.out.println("Internal server communicator closed");
                } catch (Exception e) {
                    LOGGER.error("Failed to close Netty socket", e);
                }

                TaskExecutor.getExecutor().shutdownNow();
                System.out.println("Task executor shutdown");
            }
        }));

        MapleMonsterInformationProvider.createGlobalCache();

        long timeToTake = System.currentTimeMillis();
        MapleQuest.loadAllQuest();
        CQuestBuilder.loadAllQuests();
        LOGGER.info("Quest data loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));

        timeToTake = System.currentTimeMillis();
        MapleItemInformationProvider.getInstance().getAllItems();
        LOGGER.info("Item data loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));

        timeToTake = System.currentTimeMillis();
        SkillFactory.loadAllSkills();
        LOGGER.info("Skill data loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));

        int achievements = Achievements.loadAchievements();
        LOGGER.info("{} achievements loaded in {}s", achievements, ((System.currentTimeMillis() - timeToTake) / 1000d));

        timeToTake = System.currentTimeMillis();
        int count = HouseManager.loadHouses();
        LOGGER.info("{} houses loaded in {}s", count, ((System.currentTimeMillis() - timeToTake) / 1000d));

        // schedule a daily task
        Calendar tmrw = Calendar.getInstance();
        tmrw.set(Calendar.DATE, tmrw.get(Calendar.DATE) + 1);
        tmrw.set(Calendar.HOUR_OF_DAY, 2);
        tmrw.set(Calendar.MINUTE, 0);
        tmrw.set(Calendar.SECOND, 0);
        tmrw.set(Calendar.MILLISECOND, 0);
        ScheduledExecutorService repeatWorker = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("RepeatWorker"));
        repeatWorker.scheduleWithFixedDelay(new DailyWorker(), (tmrw.getTimeInMillis() - System.currentTimeMillis()), TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        repeatWorker.scheduleWithFixedDelay(new RankingWorker(), RankingWorker.UpdateInterval, RankingWorker.UpdateInterval, TimeUnit.MILLISECONDS);
        LOGGER.info("Repeat worker initialized", (tmrw.getTimeInMillis() - System.currentTimeMillis()));

        CashShop.CashItemFactory.loadCommodities();

        for (MapleWorld world : Server.getWorlds()) {
            HikariDataSource hikari = Database.createDataSource("hikari-world" + world.getId());

            final long repeat = (1000 * 60 * 60) * 4;
            TaskExecutor.createRepeatingTask(() -> GAutoEventManager.startRandomEvent(world), repeat);
            new FieldUpdateTask(world).run();

            world.addScheduledEvent(new SOuterSpace(world));
            world.setHikari(hikari);

            for (MapleChannel channel : world.getChannels()) {
                File eventFolder = new File("scripts/features");
                if (!eventFolder.exists() && eventFolder.mkdirs()) {
                    LOGGER.info("Created folder 'scripts/features'");
                }
                try {
                    MapleServerInboundHandler handler = new MapleServerInboundHandler(ReceivePacketState.ChannelServer, channel.getNetworkAddress().getHostName(), channel.getPort(), new NioEventLoopGroup());
                    channel.setServerHandler(handler);
                    channel.reloadEventScriptManager();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                    return;
                }
                LOGGER.info("World {} channel {} bound to port {}", (world.getId() + 1), (channel.getId()), channel.getPort());
            }
        }

        try {
            Config config = Server.getConfig();
            LOGGER.info("Initializing communications connector");
            communicationsHandler = new InternalChannelCommunicationsHandler(config.getString("ServerHost"), config.getNumber("LoginBasePort").intValue() + 1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
            return;
        }

        DiscordConnection.listen();

        try (Connection con = Server.getConnection()) {
            Database.execute(con, "update accounts set loggedin = 0 where loggedin != 0");
            Database.execute(con, "update characters set hasmerchant = 0");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ConsoleCommands.beginReading();
    }

    private static void initReceiveHeaders() {
        RecvOpcode.PLAYER_LOGGEDIN.clazz = PlayerLoginEvent.class;
        RecvOpcode.CHANGE_MAP.clazz = ChangeMapEvent.class;
        RecvOpcode.CHANGE_CHANNEL.clazz = ChangeChannelEvent.class;
        RecvOpcode.ENTER_CASHSHOP.clazz = EnterCashShopEvent.class;
        RecvOpcode.MOVE_PLAYER.clazz = PlayerMoveEvent.class;
        RecvOpcode.CANCEL_CHAIR.clazz = PlayerChairRemoveEvent.class;
        RecvOpcode.USE_CHAIR.clazz = PlayerChairUseEvent.class;
        RecvOpcode.CLOSE_RANGE_ATTACK.clazz = PlayerDealDamageNearbyEvent.class;
        RecvOpcode.RANGED_ATTACK.clazz = PlayerDealDamageRangedEvent.class;
        RecvOpcode.MAGIC_ATTACK.clazz = PlayerDealDamageMagicEvent.class;
        RecvOpcode.TOUCH_MONSTER_ATTACK.clazz = PlayerDealDamageTouchEvent.class;
        RecvOpcode.TAKE_DAMAGE.clazz = PlayerTakeDamageEvent.class;
        RecvOpcode.GENERAL_CHAT.clazz = PlayerAllChatEvent.class;
        RecvOpcode.CLOSE_CHALKBOARD.clazz = PlayerChalkboardCloseEvent.class;
        RecvOpcode.FACE_EXPRESSION.clazz = PlayerFaceExpressionEvent.class;
        RecvOpcode.NPC_TALK.clazz = NpcTalkEvent.class;
        RecvOpcode.NPC_TALK_MORE.clazz = NpcMoreTalkEvent.class;
        RecvOpcode.STORAGE.clazz = PlayerStorageOperationEvent.class;
        RecvOpcode.HIRED_MERCHANT_REQUEST.clazz = HiredMerchantEvent.class;
        RecvOpcode.ITEM_SORT.clazz = PlayerInventorySortEvent.class;
        RecvOpcode.ITEM_MOVE.clazz = PlayerInventoryMoveEvent.class;
        RecvOpcode.USE_ITEM.clazz = PlayerItemUseEvent.class;
        RecvOpcode.CANCEL_ITEM_EFFECT.clazz = PlayerItemEffectCancelEvent.class;
        RecvOpcode.USE_SUMMON_BAG.clazz = PlayerSummoningBagUseEvent.class;
        RecvOpcode.USE_CASH_ITEM.clazz = PlayerCashItemUseEvent.class;
        RecvOpcode.USE_RETURN_SCROLL.clazz = PlayerItemUseEvent.class;
        RecvOpcode.USE_UPGRADE_SCROLL.clazz = PlayerScrollUseEvent.class;
        RecvOpcode.DISTRIBUTE_AP.clazz = DistributeAPEvent.class;
        RecvOpcode.HEAL_OVER_TIME.clazz = PlayerHealIdleEvent.class;
        RecvOpcode.DISTRIBUTE_SP.clazz = PlayerSkillPointUseEvent.class;
        RecvOpcode.SPECIAL_MOVE.clazz = PlayerSpecialMoveEvent.class;
        RecvOpcode.CANCEL_BUFF.clazz = PlayerBuffCancelEvent.class;
        RecvOpcode.SKILL_EFFECT.clazz = PlayerSkillEffectEvent.class;
        RecvOpcode.MESO_DROP.clazz = PlayerMoneyDropEvent.class;
        RecvOpcode.GIVE_FAME.clazz = PlayerFameGiveEvent.class;
        RecvOpcode.CHAR_INFO_REQUEST.clazz = ViewCharacterInfoEvent.class;
        RecvOpcode.USE_INNER_PORTAL.clazz = PlayerFieldPortalUseEvent.class;
        RecvOpcode.QUEST_ACTION.clazz = QuestOperastionEvent.class;
        RecvOpcode.USE_ITEM_REWARD.clazz = PlayerItemUseRewardEvent.class;
        RecvOpcode.USE_REMOTE.clazz = RemoteGachaponEvent.class;
        RecvOpcode.PARTYCHAT.clazz = PlayerGroupChatEvent.class;
        RecvOpcode.WHISPER.clazz = PlayerWhisperEvent.class;
        RecvOpcode.SPOUSE_CHAT.clazz = PlayerSpouseChatEvent.class;
        RecvOpcode.PLAYER_INTERACTION.clazz = PlayerInteractionEvent.class;
        RecvOpcode.PARTY_OPERATION.clazz = PlayerPartyOperationEvent.class;
        RecvOpcode.DENY_PARTY_REQUEST.clazz = PlayerPartyInviteDenyEvent.class;
        RecvOpcode.GUILD_OPERATION.clazz = PlayerGuildOperationEvent.class;
        RecvOpcode.DENY_GUILD_REQUEST.clazz = PlayerGuildInviteDenyEvent.class;
        RecvOpcode.ADMIN_COMMAND.clazz = AdministratorCommandEvent.class;
        RecvOpcode.BUDDYLIST_MODIFY.clazz = PlayerFriendsListModifyEvent.class;
        RecvOpcode.USE_DOOR.clazz = PlayerMagicDoorUseEvent.class;
        RecvOpcode.CHANGE_KEYMAP.clazz = KeymapChangeEvent.class;
        RecvOpcode.RPS_ACTION.clazz = RockPaperScissorsEvent.class;
        RecvOpcode.RING_ACTION.clazz = PlayerRingActionEvent.class;
        RecvOpcode.ENTER_MTS.clazz = PlayerMTSEnterEvent.class;
        RecvOpcode.ARAN_COMBO_COUNTER.clazz = PlayerAranComboEvent.class;
        RecvOpcode.MOVE_PET.clazz = PetMoveEvent.class;
        RecvOpcode.PET_CHAT.clazz = PlayerPetChatEvent.class;
        RecvOpcode.MOVE_SUMMON.clazz = SummonMoveEvent.class;
        RecvOpcode.SUMMON_ATTACK.clazz = SummonDealDamageEvent.class;
        RecvOpcode.DAMAGE_SUMMON.clazz = PlayerSummonTakeDamageEvent.class;
        RecvOpcode.MOVE_DRAGON.clazz = DragonMoveEvent.class;
        RecvOpcode.MOVE_LIFE.clazz = LifeMoveEvent.class;
        RecvOpcode.MOB_DAMAGE_MOB_FRIENDLY.clazz = MobDealDamageMobFriendlyEvent.class;
        RecvOpcode.NPC_ACTION.clazz = NpcMoveEvent.class;
        RecvOpcode.DAMAGE_REACTOR.clazz = PlayerReactorHitEvent.class;
        RecvOpcode.TOUCHING_REACTOR.clazz = PlayerReactorTouchEvent.class;
        RecvOpcode.MONSTER_CARNIVAL.clazz = MonsterCarnivalEvent.class;
        RecvOpcode.CASHSHOP_OPERATION.clazz = PlayerCashShopOperationEvent.class;
        RecvOpcode.USE_HAMMER.clazz = PlayerHammerUseEvent.class;
        RecvOpcode.USE_CATCH_ITEM.clazz = PlayerCatchItemUseEvent.class;
        RecvOpcode.ACCEPT_FAMILY.clazz = PlayerFamilyInviteAcceptEvent.class;
        RecvOpcode.SCRIPTED_ITEM.clazz = PlayerItemScriptedUseEvent.class;
        RecvOpcode.BBS_OPERATION.clazz = PlayerGuildBoardInteractionEvent.class;
        RecvOpcode.USE_ITEMEFFECT.clazz = PlayerItemEffectUseEvent.class;
        RecvOpcode.CHECK_CASH.clazz = PlayerCashShopCashCheckEvent.class;
        RecvOpcode.SPAWN_PET.clazz = PlayerPetSummonEvent.class;
        RecvOpcode.PET_COMMAND.clazz = PlayerPetCommandEvent.class;
        RecvOpcode.PET_FOOD.clazz = PlayerPetFeedEvent.class;
        RecvOpcode.PET_LOOT.clazz = PlayerPetPickupEvent.class;
        RecvOpcode.AUTO_AGGRO.clazz = PlayerMonsterAutoAggroEvent.class;
        RecvOpcode.MONSTER_BOMB.clazz = PlayerMonsterBombedEvent.class;
        RecvOpcode.USE_SKILL_BOOK.clazz = PlayerSkillBookUseEvent.class;
        RecvOpcode.SKILL_MACRO.clazz = PlayerSkillMacroUpdateEvent.class;
        RecvOpcode.NOTE_ACTION.clazz = NoteActionHandler.class;
        RecvOpcode.USE_MOUNT_FOOD.clazz = PlayerMountFeedEvent.class;
        RecvOpcode.PET_AUTO_POT.clazz = PlayerPetAutoPotEvent.class;
        RecvOpcode.TROCK_ADD_MAP.clazz = PlayerTeleportRockRecordEvent.class;
        RecvOpcode.MOB_DAMAGE_MOB.clazz = MobDealDamageMobEvent.class;
        RecvOpcode.REPORT.clazz = PlayerReportedEvent.class;
        RecvOpcode.AUTO_DISTRIBUTE_AP.clazz = PlayerAutoAssignAPEvent.class;
        RecvOpcode.MAKER_SKILL.clazz = PlayerSkillItemMakerEvent.class;
        RecvOpcode.BEHOLDER.clazz = PlayerBeholderEvent.class;
        RecvOpcode.ADD_FAMILY.clazz = PlayerFamilyInviteEvent.class;
        RecvOpcode.USE_FAMILY.clazz = PlayerFamilyUseEvent.class;
        RecvOpcode.ALLIANCE_OPERATION.clazz = GuildUnionOperationEvent.class;
        RecvOpcode.USE_SOLOMON_ITEM.clazz = PlayerSolomonUseEVent.class;
        RecvOpcode.USE_GACHA_EXP.clazz = PlayerGachaponUsedEvent.class;
        RecvOpcode.USE_DEATHITEM.clazz = PlayerDeathItemUseEvent.class;
        RecvOpcode.USE_MAPLELIFE.clazz = PlayerLifeUseEvent.class;
        RecvOpcode.ITEM_SORT2.clazz = PlayerInventorySortByIDEvent.class;
        RecvOpcode.LEFT_KNOCKBACK.clazz = PlayerLeftKnockbackEvent.class;
        RecvOpcode.REMOTE_STORE.clazz = PlayerRemoteStoreEvent.class;
        RecvOpcode.WEDDING_ACTION.clazz = PlayerWeddingEvent.class;
        RecvOpcode.FREDRICK_ACTION.clazz = PlayerFredrickInteractionEvent.class;
        RecvOpcode.MONSTER_BOOK_COVER.clazz = PlayerMonsterBookCoverEvent.class;
        RecvOpcode.SNOWBALL.clazz = PlayerSnowballEvent.class;
        RecvOpcode.COCONUT.clazz = PlayerCoconutEvent.class;
        RecvOpcode.MESSENGER.clazz = PlayerMessengerEvent.class;
        RecvOpcode.PARTY_SEARCH_START.clazz = PlayerPartySearchBeginEvent.class;
        RecvOpcode.NPC_SHOP.clazz = PlayerNpcShopInteractionEvent.class;
        RecvOpcode.CHANGE_MAP_SPECIAL.clazz = ChangeMapSpecialEvent.class;
        RecvOpcode.ITEM_PICKUP.clazz = ItemPickupEvent.class;
        RecvOpcode.DAMAGE_STAT_CHANGED.clazz = PlayerDamageStatChangedEvent.class;
    }
}
