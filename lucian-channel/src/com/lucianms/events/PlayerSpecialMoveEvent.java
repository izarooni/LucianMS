package com.lucianms.events;

import com.lucianms.client.*;
import com.lucianms.client.MapleCharacter.CancelCooldownAction;
import com.lucianms.constants.GameConstants;
import com.lucianms.constants.skills.*;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleStatEffect;
import com.lucianms.server.life.MapleMonster;
import tools.MaplePacketCreator;

import java.awt.*;

/**
 * @author izarooni
 */
public class PlayerSpecialMoveEvent extends PacketEvent {

    private Point point;

    private int skillID;
    private byte skillLevel;

    private int count;
    private int[][] caught;
    private byte direction;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
        skillID = reader.readInt();
        skillLevel = reader.readByte();

        if (skillID == Hero.MONSTER_MAGNET || skillID == Paladin.MONSTER_MAGNET || skillID == DarkKnight.MONSTER_MAGNET) { // Monster Magnet
            count = reader.readInt();
            caught = new int[count][];
            for (int i = 0; i < count; i++) {
                caught[i] = new int[]{reader.readInt(), reader.readByte()};
            }
            direction = reader.readByte();
        }

        if (reader.available() == 5) {
            point = reader.readPoint();
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if ((!GameConstants.isPQSkillMap(player.getMapId()) && GameConstants.isPqSkill(skillID))) {
            return null;
        }
        Skill skill = SkillFactory.getSkill(skillID);
        if (skill == null) {
            getLogger().error("No skill found ID {}", skillID);
            return null;
        }
        int playerSkillLevel = player.getSkillLevel(skill);
        if (skillID % 10000000 == 1010 || skillID % 10000000 == 1011) {
            playerSkillLevel = 1;
            player.setDojoEnergy(0);
            getClient().announce(MaplePacketCreator.setSessionValue("energy", 0));
        }
        if (playerSkillLevel == 0 || playerSkillLevel != skillLevel) {
            if (playerSkillLevel < skill.getMaxLevel()) {
                getClient().announce(MaplePacketCreator.enableActions());
                getLogger().warn("player skill {} level {} does not match packet level {}", skillID, playerSkillLevel, skillLevel);
                return null;
            } else {
                playerSkillLevel = (skillLevel = skill.getMaxLevel());
                player.getSkills().get(skillID).level = skillLevel;
            }
        }

        MapleStatEffect effect = skill.getEffect(playerSkillLevel);
        if ((effect.isMorph() && player.getEffects().containsKey(MapleBuffStat.COMBO_COUNTER))
                || ((skill.getId() == Crusader.COMBO_ATTACK || skill.getId() == DawnWarrior.COMBO_ATTACK) && player.getBuffedValue(MapleBuffStat.MORPH) != null)) {
            return null;
        }
        if (effect.getCooldown() > 0) {
            if (player.skillisCooling(skillID)) {
                return null;
            } else if (skillID != Corsair.BATTLESHIP) {
                getClient().announce(MaplePacketCreator.skillCooldown(skillID, effect.getCooldown()));
                Task task = TaskExecutor.createTask(new CancelCooldownAction(player, skillID), effect.getCooldown() * 1000);
                player.addCooldown(skillID, System.currentTimeMillis(), effect.getCooldown() * 1000, task);
            }
        }
        if (skillID == Hero.MONSTER_MAGNET || skillID == Paladin.MONSTER_MAGNET || skillID == DarkKnight.MONSTER_MAGNET) {
            for (int[] catches : caught) {
                player.getMap().broadcastMessage(player, MaplePacketCreator.showMagnet(catches[0], (byte) catches[1]), false);
                MapleMonster monster = player.getMap().getMonsterByOid(catches[0]);
                if (monster != null) {
                    if (!monster.isBoss()) {
                        monster.switchController(player, monster.isControllerHasAggro());
                    }
                }
            }
            player.getMap().broadcastMessage(player, MaplePacketCreator.showBuffeffect(player.getId(), skillID, player.getSkillLevel(skillID), direction), false);
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        } else if (skillID == Brawler.MP_RECOVERY) {// MP Recovery
            MapleStatEffect ef = skill.getEffect(playerSkillLevel);
            int lose = player.getMaxHp() / ef.getX();
            player.setHp(player.getHp() - lose);
            player.updateSingleStat(MapleStat.HP, player.getHp());
            int gain = lose * (ef.getY() / 100);
            player.setMp(player.getMp() + gain);
            player.updateSingleStat(MapleStat.MP, player.getMp());
        }

        if (skill.getId() == Priest.MYSTIC_DOOR && !player.isGM()) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        if (player.isAlive()) {
            if (skill.getId() != Priest.MYSTIC_DOOR || player.canDoor()) {
                skill.getEffect(playerSkillLevel).applyTo(player, point);
            } else {
                player.message("Please wait 5 seconds before casting Mystic Door again");
                getClient().announce(MaplePacketCreator.enableActions());
            }
        } else {
            getClient().announce(MaplePacketCreator.enableActions());
        }
        return null;
    }
}