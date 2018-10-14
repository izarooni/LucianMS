package tools;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author izarooni
 */
public class Database {

    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
    private static boolean Initialized = false;
    private static ComboPooledDataSource cpds = new ComboPooledDataSource();

    public static void init(String host, String schema, String username, String password) {
        if (Initialized) {
            throw new IllegalStateException("Database already initialized");
        }
        try {
            cpds.setDriverClass("com.mysql.cj.jdbc.Driver"); // loads the jdbc driver
            cpds.setJdbcUrl("jdbc:mysql://" + host + "/" + schema + "?useSSL=false");
            cpds.setUser(username);
            cpds.setPassword(password);
            cpds.setMaxIdleTime(300);
            cpds.setMaxIdleTimeExcessConnections(180);
            cpds.setMaxStatementsPerConnection(300);
            cpds.setMinPoolSize(3);
            cpds.setMaxPoolSize(20);
            Initialized = true;
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
//        System.out.println(Thread.currentThread().getStackTrace()[2]);
        try {
            return cpds.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve database connection", e);
        }
    }

    public static void execute(Connection con, String sql) throws SQLException {
        try (Statement statement = con.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    public static void executeSingle(Connection con, String sql, Object value) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, value);
            ps.executeUpdate();
        }
    }
}
