package discord.commands;

import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

import java.util.List;

/**
 * Basically, directs messages to commands (especially if you have multiple helpers/command files)
 *
 * @author izarooni
 */
public abstract class CommandHelper {

    private Runnable run = null;

    public abstract void onLoad();

    public abstract void onUnload();

    public abstract void onCommand(MessageReceivedEvent event);

    public abstract List<String> getPermissions();

    public abstract boolean isValidPermission(String permission);


    public final void dispose() {
        onUnload();
        if (run != null) {
            run.run();
        }
    }

    public final void onDispose(Runnable run) {
        this.run = run;
    }
}
