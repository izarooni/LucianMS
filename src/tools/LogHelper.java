package tools;

import client.MapleCharacter;
import client.inventory.Item;
import net.server.Server;
import server.MapleItemInformationProvider;
import server.MapleTrade;
import server.expeditions.MapleExpedition;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogHelper {

    public static void logTrade(MapleTrade trade1, MapleTrade trade2) {
        String name1 = trade1.getChr().getName();
        String name2 = trade2.getChr().getName();
        StringBuilder log = new StringBuilder("TRADE BETWEEN " + name1 + " AND " + name2 + "\r\n");
        //Trade 1 to trade 2
        log.append(trade1.getExchangeMesos()).append(" mesos from ").append(name1).append(" to ").append(name2).append(" \r\n");
        for (Item item : trade1.getItems()) {
            String itemName = MapleItemInformationProvider.getInstance().getName(item.getItemId()) + "(" + item.getItemId() + ")";
            log.append(item.getQuantity()).append(" ").append(itemName).append(" from ").append(name1).append(" to ").append(name2).append(" \r\n");
        }
        //Trade 2 to trade 1
        log.append(trade2.getExchangeMesos()).append(" mesos from ").append(name2).append(" to ").append(name1).append(" \r\n");
        for (Item item : trade2.getItems()) {
            String itemName = MapleItemInformationProvider.getInstance().getName(item.getItemId()) + "(" + item.getItemId() + ")";
            log.append(item.getQuantity()).append(" ").append(itemName).append(" from ").append(name2).append(" to ").append(name1).append(" \r\n");
        }
        log.append("\r\n\r\n");
        FilePrinter.printError("trades.txt", log.toString());
    }

    public static void logExpedition(MapleExpedition expedition) {
        Server.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, expedition.getType().toString() + " Expedition with leader " + expedition.getLeader().getName() + " finished after " + getTimeString(expedition.getStartTime())));

        StringBuilder log = new StringBuilder(expedition.getType().toString() + " EXPEDITION\r\n");
        log.append(getTimeString(expedition.getStartTime())).append("\r\n");

        for (MapleCharacter member : expedition.getMembers()) {
            log.append(">>").append(member.getName()).append("\r\n");
        }
        log.append("BOSS KILLS\r\n");
        for (String message : expedition.getBossLogs()) {
            log.append(message);
        }
        log.append("\r\n\r\n");
        FilePrinter.printError("expeditions.txt", log.toString());
    }

    public static String getTimeString(long then) {
        long duration = System.currentTimeMillis() - then;
        int seconds = (int) (duration / 1000) % 60;
        int minutes = (int) ((duration / (1000 * 60)) % 60);
        return minutes + " Minutes and " + seconds + " Seconds";
    }

    public static void logLeaf(MapleCharacter player, boolean gotPrize, String operation) {
        String timeStamp = new SimpleDateFormat("dd-M-yyyy hh:mm:ss").format(new Date());
        String log = player.getName() + (gotPrize ? " used a maple leaf to buy " + operation : " redeemed " + operation + " VP for a leaf") + " - " + timeStamp + "\r\n";
        FilePrinter.printError("mapleleaves.txt", log);
    }

    public static void logGacha(MapleCharacter player, int itemid, String map) {
        String itemName = MapleItemInformationProvider.getInstance().getName(itemid);
        String timeStamp = new SimpleDateFormat("dd-M-yyyy hh:mm:ss").format(new Date());
        String log = player.getName() + " got a " + itemName + "(" + itemid + ") from the " + map + " gachapon. - " + timeStamp + "\r\n";
        FilePrinter.printError("gachapon.txt", log);
    }
}
