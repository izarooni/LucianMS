package com.lucianms.events;

import com.lucianms.BanManager;
import com.lucianms.Whitelist;
import com.lucianms.client.LoginState;
import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.ArrayUtil;
import tools.HexTool;
import tools.MaplePacketCreator;

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
            client.announce(MaplePacketCreator.serverNotice(1, "The server is in whitelist mode! Only certain users will have access to the game right now."));
            return null;
        }
        if (loginResult == 3) {// normally 3
            client.announce(MaplePacketCreator.getAccountBanned(client.getTemporaryBanLength()));
            client.announce(MaplePacketCreator.serverNotice(1, String.format("You have been banned for\r\n'%s'", client.getBanReason())));
        } else if (loginResult != 0) {
            if (loginResult == 7) client.announce(MaplePacketCreator.serverNotice(1, "Account is already logged-in"));
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
}
