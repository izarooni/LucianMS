package tools;

import com.lucianms.io.Config;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

/**
 * @author Frz (Big Daddy)
 * @author The Real Spookster (some modifications to this beautiful code)
 */
public class DatabaseConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnection.class);

    private static ThreadLocal<Connection> lConnection = null;
    private static long timeout = 28000;
    private static volatile long lastUse = System.currentTimeMillis();

    public static void useConfig(Config config) {
        lConnection = new ThreadLocalConnection(config);
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT @@GLOBAL.wait_timeout")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    timeout = rs.getInt(1);
                    LOGGER.info("Retrieved wait_timeout value is: {}", timeout);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        final long current = System.currentTimeMillis();
        if (current - lastUse >= timeout) {
            lConnection.remove();
        }
        lastUse = current;

        Connection c = lConnection.get();
        try {
            if (c.isClosed()) {
                // re initialize the database connection?
                lConnection = new ThreadLocalConnection(null);
                return getConnection();
            }
            c.getMetaData();
        } catch (SQLException e) { // connection is dead, therefore discard old object 5ever
            lConnection.remove();
            c = lConnection.get();
        }
        return c;
    }

    private static class ThreadLocalConnection extends ThreadLocal<Connection> {

        private final String URL;
        private final String username, password;

        private ThreadLocalConnection(Config config) {
            Config c = Optional.ofNullable(config).orElse(Server.getInstance().getConfig());
            URL = c.getString("DatabaseURL");
            username = c.getString("DatabaseUsername");
            password = c.getString("DatabasePassword");
        }

        @Override
        protected Connection initialValue() {
            try {
                return DriverManager.getConnection(URL, username, password);
            } catch (SQLException e) {
                LOGGER.error("Unable to establish database connection", e);
                e.printStackTrace();
                return null;
            }
        }
    }
}
