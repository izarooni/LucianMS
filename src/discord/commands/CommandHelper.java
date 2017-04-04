package discord.commands;

import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

/**
 * Basically, directs messages to commands (especially if you have multiple helpers/command files)
 *
 * @author izarooni
 */
public abstract class CommandHelper {

    public abstract void onLoad();

    public abstract void onUnload();

    public abstract void onCommand(MessageReceivedEvent event);
}
