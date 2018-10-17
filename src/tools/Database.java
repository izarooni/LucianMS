package tools;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author izarooni
 */
public class Database {

    public static HikariDataSource createDataSource(String name) {
        HikariConfig config = new HikariConfig("hikari-db.properties");
        config.setPoolName(name);
        return new HikariDataSource(config);
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
