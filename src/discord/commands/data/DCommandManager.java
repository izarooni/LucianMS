package discord.commands.data;

import discord.Discord;
import discord.commands.Command;
import discord.commands.CommandFactory;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * @author izarooni
 */
public class DCommandManager {

    public void onCommand(MessageReceivedEvent event) {
        if (isValidCommand(event)) {
            Command command = CommandFactory.newInstance(event);
            if (command != null) {
                try {

                    DCommands.invoke(event, command);
                } catch (RateLimitException | MissingPermissionsException | DiscordException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean isValidCommand(MessageReceivedEvent event) {
        return !event.getMessage().getAuthor().getID().equals(Discord.getBot().getClient().getOurUser().getID()) // bot mustn't self-execute
                && event.getMessage().getContent().startsWith(Discord.getConfig().getString("commandTrigger"));
    }
}
