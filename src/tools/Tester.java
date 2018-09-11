package tools;

import com.lucianms.io.Config;
import com.lucianms.io.defaults.Defaults;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class Tester {

    private static final Logger LOGGER = LoggerFactory.getLogger(Tester.class);
    private static Config config = null;

    public static void main(String[] args) {
        System.setProperty("wzpath", "wz");
//        initConfig();
//        DatabaseConnection.useConfig(config);
//        createAccount("izarooni", "test2");
    }

    private static void initConfig() {
        try {
            if (Defaults.createDefaultIfAbsent(null, "server-config.json")) {
                LOGGER.info("Server config created. Configure settings and restart the server");
                System.exit(0);
            } else {
                config = new Config(new JSONObject(new JSONTokener(new FileInputStream("server-config.json"))));
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void createAccount(String name, String password) {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("insert into accounts (name, password) values (?, ?)")) {
            ps.setString(1, name);
            ps.setString(2, password);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
