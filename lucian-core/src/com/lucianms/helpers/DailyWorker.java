package com.lucianms.helpers;

import com.lucianms.io.scripting.Achievements;
import com.lucianms.server.Server;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author izarooni
 */
public class DailyWorker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyWorker.class);

    @Override
    public void run() {
        try (Connection con = Server.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.execute("update entry_limit set entries = 0");
            }
            LOGGER.info("Entry limits reset!");
        } catch (SQLException e) {
            LOGGER.error("Error while resetting entry limits", e);
        }

        LOGGER.info("Testing for account age achievements");
        // testFor character data (character age)
        for (MapleWorld world : Server.getWorlds()) {
            world.forEachPlayer(p -> Achievements.testFor(p, -1));
        }
    }
}
