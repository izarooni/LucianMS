package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleJob;
import com.lucianms.client.MapleSkinColor;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.Server;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class CreatePlayerEvent extends PacketEvent {

    private static int[] IDs = {
            1302000, 1312004, 1322005, 1442079,// weapons
            1040002, 1040006, 1040010, 1041002, 1041006, 1041010, 1041011, 1042167,// bottom
            1060002, 1060006, 1061002, 1061008, 1062115, // top
            1072001, 1072005, 1072037, 1072038, 1072383,// shoes
            30000, 30007, 30003, 30002, 30010, 30020, 30027,30023,30022, 30030,30032,30033,30037, 31000, 31040, 31050,// hair
            20000, 20001, 20002, 21000, 21001, 21002, 21201, 20401, 20402, 21700, 20100  //face
            //#NeverTrustStevenCode
    };


    private static boolean isLegal(int toCompare) {
        for (int ID : IDs) {
            if (ID == toCompare) {
                return true;
            }
        }
        return false;
    }

    private String username;
    private byte gender;
    private int jobID;
    private int hair, face, skin;
    private int top, bottom, shoes, weapon;

    public CreatePlayerEvent() {
        onPost(new Runnable() {
            @Override
            public void run() {
                if (username != null) {
                    try (Connection con = Server.getConnection();
                         PreparedStatement ps = con.prepareStatement("delete from ign_reserves where reserve = ? and username = ?")) {
                        ps.setString(1, username);
                        ps.setString(2, getClient().getAccountName());
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        username = reader.readMapleAsciiString();

        jobID = reader.readInt();
        face = reader.readInt();
        hair = reader.readInt() + reader.readInt();
        skin = reader.readInt();

        top = reader.readInt();
        bottom = reader.readInt();
        shoes = reader.readInt();
        weapon = reader.readInt();
        gender = reader.readByte();

        if (weapon == 1442079) {
            weapon = 1302000;
        }

        int[] items = new int[]{weapon, top, bottom, shoes, hair, face};
        for (int item : items) {
            if (!isLegal(item)) {
                setCanceled(true);
            }
        }

        MapleClient client = getClient();
        if (!client.getCreationName().equals(username)) { // username mismatch or in the case that MapleClient#getCreationName is unset, a NPE will occur
            getLogger().warn("MapleClient({}} attempted creation with un-confirmed username: '{}' - supposed to be '{}'", client.getAccountName(), username, client.getCreationName());
            setCanceled(true);
        } else if (!MapleCharacter.canCreateChar(username)) {
            getLogger().warn("MapleClient({}) failed to create character with username: '{}'", client.getAccountName(), username);
            setCanceled(true);
        }
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from ign_reserves where reserve = ? and username = ?")) {
            ps.setString(1, username);
            ps.setString(2, client.getAccountName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (!rs.getString("username").equalsIgnoreCase(client.getAccountName())) {
                        client.announce(MaplePacketCreator.charNameResponse(username, true));
                        setCanceled(true);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter newchar = new MapleCharacter(getClient());
        newchar.setWorld(getClient().getWorld());

        newchar.setSkinColor(MapleSkinColor.getById(skin));
        newchar.setGender(gender);
        newchar.setName(username);
        newchar.setHair(hair);
        newchar.setFace(face);

        newchar.setMapId(749081000);
        if (jobID == 0) { // Knights of Cygnus
            //newchar.setJob(MapleJob.NOBLESSE);
            newchar.setJob(MapleJob.BEGINNER);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (short) 0, (short) 1));
            //newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161047, (short) 0, (short) 1));
        } else if (jobID == 1) { // Adventurer
            newchar.setJob(MapleJob.BEGINNER);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (short) 0, (short) 1));
        } else if (jobID == 2) { // Aran
            newchar.setJob(MapleJob.LEGEND);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161048, (short) 0, (short) 1));
        } else {
            getClient().announce(MaplePacketCreator.deleteCharResponse(0, 9));
            return null;
        }

        MapleInventory equipped = newchar.getInventory(MapleInventoryType.EQUIPPED);

        Equip equip;

        equip = MapleItemInformationProvider.getInstance().getEquipById(top);
        equip.setPosition((byte) -5);
        equipped.addFromDB(equip);

        equip = MapleItemInformationProvider.getInstance().getEquipById(bottom);
        equip.setPosition((byte) -6);
        equipped.addFromDB(equip);

        equip = MapleItemInformationProvider.getInstance().getEquipById(shoes);
        equip.setPosition((byte) -7);
        equipped.addFromDB(equip);

        equip = MapleItemInformationProvider.getInstance().getEquipById(weapon);
        equip.setPosition((byte) -11);
        equip.setWatk((short) 30);
        equipped.addFromDB(equip);

        if (!newchar.insertNewChar()) {
            getClient().announce(MaplePacketCreator.deleteCharResponse(0, 9));
            return null;
        }
        getClient().announce(MaplePacketCreator.addNewCharEntry(newchar));
        return null;
    }
}