package tools.workers;

import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class DailyWorker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyWorker.class);


    @Override
    public void run() {
        Connection con = DatabaseConnection.getConnection();
        try {
            con.createStatement().execute("update entry_limit set entries = 0");
            LOGGER.info("Entry limits reset!");
        } catch (SQLException e) {
            LOGGER.error("Error while resetting entry limits", e);
        }

        LOGGER.info("Testing for account age achievements");
        // testFor character data (character age)
        Server.getInstance().getWorlds().forEach(w -> w.getPlayerStorage().getAllCharacters().forEach(p -> scripting.Achievements.testFor(p, -1)));
    }
}
