package discord.event;

import discord.commands.CommandManagerHelper;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

/**
 * @author Ian
 */
public class ChatHandler {

    @EventSubscriber
    public void onGuildMessageReceived(MessageReceivedEvent event) {
        if (CommandManagerHelper.isValidCommand(event)) {
            CommandManagerHelper.getManagers().forEach(m -> m.onCommand(event));
        }
    }
}
