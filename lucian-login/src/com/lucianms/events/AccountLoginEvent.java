package com.lucianms.events;

import com.lucianms.Whitelist;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import java.util.Calendar;

/**
 * @author izarooni
 */
public class AccountLoginEvent extends PacketEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountLoginEvent.class);

    private String username;
    private String password;

    @Override
    public boolean inValidState() {
        return !getClient().isLoggedIn();
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        username = reader.readMapleAsciiString();
        password = reader.readMapleAsciiString();
        if (!Server.getToggles().checkProperty("server_online", false)) {
            getClient().announce(MaplePacketCreator.getLoginFailed((byte) 7));
            getClient().announce(MaplePacketCreator.serverNotice(0,
                    "The server is currently under maintenance." +
                            "\r\nFeel free to keep your game open and" +
                            "\r\ntry again in a few minutes." +
                            "\r\nYou will otherwise be noticed" +
                            "\r\nwhen you may login."));
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        getClient().setAccountName(username);
        int loginResult = getClient().login(username, password);
        if (Server.getConfig().getBoolean("WhitelistEnabled")) {
            if (!Whitelist.hasAccount(getClient().getAccID())) {
                LOGGER.warn("Attempted non-whitelist account login username: '{}' , accountID: '{}'", username, getClient().getAccID());
                getClient().announce(MaplePacketCreator.getLoginFailed(5));
                getClient().announce(MaplePacketCreator.serverNotice(1, "The server is in whitelist mode! Only certain users will have access to the game right now."));
                return null;
            }
        }

        //region ban checks
        if (getClient().hasBannedIP() || getClient().hasBannedMac()) {
            getClient().announce(MaplePacketCreator.getLoginFailed(3));
            return null;
        }
        Calendar tempban = getClient().getTempBanCalendar();
        if (tempban != null) {
            if (tempban.getTimeInMillis() > System.currentTimeMillis()) {
                getClient().announce(MaplePacketCreator.getTempBan(tempban.getTimeInMillis(), getClient().getGReason()));
                return null;
            }
        }
        //endregion

        if (loginResult == 3) {
            getClient().announce(MaplePacketCreator.getPermBan(getClient().getGReason()));
        } else if (loginResult != 0) {
            getClient().announce(MaplePacketCreator.getLoginFailed(loginResult));
        } else if (getClient().finishLogin() == 0) {
            getClient().announce(MaplePacketCreator.getAuthSuccess(getClient()));
        } else {
            getClient().announce(MaplePacketCreator.getLoginFailed(7));
        }
        return null;
    }
}
