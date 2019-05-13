package provider;

import provider.wz.XMLWZFile;

import java.io.File;

public class MapleDataProviderFactory {

    private static final String WZ_PATH = System.getProperty("wzpath");

    private MapleDataProviderFactory() {
    }

    public static MapleDataProvider getWZ(String directory) {
        return getWZ(new File(WZ_PATH, directory));
    }

    public static MapleDataProvider getWZ(File in) {
        return new XMLWZFile(in);
    }
}