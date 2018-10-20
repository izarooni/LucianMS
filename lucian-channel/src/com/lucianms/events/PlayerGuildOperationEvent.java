package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import com.lucianms.constants.ServerConstants;
import com.lucianms.server.Server;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.guild.MapleGuildResponse;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * @author izarooni
 */
public class PlayerGuildOperationEvent extends PacketEvent {

    private boolean isGuildNameAcceptable(String name) {
        return name.matches(Pattern.compile("[a-zA-Z0-9]{4,13}]").pattern());
    }

    private void respawnPlayer(MapleCharacter mc) {
        mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(mc.getId()), false);
        mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
    }

    private class Invited {
        public String username;
        public int guildID;
        public long expiration;

        Invited(String n, int id) {
            username = n.toLowerCase();
            guildID = id;
            expiration = System.currentTimeMillis() + 60 * 60 * 1000;
        }
    }

    private static ArrayList<Invited> Invitations = new ArrayList<>();
    private static long NextPurgeTimestamp = System.currentTimeMillis() + 20 * 60 * 1000;
    private static final int GuildHQ = 200000301;

    private String name;
    private String content;
    private String[] ranks;
    private byte action;
    private byte newRank;
    private byte backgroundColor;
    private byte logoColor;
    private short background;
    private short logo;
    private int guildID;
    private int playerID;

    @Override
    public void clean() {
        ranks = null;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        MapleCharacter player = getClient().getPlayer();
        action = reader.readByte();
        switch (action) {
            case 0x02: // create
                name = reader.readMapleAsciiString();
                break;
            case 0x05: // invite
                name = reader.readMapleAsciiString();
                if (player.getGuildId() <= 0 || player.getGuildRank() > 2) {
                    setCanceled(true);
                }
                break;
            case 0x06: // join
                guildID = reader.readInt();
                playerID = reader.readInt();
                if (player.getGuildId() > 0) {
                    setCanceled(true);
                }
                break;
            case 0x07: // leave
                playerID = reader.readInt();
                name = reader.readMapleAsciiString();
                if (player.getGuildId() <= 0) {
                    setCanceled(true);
                }
                break;
            case 0x08: // expel
                playerID = reader.readInt();
                name = reader.readMapleAsciiString();
                if (player.getGuildRank() > 2 || player.getGuildId() <= 0) {
                    setCanceled(true);
                }
                break;
            case 0x0d: // rank name change
                ranks = new String[5];
                for (int i = 0; i < 5; i++) {
                    String s = reader.readMapleAsciiString();
                    if (s.length() < 1 || s.length() > 10) {
                        setCanceled(true);
                        return;
                    }
                    ranks[i] = s;
                }
                if (player.getGuildId() <= 0 || player.getGuildRank() != 1) {
                    setCanceled(true);
                }
                break;
            case 0x0e: // rank change
                playerID = reader.readInt();
                newRank = reader.readByte();
                if (player.getGuildRank() > 2 || newRank <= 2 && player.getGuildRank() != 1 || player.getGuildId() <= 0) {
                    setCanceled(true);
                }
                if (newRank <= 1 || newRank > 5) {
                    setCanceled(true);
                }
                break;
            case 0x0f: // emblem change
                background = reader.readShort();
                backgroundColor = reader.readByte();
                logo = reader.readShort();
                logoColor = reader.readByte();
                if (player.getGuildId() <= 0 || player.getGuildRank() != 1 || (player.getMapId() != GuildHQ && player.getMapId() != ServerConstants.HOME_MAP)) {
                    setCanceled(true);
                }
                break;
            case 0x10:
                content = reader.readMapleAsciiString();
                if (player.getGuildId() <= 0 || player.getGuildRank() > 2) {
                    setCanceled(true);
                }
                if (content.length() > 100) {
                    setCanceled(true);
                }
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (System.currentTimeMillis() >= NextPurgeTimestamp) {
            Iterator<Invited> itr = Invitations.iterator();
            Invited inv;
            while (itr.hasNext()) {
                inv = itr.next();
                if (System.currentTimeMillis() >= inv.expiration) {
                    itr.remove();
                }
            }
            NextPurgeTimestamp = System.currentTimeMillis() + 20 * 60 * 1000;
        }

        switch (action) {
            case 0x02: {
                if (player.getGuildId() > 0) {
                    player.sendMessage(1, "You cannot create a new Guild while in one.");
                    return null;
                } else if (player.getMapId() != 200000301 && player.getMapId() != ServerConstants.HOME_MAP) {
                    player.sendMessage(1, "You cannot create a guild here.");
                } else if (player.getMeso() < MapleGuild.CREATE_GUILD_COST) {
                    player.dropMessage(1, "You do not have enough mesos to create a Guild.");
                    return null;
                } else if (!isGuildNameAcceptable(name)) {
                    player.dropMessage(1, "The Guild name you have chosen is not accepted.");
                    return null;
                }
                int guildID = Server.getInstance().createGuild(player.getId(), name);
                if (guildID == 0) {
                    getClient().announce(MaplePacketCreator.genericGuildMessage((byte) 0x1c));
                    return null;
                }
                player.gainMeso(-MapleGuild.CREATE_GUILD_COST, true, false, true);
                player.setGuildId(guildID);
                player.setGuildRank(1);
                player.saveGuildStatus();
                getClient().announce(MaplePacketCreator.showGuildInfo(player));
                player.dropMessage(1, "You have successfully created a Guild.");
                respawnPlayer(player);
                break;
            }
            case 0x05: {
                MapleGuildResponse mgr = MapleGuild.sendInvite(getClient(), name);
                if (mgr != null) {
                    getClient().announce(mgr.getPacket());
                } else {
                    Invited inv = new Invited(name, player.getGuildId());
                    if (!Invitations.contains(inv)) {
                        Invitations.add(inv);
                    }
                }
                break;
            }
            case 0x06: {
                if (playerID != player.getId()) {
                    return null;
                }
                name = player.getName().toLowerCase();
                Iterator<Invited> itr = Invitations.iterator();
                boolean bOnList = false;
                while (itr.hasNext()) {
                    Invited inv = itr.next();
                    if (guildID == inv.guildID && name.equalsIgnoreCase(inv.username)) {
                        bOnList = true;
                        itr.remove();
                        break;
                    }
                }
                if (!bOnList) {
                    return null;
                }
                player.setGuildId(guildID); // joins the guild
                player.setGuildRank(5); // start at lowest rank
                int s;

                s = Server.getInstance().addGuildMember(player.getMGC());
                if (s == 0) {
                    player.dropMessage(1, "The Guild you are trying to join is already full.");
                    player.setGuildId(0);
                    return null;
                }
                getClient().announce(MaplePacketCreator.showGuildInfo(player));
                player.saveGuildStatus(); // update database
                respawnPlayer(player);
                break;
            }
            case 0x07:
                if (playerID != player.getId() || !name.equalsIgnoreCase(player.getName())) {
                    return null;
                }
                getClient().announce(MaplePacketCreator.updateGP(player.getGuildId(), 0));
                Server.getInstance().leaveGuild(player.getMGC());
                getClient().announce(MaplePacketCreator.showGuildInfo(null));
                player.setGuildId(0);
                player.saveGuildStatus();
                respawnPlayer(player);
                break;
            case 0x08:
                Server.getInstance().expelMember(player.getMGC(), name, playerID);
                break;
            case 0x0d:
                Server.getInstance().changeRankTitle(player.getGuildId(), ranks);
                break;
            case 0x0e:
                Server.getInstance().changeRank(player.getGuildId(), playerID, newRank);
                break;
            case 0x0f:
                if (player.getMeso() < MapleGuild.CHANGE_EMBLEM_COST) {
                    getClient().announce(MaplePacketCreator.serverNotice(1, "You do not have enough mesos to create a Guild."));
                    return null;
                }
                Server.getInstance().setGuildEmblem(player.getGuildId(), background, backgroundColor, logo, logoColor);
                player.gainMeso(-MapleGuild.CHANGE_EMBLEM_COST, true, false, true);
                respawnPlayer(player);
                break;
            case 0x10:
                Server.getInstance().setGuildNotice(player.getGuildId(), content);
                break;
        }
        return null;
    }
}
