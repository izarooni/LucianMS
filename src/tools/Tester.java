package tools;

import io.Config;
import io.defaults.Defaults;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author izarooni
 */
public class Tester {

    private static final Logger LOGGER = LoggerFactory.getLogger(Tester.class);
    private static Config config = null;

    public static void main(String[] args) {
//        initConfig();
//        DatabaseConnection.useConfig(config);
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
}
