package tools;

import io.Config;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * @author Frz (Big Daddy)
 * @author The Real Spookster (some modifications to this beautiful code)
 */
public class DatabaseConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnection.class);
    private static ThreadLocal<Connection> con = new ThreadLocalConnection();
    private static long timeout = 28000;
    private static volatile long lastUse = System.currentTimeMillis();

    static {
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
            con.remove();
        }
        lastUse = current;

        Connection c = con.get();
        try {
            c.getMetaData();
        } catch (SQLException e) { // connection is dead, therefore discard old object 5ever
            con.remove();
            c = con.get();
        }
        return c;
    }

    private static class ThreadLocalConnection extends ThreadLocal<Connection> {

        @Override
        protected Connection initialValue() {
            try {
                Class.forName("com.mysql.jdbc.Driver"); // touch the mysql driver
            } catch (ClassNotFoundException e) {
                System.out.println("[SEVERE] SQL Driver Not Found. Consider death by clams.");
                e.printStackTrace();
                return null;
            }
            try {
                Config config = Server.getInstance().getConfig();
                return DriverManager.getConnection(config.getString("DatabaseURL"), config.getString("DatabaseUsername"), config.getString("DatabasePassword"));
            } catch (SQLException e) {
                System.out.println("[SEVERE] Unable to make database connection.");
                e.printStackTrace();
                return null;
            }
        }
    }
}
