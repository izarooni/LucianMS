package net.server.handlers.login;

import client.MapleCharacter;
import client.MapleJob;
import client.MapleSkinColor;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import net.PacketEvent;
import server.MapleItemInformationProvider;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class CreateCharHandler extends PacketEvent {

    private static int[] IDs = {
            1302000, 1312004, 1322005, 1442079,// weapons
            1040002, 1040006, 1040010, 1041002, 1041006, 1041010, 1041011, 1042167,// bottom
            1060002, 1060006, 1061002, 1061008, 1062115, // top
            1072001, 1072005, 1072037, 1072038, 1072383,// shoes
            30000, 30010, 30020, 30030, 31000, 31040, 31050,// hair
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

    @Override
    public void post() {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("delete from ign_reserves where reserve = ? and username = ?")) {
            ps.setString(1, username);
            ps.setString(2, getClient().getAccountName());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        username = slea.readMapleAsciiString();

        jobID = slea.readInt();
        face = slea.readInt();
        hair = slea.readInt() + slea.readInt();
        skin = slea.readInt();

        top = slea.readInt();
        bottom = slea.readInt();
        shoes = slea.readInt();
        weapon = slea.readInt();
        gender = slea.readByte();


        int[] items = new int[]{weapon, top, bottom, shoes, hair, face};
        for (int item : items) {
            if (!isLegal(item)) {
                return;
            }
        }

        if (!MapleCharacter.canCreateChar(username)) {
            setCanceled(true);
        }
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("select * from ign_reserves where reserve = ? and username = ?")) {
            ps.setString(1, username);
            ps.setString(2, getClient().getAccountName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (!rs.getString("username").equalsIgnoreCase(getClient().getAccountName())) {
                        getClient().announce(MaplePacketCreator.charNameResponse(username, true));
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
        MapleCharacter newchar = MapleCharacter.getDefault(getClient());
        newchar.setWorld(getClient().getWorld());

        newchar.setSkinColor(MapleSkinColor.getById(skin));
        newchar.setGender(gender);
        newchar.setName(username);
        newchar.setHair(hair);
        newchar.setFace(face);

        newchar.setMapId(90000000);
        if (jobID == 0) { // Knights of Cygnus
            newchar.setJob(MapleJob.NOBLESSE);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161047, (short) 0, (short) 1));
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