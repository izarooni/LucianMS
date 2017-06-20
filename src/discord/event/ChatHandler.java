package discord.event;

import discord.Discord;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

/**
 * @author Ian
 */
public class ChatHandler {

    @EventSubscriber
    public void onGuildMessageReceived(MessageReceivedEvent event) {
        Discord.getCommandManager().onCommand(event);
    }
}
