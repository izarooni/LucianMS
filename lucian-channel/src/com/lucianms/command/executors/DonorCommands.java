package com.lucianms.command.executors;

import com.lucianms.client.ChatType;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.command.CommandEvent;
import com.lucianms.server.MapleItemInformationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Pair;

import java.util.ArrayList;
import java.util.Map;

public class DonorCommands extends CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministratorCommands.class);
    private static ArrayList<String> HELP_LIST;

    public DonorCommands() {
        addCommand("help", this::Help, "Show a list of player commands");
        addCommand("donorchat", this::DonorChat,"Changes your chat color");
        addCommand("smega", this::Smega, "Smega");

        Map<String, Pair<CommandEvent, String>> commands = getCommands();
        HELP_LIST = new ArrayList<>(commands.size());
        for (Map.Entry<String, Pair<CommandEvent, String>> e : commands.entrySet()) {
            HELP_LIST.add(String.format("$%s - %s", e.getKey(), e.getValue().getRight()));
        }
        HELP_LIST.sort(String::compareTo);
    }

    private void Smega(MapleCharacter player, Command cmd, CommandArgs args){

        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.Smega);
        if (spamTracker.testFor(15000) && spamTracker.getTriggers() > 1) {
            player.sendMessage(5, "You are doing this too fast");

        }else {

            String medal = "";
            Item medalItem = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -49);
            if (medalItem != null) {
                medal = "<" + MapleItemInformationProvider.getInstance().getName(medalItem.getItemId()) + "> ";
            }
            player.getClient().getWorldServer().sendPacket(MaplePacketCreator.serverNotice(3, player.getClient().getChannelServer().getId(), String.format("%s%s : %s", medal, player.getName(), args.concatFrom(0)), true));
        }

        spamTracker.record();
    }

    private void DonorChat(MapleCharacter player, Command cmd, CommandArgs args){
        if(player.getClient().getGMLevel()>= 1){
            if (args.length() == 1) {
                Integer ordinal = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }

                switch(ordinal){
                    case 0:
                        player.setChatType(ChatType.NORMAL);
                        player.dropMessage("Chat type is set to '" + ChatType.NORMAL.name().toLowerCase() + "'");
                        break;
                    case 1:
                        player.setChatType(ChatType.YELLOW);
                        player.dropMessage("Chat type is set to '" + ChatType.YELLOW.name().toLowerCase() + "'");
                        break;
                    default:
                        player.sendMessage("Please choose 0 for normal, 1 for yellow");

                }
            }
        }
        else {
            player.sendMessage("This command is for Donors only");
        }
    }

    private void Help(MapleCharacter player, Command cmd, CommandArgs args) {
        boolean npc = args.length() == 1 && args.get(0).equalsIgnoreCase("npc");
        if (npc) {
            StringBuilder sb = new StringBuilder();
            for (String s : HELP_LIST) {
                String[] split = s.split(" - ");
                sb.append("\r\n#b").append(split[0]).append("#k - #r").append(split[1]);
            }
            player.announce(MaplePacketCreator.getNPCTalk(2007, (byte) 0, sb.toString(), "00 00", (byte) 0));
            sb.setLength(0);
        } else {
            HELP_LIST.forEach(player::dropMessage);
        }
    }
}
