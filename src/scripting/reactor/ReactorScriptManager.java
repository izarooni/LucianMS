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
package scripting.reactor;

import client.MapleClient;
import scripting.ScriptUtil;
import server.maps.MapleReactor;
import server.maps.ReactorDropEntry;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.Pair;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * @author Lerk
 * @author izarooni
 */
public class ReactorScriptManager {

    private static Map<Integer, List<ReactorDropEntry>> drops = new HashMap<>();

    public static void act(MapleClient c, MapleReactor reactor) {
        try {
            ReactorActionManager rm = new ReactorActionManager(c, reactor);
            Invocable iv = ScriptUtil.eval(c, "reactor/" + reactor.getId() + ".js", Collections.singleton(new Pair<>("rm", rm)));
            if (iv == null) {
                return;
            }
            iv.invokeFunction("act");
        } catch (final ScriptException | NoSuchMethodException | NullPointerException e) {
            FilePrinter.printError(FilePrinter.REACTOR + reactor.getId() + ".txt", e);
        }
    }

    public static List<ReactorDropEntry> getDrops(int rid) {
        List<ReactorDropEntry> ret = drops.get(rid);
        if (ret == null) {
            ret = new LinkedList<>();
            try {
                try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid, chance, questid FROM reactordrops WHERE reactorid = ? AND chance >= 0")) {
                    ps.setInt(1, rid);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ret.add(new ReactorDropEntry(rs.getInt("itemid"), rs.getInt("chance"), rs.getInt("questid")));
                        }
                    }
                }
            } catch (Throwable e) {
                FilePrinter.printError(FilePrinter.REACTOR + rid + ".txt", e);
            }
            drops.put(rid, ret);
        }
        return ret;
    }

    public static void clearDrops() {
        drops.clear();
    }

    public static void touch(MapleClient c, MapleReactor reactor) {
        touching(c, reactor, true);
    }

    public static void untouch(MapleClient c, MapleReactor reactor) {
        touching(c, reactor, false);
    }

    public static void touching(MapleClient c, MapleReactor reactor, boolean touching) {
        try {
            ReactorActionManager rm = new ReactorActionManager(c, reactor);
            Invocable iv = ScriptUtil.eval(c, "reactor/" + reactor.getId() + ".js", Collections.singleton(new Pair<>("rm", rm)));
            if (iv == null) {
                return;
            }
            if (touching) {
                iv.invokeFunction("touch");
            } else {
                iv.invokeFunction("untouch");
            }
        } catch (final ScriptException | NoSuchMethodException | NullPointerException ute) {
            FilePrinter.printError(FilePrinter.REACTOR + reactor.getId() + ".txt", ute);
        }
    }
}