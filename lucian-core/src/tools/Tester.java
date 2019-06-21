package tools;

import com.lucianms.io.Config;
import com.lucianms.io.defaults.Defaults;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.Server;
import com.zaxxer.hikari.HikariDataSource;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * @author izarooni
 */
public class Tester {

    private static final Logger LOGGER = LoggerFactory.getLogger(Tester.class);
    private static Config config = null;

    public static void main(String[] args) throws IOException {
        initConfig();
        TaskExecutor.initPoolSize(1);
        Server.createServer();
        System.out.println(HexTool.toString(MaplePacketCreator.showBossHP(9300184, 100, 100, (byte) 1, (byte) 5)));
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
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("insert into accounts (name, password) values (?, ?)")) {
            ps.setString(1, name);
            ps.setString(2, password);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void exportBINtoDatabase() {
        TaskExecutor.initPoolSize(Runtime.getRuntime().availableProcessors());
        Server.createServer();
        HikariDataSource src = Database.createDataSource("wz", c -> {
            c.setJdbcUrl("jdbc:mysql://localhost/wz_exports");
            c.setLeakDetectionThreshold(Long.MAX_VALUE);
            c.setThreadFactory(Executors.defaultThreadFactory());
        });

        try (Connection con = src.getConnection()) {
            con.setAutoCommit(false);
            File root = new File("resources/Mobs");
            File[] files = root.listFiles();
            if (files == null) {
                LOGGER.error("No files to evaluate");
                return;
            }
            for (File file : files) {
                String fn = file.getName();
                if (fn.contains("hair") || fn.contains("face")) continue;
                try (PreparedStatement ps = con.prepareStatement("insert into wz_mobs_cache values (?, ?, ?)")) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        MaplePacketReader reader = new MaplePacketReader(fis.readAllBytes());
                        int rows = reader.readInt();
                        LOGGER.info("{} rows of data to be processed in file {}", rows, fn);
                        for (int row = 0; row < rows; row++) {
                            int ID = reader.readInt();
                            String name = reader.read7BitEncodedString();
                            int propertyCount = reader.readInt();
                            HashMap<String, String> properties = new HashMap<>((int) (propertyCount * 0.8));
                            properties.put("name", name);
                            ps.setInt(1, ID);
                            for (int pIdx = 0; pIdx < propertyCount; pIdx++) {
                                String property = reader.read7BitEncodedString();
                                int spIdx = property.indexOf('=');
                                if (spIdx > -1) {
                                    String key = property.substring(0, spIdx);
                                    String value = property.substring(spIdx + 1);
                                    properties.put(key, value);
                                }
                            }
                            for (Map.Entry<String, String> entry : properties.entrySet()) {
                                ps.setString(2, entry.getKey());
                                ps.setString(3, entry.getValue());
                                ps.addBatch();
                            }
                            if (reader.readByte() == 1) {
                                reader.read(reader.readInt());
                            }
                        }
                    }
                    LOGGER.info("Executing batch");
                    ps.executeBatch();
                }
            }
            LOGGER.info("Now auto-committing");
            con.setAutoCommit(true);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
