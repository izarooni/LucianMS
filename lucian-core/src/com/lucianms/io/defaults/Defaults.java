package com.lucianms.io.defaults;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * @author izarooni
 */
public final class Defaults {

    private Defaults() {
    }

    /**
     * Copies file contents of specified fileName to output file located in the specified directory
     *
     * @param path     directory tree to desired file location
     * @param fileName name of file to create
     * @throws URISyntaxException
     * @throws IOException
     */
    public static boolean createDefault(String path, String fileName) throws URISyntaxException, IOException {
        File dirs = new File(path == null ? "" : path); // create any missing directories
        if (path != null) {
            dirs.mkdirs();
        }
        File output = new File(path, fileName); // desired file output location
        try (InputStream fis = Defaults.class.getResourceAsStream(fileName)) {
            try (FileOutputStream fos = new FileOutputStream(output)) {
                while (fis.available() > 0) {
                    fos.write(fis.read()); // copy contents
                }
                fos.flush();
                return true;
            }
        }
    }

    public static boolean createDefaultIfAbsent(String path, String fileName) throws URISyntaxException, IOException {
        File dirs = new File(path == null ? "" : path); // create any missing directories
        if (path != null) {
            dirs.mkdirs();
        }
        File output = new File(path, fileName); // desired file output location
        return !output.exists() && createDefault(path, fileName);
    }
}
