package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleKeyBinding;
import client.Skill;
import client.SkillFactory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import net.PacketEvent;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;

/**
 * @author izarooni
 */
public class KeymapChangeHandler extends PacketEvent {

    private class ChangedBind {
        int key, action;
        byte type;

        private ChangedBind(int key, byte type, int action) {
            this.key = key;
            this.action = action;
            this.type = type;
        }
    }

    private int action;
    private int itemID;
    private ArrayList<ChangedBind> changes;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        if (slea.available() >= 8) {
            action = slea.readInt();
            switch (action) {
                case 0:
                    int n = slea.readInt();
                    changes = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        changes.add(new ChangedBind(slea.readInt(), slea.readByte(), slea.readInt()));
                    }
                    break;
                case 1:
                case 2:
                    itemID = slea.readInt();
                    break;
            }
        } else {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        switch (action) {
            case 0:
                for (ChangedBind change : changes) {
                    Skill skill = SkillFactory.getSkill(change.action);
                    if (skill != null) {
                        boolean deny = GameConstants.bannedBindSkills(skill.getId());
                        if (deny
                                || (!player.isGM()
                                && (GameConstants.isGMSkills(skill.getId())
                                || !GameConstants.isInJobTree(skill.getId(), player.getJob().getId())))) {
                            return null;
                        }
                        if (player.getSkillLevel(skill) < 1) {
                            continue;
                        }
                    }
                    player.changeKeybinding(change.key, new MapleKeyBinding(change.type, change.action));
                }
                break;
            case 1:
            case 2:
                if (player.getInventory(MapleInventoryType.USE).findById(itemID) != null) {
                    player.changeKeybinding(90 + action, new MapleKeyBinding(7, itemID));
                }
                break;
        }
        return null;
    }
}
