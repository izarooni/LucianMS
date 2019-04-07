package com.lucianms.command.executors;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleDisease;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.meta.Occupation;
import com.lucianms.command.CommandWorker;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MobSkill;
import com.lucianms.server.life.MobSkillFactory;
import tools.MaplePacketCreator;

import java.util.concurrent.TimeUnit;

/**
 * @author izarooni
 */
public class OccupationCommands {

    public static boolean execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {
        MapleCharacter player = client.getPlayer();

        if (command.equals("occupation")) {
            Occupation occupation = player.getOccupation();
            if (occupation == null) {
                player.dropMessage("You do not have an occupation");
                return true;
            }
            Occupation.Type type = occupation.getType();
            byte level = occupation.getLevel();
            switch (type) {
                default:
                    player.sendMessage("Your occupation '{}' does not have any commands", type.name());
                    break;
                case Troll:
                    if (level >= 1) player.dropMessage("@warp");
                    if (level >= 2) player.dropMessage("@stun");
                    if (level >= 3) player.dropMessage("@reverse");
                    if (level >= 4) player.dropMessage("@bomb");
                    if (level >= 5) player.dropMessage("@seduce");
                    break;
                case Farmer:
                    player.dropMessage("@autocoin - Automatically converts mesos to server currency when possible");
                    break;
            }
            return true;
        } else if (player.getOccupation().getType() == Occupation.Type.Troll) {
            return ExecuteTroll(client, command, args);
        } else if (player.getOccupation().getType() == Occupation.Type.Farmer) {
            return ExecuteFarmer(client, command, args);
        }
        return false;
    }

    private static boolean ExecuteFarmer(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {
        MapleCharacter player = client.getPlayer();
        if (command.equals("autocoin")) {
            player.setAutoCurrency(!player.isAutoCurrency());
            player.sendMessage("Auto currency is now {}", (player.isAutoCurrency() ? "enabled" : "disabled"));
            player.gainMeso(0, false);
            return true;
        }
        return false;
    }

    private static boolean ExecuteTroll(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {
        MapleCharacter player = client.getPlayer();

        SpamTracker.SpamData spammer = player.getSpamTracker(SpamTracker.SpamOperation.OccTrollDebuff);
        if (spammer.testFor(TimeUnit.SECONDS.toMillis(60))) {
            player.sendMessage("Commands on cooldown for {}s", (System.currentTimeMillis() - spammer.getTimestamp()) / 1000);
            return true;
        }
        if (command.equals("warp")) {
            if (args.length() == 1) {
                MapleCharacter target = getTarget(client, args.get(0));
                if (target != null) {
                    if (!target.getMap().isInstanced() && target.getMap() != client.getWorldServer().getPlayerEvent().getMap()) {
                        player.changeMap(target.getMap());
                        spammer.record();
                    } else {
                        player.sendMessage("You may not warp to this player");
                    }
                }
            } else {
                player.sendMessage("usage: @warp <username>");
            }
        } else if (command.equals("stun")) {
            args.setLength(1);
            giveDebuff(player, command, args.get(0), MobSkillFactory.getMobSkill(123, 1));
            spammer.record();
        } else if (command.equals("reverse")) {
            args.setLength(1);
            giveDebuff(player, command, args.get(0), MobSkillFactory.getMobSkill(120, 1));
            spammer.record();
        } else if (command.equals("bomb")) {
            MapleMonster monster = MapleLifeFactory.getMonster(9300166);
            if (monster != null) {
                monster.getStats().getSelfDestruction().setRemoveAfter(5);
                player.getMap().spawnMonsterOnGroudBelow(monster, player.getPosition());
                spammer.record();
            }
        } else if (command.equals("seduce")) {
            args.setLength(1);
            giveDebuff(player, command, args.get(0), MobSkillFactory.getMobSkill(128, 1));
            spammer.record();
        }
        return true;
    }

    private static void giveDebuff(MapleCharacter player, CommandWorker.Command command, String username, MobSkill skill) {
        MapleCharacter target = getTarget(player.getClient(), username);
        if (target != null) {
            if (skill.getSkillId() == 128) { // seduce
                target.setChair(0);
                target.announce(MaplePacketCreator.cancelChair(-1));
                target.getMap().broadcastMessage(target, MaplePacketCreator.showChair(target.getId(), 0), false);
            }

            MapleDisease disease;
            switch (skill.getSkillId()) {
                default:
                    return;
                case 120:
                    disease = MapleDisease.SEAL;
                    break;
                case 121:
                    disease = MapleDisease.DARKNESS;
                    break;
                case 122:
                    disease = MapleDisease.WEAKEN;
                    break;
                case 123:
                    disease = MapleDisease.STUN;
                    break;
                case 124:
                    disease = MapleDisease.CURSE;
                    break;
                case 125:
                    disease = MapleDisease.POISON;
                    break;
                case 126:
                    disease = MapleDisease.SLOW;
                    break;
                case 128:
                    disease = MapleDisease.SEDUCE;
                    break;
                case 132:
                    disease = MapleDisease.CONFUSE;
                    break;
            }

            target.giveDebuff(disease, skill);
        }
    }

    private static MapleCharacter getTarget(MapleClient client, String username) {
        MapleCharacter target = client.getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(username));
        if (target != null && (!target.isGM() || target.isDebug())) {
            return target;
        } else {
            client.getPlayer().sendMessage("Unable to find any player named '{}'", username);
        }
        return null;
    }
}
