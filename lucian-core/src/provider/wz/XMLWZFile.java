/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package provider.wz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class XMLWZFile implements MapleDataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLWZFile.class);
    private File root;
    private WZDirectoryEntry rootForNavigation;

    public XMLWZFile(File root) {
        this.root = root;
        rootForNavigation = new WZDirectoryEntry(root, 0, 0, null);
        fillMapleDataEntitys(this.root, rootForNavigation);
    }

    private void fillMapleDataEntitys(File root, WZDirectoryEntry wzdir) {
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (file.isDirectory() && !fileName.endsWith(".img")) {
                    WZDirectoryEntry newDir = new WZDirectoryEntry(file, 0, 0, wzdir);
                    wzdir.addDirectory(newDir);
                    fillMapleDataEntitys(file, newDir);
                } else if (fileName.endsWith(".xml")) {
                    wzdir.addFile(new WZFileEntry(file, 0, 0, wzdir));
                }
            }
        }
    }

    @Override
    public MapleData getData(String path) {
        File dataFile = new File(root, path + ".xml");
        File imageDataDir = new File(root, path);
        if (!dataFile.exists()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(dataFile)) {
            return new XMLDomMapleData(fis, imageDataDir.getParentFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Datafile " + path + " does not exist in " + root.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            LOGGER.error("Unable to parse file '{}'", imageDataDir.getAbsolutePath(), e);
        }
        return null;
    }

    @Override
    public MapleDataDirectoryEntry getRoot() {
        return rootForNavigation;
    }
}
