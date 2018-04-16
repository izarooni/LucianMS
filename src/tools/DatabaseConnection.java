package tools;

import io.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

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

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Unable to register jdbc driver", e);
        }
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
            c.getMetaData();
        } catch (SQLException e) { // connection is dead, therefore discard old object 5ever
            lConnection.remove();
            c = lConnection.get();
        }
        return c;
    }

    private static class ThreadLocalConnection extends ThreadLocal<Connection> {

        private final Config config;

        public ThreadLocalConnection(Config config) {
            this.config = config;
        }

        @Override
        protected Connection initialValue() {
            try {
                return DriverManager.getConnection(config.getString("DatabaseURL"), config.getString("DatabaseUsername"), config.getString("DatabasePassword"));
            } catch (SQLException e) {
                LOGGER.error("Unable to establish database connection", e);
                e.printStackTrace();
                return null;
            }
        }
    }
}
