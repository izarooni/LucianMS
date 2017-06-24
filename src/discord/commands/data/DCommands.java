package discord.commands.data;

import client.MapleCharacter;
import client.MapleStat;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.ItemConstants;
import discord.DiscordGuild;
import discord.Discord;
import discord.commands.Command;
import discord.user.DiscordUser;
import discord.user.Permissions;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.json.JSONObject;
import org.json.JSONTokener;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.wz.XMLDomMapleData;
import server.MapleInventoryManipulator;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.*;
import tools.DatabaseConnection;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author izarooni
 */
public class DCommands {

    public static void invoke(MessageReceivedEvent event, Command command) throws RateLimitException, DiscordException, MissingPermissionsException {

        DiscordUser user = Discord.getUser(event.getAuthor());
        DiscordGuild guild = Discord.getGuild(event.getGuild());

        if (command.equals("help", "commands")) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            try {
                map.put("warp", "Warp a player to a map");
                map.put("reloadmap", "Reload a map in-game (only if it's loaded)");
                map.put("strip", "Strip a player of all equips (or until inventory is full)");
                map.put("clearinv", "Clear a player's inventory of all items");
                map.put("online", "View players that are currently online");
                map.put("stylist", "View specific information about the stylist");
                map.put("sethair", "Change the hair of a player online or offline");
                map.put("setface", "Change the face of a player online or offline");
                map.put("perms", "Modify permissions of a Discord user");

                MessageBuilder mb = initMessage(event.getMessage().getChannel());
                EmbedBuilder eb = new EmbedBuilder().withColor(52, 152, 219).withTitle("[ AVAILABLE COMMANDS ]");
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String title = Discord.getConfig().getString("commandTrigger") + entry.getKey();
                    String content = entry.getValue();
                    eb.appendField(title, content, false);
                }
                mb.withEmbed(eb.build()).build();
            } finally {
                map.clear();
            }
        } else if (command.equals("warp")) {
            if (!hasPermission(guild, user, DUserPower.Warp.toString())) {
                event.getMessage().getChannel().sendMessage("You don't have permission to do that!");
                return;
            }
            if (command.args.length == 2) {
                String username = command.args[0];
                int nMapId;
                try {
                    nMapId = Integer.parseInt(command.args[1]);
                } catch (NumberFormatException e) {
                    initMessage(event.getMessage().getChannel()).appendContent(command.args[1], MessageBuilder.Styles.INLINE_CODE).appendContent(" is not a number").build();
                    return;
                }
                for (World world : Server.getInstance().getWorlds()) {
                    MapleCharacter player = world.getPlayerStorage().getCharacterByName(username);
                    if (player != null) {
                        int bMapId = player.getMapId();
                        player.changeMap(nMapId);
                        MessageBuilder message = initMessage(event.getMessage().getChannel()).appendContent(player.getName(), MessageBuilder.Styles.INLINE_CODE).appendContent(" warped from ").appendContent(Integer.toString(bMapId), MessageBuilder.Styles.INLINE_CODE).appendContent(" to ").appendContent(Integer.toString(nMapId), MessageBuilder.Styles.INLINE_CODE);
                        message.build();
                        return;
                    } else {
                        int id = MapleCharacter.getIdByName(username);
                        if (id > 0) {
                            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("update characters set map = ? where id = ?")) {
                                ps.setInt(1, nMapId);
                                ps.setInt(2, id);
                                ps.executeUpdate();
                                initMessage(event.getMessage().getChannel()).appendContent(username, MessageBuilder.Styles.INLINE_CODE).appendContent(" offline warped to ").appendContent(Integer.toString(nMapId), MessageBuilder.Styles.INLINE_CODE).build();
                                return;
                            } catch (SQLException e) {
                                event.getMessage().getChannel().sendMessage("An error occurred");
                            }
                        }
                    }
                }
                event.getMessage().getChannel().sendMessage(String.format("Unable to find any player online named '%s'", username));
            }
        } else if (command.equals("reloadmap")) {
            if (!hasPermission(guild, user, DUserPower.ReloadMap.toString())) {
                event.getMessage().getChannel().sendMessage("You don't have permission to do that!");
                return;
            }
            if (command.args.length == 1) {
                int mapId;
                try {
                    mapId = Integer.parseInt(command.args[0]);
                } catch (NumberFormatException e) {
                    initMessage(event.getMessage().getChannel()).appendContent(command.args[0], MessageBuilder.Styles.INLINE_CODE).appendContent(" is not a number").build();
                    return;
                }
                for (World worlds : Server.getInstance().getWorlds()) {
                    for (Channel channels : worlds.getChannels()) {
                        if (channels.getMapFactory().getMap(mapId) != null) {
                            channels.getMapFactory().reloadField(mapId);
                        } else {
                            initMessage(event.getMessage().getChannel()).appendContent(Integer.toString(mapId), MessageBuilder.Styles.INLINE_CODE).appendContent(" is an invalid map").build();
                            return;
                        }
                    }
                }
                event.getMessage().getChannel().sendMessage("Map reloaded");
            }
        } else if (command.equals("strip")) {
            if (!hasPermission(guild, user, DUserPower.Strip.toString())) {
                event.getMessage().getChannel().sendMessage("You don't have permission to do that!");
                return;
            }
            if (command.args.length == 1) {
                String username = command.args[0];
                for (World worlds : Server.getInstance().getWorlds()) {
                    MapleCharacter target = worlds.getPlayerStorage().getCharacterByName(username);
                    if (target != null) {
                        MapleInventory equipped = target.getInventory(MapleInventoryType.EQUIPPED);
                        MapleInventory equip = target.getInventory(MapleInventoryType.EQUIP);
                        if (equip.getNextFreeSlot() == -1) {
                            initMessage(event.getMessage().getChannel()).appendContent("Unable to strip ").appendContent(target.getName(), MessageBuilder.Styles.INLINE_CODE).appendContent(" due to full inventory").build();
                        } else {
                            ArrayList<Short> pos = new ArrayList<>(); // item positions - concurrency
                            for (Item item : equipped.list()) {
                                pos.add(item.getPosition());
                            }
                            for (short position : pos) {
                                short freeSlot = equip.getNextFreeSlot();
                                if (freeSlot == -1) {
                                    initMessage(event.getMessage().getChannel()).appendContent("Unable to fully strip ").appendContent(target.getName(), MessageBuilder.Styles.INLINE_CODE).appendContent(" due to full inventory").build();
                                    break;
                                }
                                MapleInventoryManipulator.unequip(target.getClient(), position, freeSlot);
                            }
                            initMessage(event.getMessage().getChannel()).appendContent("Finished stripping items from player ").appendContent(username, MessageBuilder.Styles.INLINE_CODE).build();
                        }
                        return;
                    }
                }
                try {
                    int playerId = MapleCharacter.getIdByName(username);
                    ArrayList<Integer> ids = new ArrayList<>(); // inventoryitemids
                    ArrayList<Integer> emptyEquipSlots = new ArrayList<>();
                    if (playerId > -1) {
                        int equipSlots = 0;
                        int equipSlotsAvailable;
                        // get equip inventory capacity
                        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("select equipslots from characters where id = ?")) {
                            ps.setInt(1, playerId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    equipSlots = rs.getInt("equipslots");
                                }
                            }
                        }
                        // check if inventory is full
                        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("select count(*) as total from inventoryitems where characterid = ? and inventorytype = -1")) {
                            ps.setInt(1, playerId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    equipSlotsAvailable = equipSlots - rs.getInt("total");
                                    if (equipSlotsAvailable < 1) {
                                        initMessage(event.getMessage().getChannel()).appendContent("Unable to offline strip ").appendContent(username, MessageBuilder.Styles.INLINE_CODE).appendContent(" due to full inventory").build();
                                        return;
                                    }
                                }
                            }
                        }
                        // get all equipped items to begin transfer
                        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("select inventoryitemid from inventoryitems where characterid = ? and inventorytype = -1")) {
                            ps.setInt(1, playerId);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    ids.add(rs.getInt("inventoryitemid"));
                                }
                            }
                        }
                        // get empty equip slots
                        ArrayList<Integer> takenEquipSlots = new ArrayList<>();
                        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("select position from inventoryitems where characterid = ? and inventorytype = 1")) {
                            ps.setInt(1, playerId);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    takenEquipSlots.add(rs.getInt("position"));
                                }
                            }
                        }
                        for (int i = 1; i <= equipSlots; i++) {
                            if (!takenEquipSlots.contains(i)) {
                                emptyEquipSlots.add(i);
                            }
                        }
                        takenEquipSlots.clear();
                        takenEquipSlots.trimToSize();
                        // transfer items from equipped (-1) to equipped (1)
                        int changes = Math.min(ids.size(), emptyEquipSlots.size());
                        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("update inventoryitems set position = ?, inventorytype = 1 where characterid = ? and inventoryitemid = ?")) {
                            for (int i = 0; i < changes; i++) {
                                int inventoryitemid = ids.get(i);
                                int newSlot = emptyEquipSlots.get(i);
                                ps.setInt(1, newSlot);
                                ps.setInt(2, playerId);
                                ps.setInt(3, inventoryitemid);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                        initMessage(event.getMessage().getChannel()).appendContent("Finished offline stripping ").appendContent(String.format("%d / %d", changes, ids.size()), MessageBuilder.Styles.INLINE_CODE).appendContent(" items from player ").appendContent(username, MessageBuilder.Styles.INLINE_CODE).build();
                    } else {
                        initMessage(event.getMessage().getChannel()).appendContent("Could not find any player named ").appendContent(username, MessageBuilder.Styles.INLINE_CODE).build();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    event.getMessage().getChannel().sendMessage("An error occurred");
                }
            }
        } else if (command.equals("clearinv")) {
            if (!hasPermission(guild, user, DUserPower.ClearInv.toString())) {
                event.getMessage().getChannel().sendMessage("You don't have permission to do that!");
                return;
            }
            if (command.args.length == 2) {
                try {
                    String username = command.args[0];
                    String SInventoryType = command.args[1].toUpperCase();
                    MapleInventoryType inventoryType;
                    try {
                        inventoryType = MapleInventoryType.valueOf(SInventoryType);
                        if (inventoryType == MapleInventoryType.EQUIPPED || inventoryType == MapleInventoryType.UNDEFINED) {
                            throw new IllegalArgumentException();
                        }
                    } catch (IllegalArgumentException e) {
                        StringBuilder sb = new StringBuilder();
                        for (MapleInventoryType type : MapleInventoryType.values()) {
                            if (type.getType() >= 1 && type.getType() <= 5) {
                                sb.append(type.name()).append(" ");
                            }
                        }
                        initMessage(event.getMessage().getChannel()).appendContent(SInventoryType, MessageBuilder.Styles.INLINE_CODE).appendContent(" is an invalid inventory type. Use one of the follopwing: ").appendContent(sb.toString(), MessageBuilder.Styles.INLINE_CODE).build();
                        return;
                    }
                    for (World world : Server.getInstance().getWorlds()) {
                        MapleCharacter target = world.getPlayerStorage().getCharacterByName(username);
                        if (target != null) {
                            MapleInventory inventory = target.getInventory(inventoryType);
                            for (byte i = 0; i < inventory.getSlotLimit(); i++) {
                                Item item;
                                if ((item = inventory.getItem(i)) != null) {
                                    int itemId = item.getItemId();
                                    short quantity = (short) target.getItemQuantity(itemId, false);
                                    if (inventoryType == MapleInventoryType.EQUIP) {
                                        quantity = 1;
                                    }
                                    if (ItemConstants.isPet(itemId)) {
                                        if (item.getPetId() > -1) {
                                            // maybe skip pets instead?
                                            try {
                                                DatabaseConnection.getConnection().createStatement().execute("delete from pets where petid = " + item.getPet().getUniqueId());
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                                continue;
                                            }
                                        }
                                    }
                                    if (item instanceof Equip) {
                                        if (((Equip) item).getRingId() > -1) {
                                            // skip friendship, crush and wedding rings
                                            continue;
                                        }
                                    }
                                    MapleInventoryManipulator.removeById(target.getClient(), inventoryType, itemId, quantity, false, false);
                                }
                            }
                            initMessage(event.getMessage().getChannel()).appendContent("Finished clearing ").appendContent(username + "'s", MessageBuilder.Styles.INLINE_CODE).appendContent(" ").appendContent(inventoryType.name(), MessageBuilder.Styles.INLINE_CODE).appendContent(" inventory").build();
                            return;
                        }
                    }
                    int playerId = MapleCharacter.getIdByName(username);
                    if (playerId > -1) {
                        ArrayList<Integer> ids = new ArrayList<>(); // inventoryitemids
                        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("select inventoryitemid from inventoryitems where characterid = ? and inventorytype = ?")) {
                            ps.setInt(1, playerId);
                            ps.setInt(2, inventoryType.getType());
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    ids.add(rs.getInt("inventoryitemid"));
                                }
                            }
                        }
                        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("delete from inventoryequipment where inventoryitemid = ?")) {
                            for (Integer id : ids) {
                                ps.setInt(1, id);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("delete from inventoryitems where inventoryitemid = ?")) {
                            for (Integer id : ids) {
                                ps.setInt(1, id);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                        initMessage(event.getMessage().getChannel()).appendContent("Finished offline clearing ").appendContent(username + "'s", MessageBuilder.Styles.INLINE_CODE).appendContent(" ").appendContent(inventoryType.name(), MessageBuilder.Styles.INLINE_CODE).appendContent(" inventory").build();
                    } else {
                        initMessage(event.getMessage().getChannel()).appendContent("Could not find any player named ").appendContent(username, MessageBuilder.Styles.INLINE_CODE).build();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    event.getMessage().getChannel().sendMessage("An error occurred");
                }
            }
        } else if (command.equals("online")) {
            MessageBuilder mb = initMessage(event.getMessage().getChannel());
            if (Server.getInstance().isOnline()) {
                EmbedBuilder eb = new EmbedBuilder().withColor(52, 152, 219).withTitle("[ CONNECTED PLAYERS ]");
                for (World worlds : Server.getInstance().getWorlds()) {
                    for (Channel chs : worlds.getChannels()) {
                        StringBuilder sb = new StringBuilder();
                        for (MapleCharacter players : chs.getPlayerStorage().getAllCharacters()) {
                            sb.append(players.getName()).append(" ");
                        }
                        if (sb.length() == 0) {
                            sb.append("No players");
                        }
                        eb.appendField(String.format("Channel %d", chs.getId()), sb.toString(), false);
                    }
                }
                mb.withEmbed(eb.build());
            } else {
                mb.withContent("The server is currently not online! Try again in a few minutes");
            }
            mb.build();
        } else if (command.equals("stylist")) {
            if (!hasPermission(guild, user, DUserPower.Stylist.toString())) {
                event.getMessage().getChannel().sendMessage("You don't have permission to do that!");
                return;
            }
            if (command.args.length == 2) {
                String category = command.args[0];
                if (!category.matches(Pattern.compile("([mf])(Hairs|Faces)").pattern())) {
                    initMessage(event.getMessage().getChannel()).appendContent(command.args[0], MessageBuilder.Styles.INLINE_CODE).appendContent(" is an invalid category. Try ").appendContent("(m/f)Faces", MessageBuilder.Styles.INLINE_CODE).appendContent(" or ").appendContent("(m/f)Hairs", MessageBuilder.Styles.INLINE_CODE).build();
                    return;
                }
                int index;
                try {
                    index = Integer.parseInt(command.args[1]) - 1;
                } catch (NumberFormatException e) {
                    initMessage(event.getMessage().getChannel()).appendContent(command.args[1], MessageBuilder.Styles.INLINE_CODE).appendContent(" is not a number").build();
                    return;
                }
                ArrayList<Integer> styles = new ArrayList<>();
                try {
                    try (FileInputStream fis = new FileInputStream(new File("data-styler.json"))) {
                        JSONObject object = new JSONObject(new JSONTokener(fis));
                        String[] array = object.getString(category).split(", ");
                        for (String o : array) {
                            styles.add(Integer.parseInt(o));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        event.getMessage().getChannel().sendMessage("An error occurred");
                        return;
                    }
                    if (index < 0 || index >= styles.size()) {
                        initMessage(event.getMessage().getChannel()).appendContent("You must pick a number between ").appendContent("1 - " + (styles.size()), MessageBuilder.Styles.INLINE_CODE).build();
                        return;
                    }

                    boolean hasClientID = System.getProperty("imgurClientID") != null;
                    int id = styles.get(index);

                    MapleDataProvider provider = MapleDataProviderFactory.getDataProvider(MapleDataProviderFactory.fileInWZPath("Character.wz/" + category.substring(1, category.length() - 1)));
                    MapleData data = provider.getData("000" + id + ".img");
                    if (data != null) {
                        data = data.getChildByPath("default/hairOverHead");
                        if (data != null) {
                            try {
                                String imageLink = getCacheURL(category + ":" + id);
                                if (imageLink == null) {
                                    if (hasClientID) {
                                        String response = imgurUpload(((XMLDomMapleData) data).getValue("basedata"));
                                        JSONObject object = new JSONObject(response);
                                        imageLink = object.getJSONObject("data").getString("link");
                                        storeCacheURL(category + ":" + id, imageLink);
                                    } else {
                                        System.err.println("Unable to upload style image: No imgur Client ID provided");
                                    }
                                }
                                EmbedBuilder eb = new EmbedBuilder().withColor(52, 152, 219);
                                if (imageLink == null) {
                                    // still no image? send a red X for error
                                    imageLink = "http://imgur.com/WNboC8V";
                                    eb.withFooterText("This " + category + " has no image to display");
                                }
                                eb.withImage(imageLink);
                                eb.withTitle(category + " : " + id);
                                initMessage(event.getMessage().getChannel()).withEmbed(eb.build()).build();
                            } catch (Exception e) {
                                e.printStackTrace();
                                initMessage(event.getMessage().getChannel()).appendContent("An error occurred").appendContent(e.getMessage(), MessageBuilder.Styles.CODE).build();
                            }
                        } else {
                            event.getMessage().getChannel().sendMessage(String.format("No image found for hair 000%d.img", id));
                        }
                    } else {
                        event.getMessage().getChannel().sendMessage(String.format("Could not find find 000%d.img", id));
                    }
                } finally {
                    styles.clear();
                    styles = null;
                }
            }
        } else if (command.equals("sethair")) {
            if (!hasPermission(guild, user, DUserPower.Stylist.toString())) {
                event.getMessage().getChannel().sendMessage("You don't have permission to do that!");
                return;
            }
            if (command.args.length == 2) {
                String username = command.args[0];
                int hairID;
                try {
                    hairID = Integer.parseInt(command.args[1]);
                } catch (NumberFormatException e) {
                    initMessage(event.getMessage().getChannel()).appendContent(command.args[1], MessageBuilder.Styles.INLINE_CODE).appendContent(" is not a number").build();
                    return;
                }
                for (World world : Server.getInstance().getWorlds()) {
                    MapleCharacter player = world.getPlayerStorage().getCharacterByName(username);
                    if (player != null) {
                        player.setHair(hairID);
                        player.updateSingleStat(MapleStat.HAIR, hairID);
                        player.equipChanged();
                        initMessage(event.getChannel()).appendContent("Changed ").appendContent(player.getName() + "'s", MessageBuilder.Styles.INLINE_CODE).appendContent(" hair to ").appendContent(Integer.toString(hairID), MessageBuilder.Styles.INLINE_CODE).build();
                        return;
                    }
                }
                try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("update characters set hair = ? where name = ?")) {
                    ps.setInt(1, hairID);
                    ps.setString(2, username);
                    ps.executeUpdate();
                    initMessage(event.getChannel()).appendContent("Offline changed ").appendContent(username + "'s", MessageBuilder.Styles.INLINE_CODE).appendContent(" hair to ").appendContent(Integer.toString(hairID), MessageBuilder.Styles.INLINE_CODE).build();
                } catch (SQLException e) {
                    event.getChannel().sendMessage("An error occurred");
                    e.printStackTrace();
                }
            }
        } else if (command.equals("setface")) {
            if (!hasPermission(guild, user, DUserPower.Stylist.toString())) {
                event.getMessage().getChannel().sendMessage("You don't have permission to do that!");
                return;
            }
            if (command.args.length == 2) {
                String username = command.args[0];
                int faceID;
                try {
                    faceID = Integer.parseInt(command.args[1]);
                } catch (NumberFormatException e) {
                    initMessage(event.getMessage().getChannel()).appendContent(command.args[1], MessageBuilder.Styles.INLINE_CODE).appendContent(" is not a number").build();
                    return;
                }
                for (World world : Server.getInstance().getWorlds()) {
                    MapleCharacter player = world.getPlayerStorage().getCharacterByName(username);
                    if (player != null) {
                        player.setFace(faceID);
                        player.updateSingleStat(MapleStat.FACE, faceID);
                        player.equipChanged();
                        initMessage(event.getChannel()).appendContent("Changed ").appendContent(player.getName() + "'s", MessageBuilder.Styles.INLINE_CODE).appendContent(" face to ").appendContent(Integer.toString(faceID), MessageBuilder.Styles.INLINE_CODE).build();
                        return;
                    }
                }
                try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("update characters set face = ? where name = ?")) {
                    ps.setInt(1, faceID);
                    ps.setString(2, username);
                    ps.executeUpdate();
                    initMessage(event.getChannel()).appendContent("Offline changed ").appendContent(username + "'s", MessageBuilder.Styles.INLINE_CODE).appendContent(" face to ").appendContent(Integer.toString(faceID), MessageBuilder.Styles.INLINE_CODE).build();
                } catch (SQLException e) {
                    event.getChannel().sendMessage("An error occurred");
                    e.printStackTrace();
                }
            }
        } else if (command.equals("perms")) {
            if (!hasPermission(guild, user, DUserPower.Permissions.toString())) {
                event.getMessage().getChannel().sendMessage("You don't have permission to do that!");
                return;
            }
            if (command.args.length == 1 && command.args[0].equalsIgnoreCase("list")) {
                StringBuilder sb = new StringBuilder();
                for (DUserPower commands : DUserPower.values()) {
                    sb.append(commands.toString()).append("\r\n");
                }
                if (sb.length() > 0) {
                    initMessage(event.getChannel()).appendCode(null, sb.toString()).build();
                } else {
                    event.getChannel().sendMessage("There are currently no permissions");
                }
            } else if (command.args.length > 2) {
                if (command.args[0].equalsIgnoreCase("give") || command.args[0].equalsIgnoreCase("remove")) {
                    boolean give = command.args[0].equals("give");

                    IUser mentioned = event.getMessage().getMentions().get(0);
                    for (int i = 2; i < command.args.length; i++) {
                        String permission = command.args[i];

                        DiscordUser mUser = Discord.getUser(mentioned);
                        try {
                            if (give) {
                                Permissions.setPermission(mUser, permission);
                            } else {
                                Permissions.removePermission(mUser, permission);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            event.getChannel().sendMessage("An error occurred");
                            return;
                        }
                        if (permission.equals("*")) {
                            break;
                        }
                    }
                    event.getChannel().sendMessage("Success!");
                } else if (command.args[0].equalsIgnoreCase("giverole") || command.args[0].equalsIgnoreCase("removerole")) {
                    boolean give = command.args[0].equals("giverole");
                    String role = command.args[1];
                    role = role.replaceAll("_", " ");
                    if (guild.getGuild().getRolesByName(role) == null) {
                        initMessage(event.getChannel()).appendContent(role, MessageBuilder.Styles.INLINE_CODE).appendContent(" is an invalid role").build();
                        return;
                    }
                    for (int i = 2; i < command.args.length; i++) {
                        String permission = command.args[i];
                        if (Permissions.invalidPermission(permission)) {
                            initMessage(event.getChannel()).appendContent(permission, MessageBuilder.Styles.INLINE_CODE).appendContent(" is an invalid permission").build();
                            return;
                        }
                        try {
                            Permissions.serverPermission(guild, role, permission, give);
                        } catch (IOException e) {
                            e.printStackTrace();
                            event.getChannel().sendMessage("An error occurred");
                            return;
                        }
                    }
                    event.getChannel().sendMessage("Success!");
                }
            }
        }
    }

    private static MessageBuilder initMessage(IChannel channel) {
        return new MessageBuilder(Discord.getBot().getClient()).withChannel(channel);
    }

    private static boolean hasPermission(DiscordGuild guild, DiscordUser user, String permission) {
        if (user.getUser().getLongID() == Discord.getConfig().getNumber("ownerId")) {
            return true;
        } else if (user.hasPermission(permission)) {
            return true;
        }
        for (IRole role : user.getUser().getRolesForGuild(guild.getGuild())) {
            if (guild.hasPermission(role.getName(), permission)) {
                return true;
            }
        }
        return false;
    }

    private static final String URL_API = "https://api.imgur.com/3/image";

    private static String getCacheURL(String varname) {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("select varvalue from discord_urlcache where varname = ?")) {
            ps.setString(1, varname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("varvalue");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void storeCacheURL(String varname, String varvalue) {
        // i guess... replace previous data if existing
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("insert into discord_urlcache values (DEFAULT, ?, ?)")) {
            ps.setString(1, varname);
            ps.setString(2, varvalue);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String imgurUpload(String baseData) throws Exception {
        URL url = new URL(URL_API);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        String sData;
        final StringBuilder db = new StringBuilder();
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put(URLEncoder.encode("image", "UTF-8"), URLEncoder.encode(baseData, "UTF-8"));
        data.forEach((key, value) -> db.append("&").append(key).append("=").append(value));
        sData = db.substring(1);

        conn.setDoOutput(true);
        conn.setDoInput(true);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Client-ID " + System.getProperty("imgurClientID"));
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.connect();

        try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream())) {
            wr.write(sData);
            wr.flush();
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        conn.disconnect();
        return sb.toString();
    }
}
