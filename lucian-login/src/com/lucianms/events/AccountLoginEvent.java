package com.lucianms.events;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.lucianms.BanManager;
import com.lucianms.Whitelist;
import com.lucianms.client.LoginState;
import com.lucianms.client.MapleClient;
import com.lucianms.constants.ServerConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.ArrayUtil;
import tools.HexTool;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * @author izarooni
 */
public class AccountLoginEvent extends PacketEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountLoginEvent.class);

    private String username;
    private String password;
    private String machineID;

    @Override
    public void exceptionCaught(MaplePacketReader reader, Throwable t) {
        getClient().announce(MaplePacketCreator.getLoginFailed(6));
        super.exceptionCaught(reader, t);
    }

    @Override
    public boolean inValidState() {
        return getClient().getLoginState() == LoginState.LogOut;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        username = reader.readMapleAsciiString();
        password = reader.readMapleAsciiString();
        byte[] machineByteStream = reader.read(16);
        machineByteStream = ArrayUtil.reverse(machineByteStream);
        String machineID = HexTool.toString(machineByteStream)
                .replaceAll(Pattern.compile("(00 | )").pattern(), "")
                .substring(4, 12);
        machineID = String.format("%s-%s", machineID.substring(0, 4), machineID.substring(4));
        this.machineID = machineID;

        reader.readInt(); // GameRoomClient
        reader.readByte();
        reader.readByte(); // 0
        reader.readByte(); // 0
        reader.readInt(); // PartnerCode
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        client.setAccountName(username);
        getClient().getHardwareIDs().add(machineID);
        int loginResult = client.getLoginResponse(username, password);
        if (client.getGMLevel() == 0 && Server.getConfig().getBoolean("WhitelistEnabled") && !Whitelist.hasAccount(client.getAccID())) {
            LOGGER.info("Attempted non-whitelist account username: '{}' , accountID: '{}'", username, client.getAccID());
            client.announce(MaplePacketCreator.getLoginFailed(7));
            client.sendPopup("The server is in whitelist mode! Only certain users will have access to the game right now.");
            return null;
        }

        if (loginResult == 5 && ServerConstants.ENABLE_AUTO_REGISTER) {
            if (client.getPassword() == null) {
                client.setPassword(password);
                client.announce(MaplePacketCreator.getLoginFailed(loginResult));
                client.sendPopup("This account does not exist.\r\nPress LOGIN once again to register this account.");
                return null;
            } else if (password.equalsIgnoreCase(client.getPassword())) {
                if (createAccount(username, password) == 0) {
                    loginResult = client.getLoginResponse(username, password);
                }
            }
        }

        if (loginResult == 3) {
            client.announce(MaplePacketCreator.getAccountBanned(client.getTemporaryBanLength()));
            client.sendPopup("You have been banned for \r\n'%s'", client.getBanReason());
        } else if (loginResult != 0) {
            client.announce(MaplePacketCreator.getLoginFailed(loginResult));
        } else {
            if (client.hasBannedIP() || BanManager.isBanned(client) || BanManager.isMachineBanned(machineID)) {
                client.announce(MaplePacketCreator.getAccountBanned(null));
                return null;
            }

            getClient().updateLoginState(LoginState.Login);
            client.announce(MaplePacketCreator.getAuthSuccess(client));
        }
        return null;
    }

    private int createAccount(String name, String password) {
        MapleClient client = getClient();
        String cPassword = BCrypt.with(BCrypt.Version.VERSION_2Y).hashToString(12, password.toCharArray());
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareCall("select count(*) from accounts where name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getInt(1) > 0) {
                            client.sendPopup("Username is already taken.");
                            return 6;
                        }
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("insert into accounts (name, password) values (?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, cPassword);
                ps.executeUpdate();
                return 0;
            }
        } catch (SQLException e) {
            client.sendPopup("Failed to create an account.");
            LOGGER.error("Failed to created account", e);
            return 6;
        }
    }
}
