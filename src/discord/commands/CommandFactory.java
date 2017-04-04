package discord.commands;

import discord.Discord;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

/**
 * Parses the message contents and attempts to create a command object
 *
 * @author izarooni
 */
public class CommandFactory {

    private CommandFactory() {
    }

    public static Command newInstance(MessageReceivedEvent event) {
        String message = event.getMessage().getContent();
        String[] mSplit = message.split(" "); // original message as splits
        String[] sp = mSplit[0].toLowerCase().split(Discord.getConfig().getString("commandTrigger"));
        if (sp.length > 0) {
            String name = sp[1]; // the command put lowercase
            String[] args; // n-1 of sp; args of the command
            if (mSplit.length > 0) {
                args = new String[mSplit.length - 1];
                System.arraycopy(mSplit, 1, args, 0, args.length);
            } else {
                args = new String[0];
            }
            return new Command(name, args);
        } else {
            return null;
        }
    }
}
