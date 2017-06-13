package discord;

import discord.event.ChatHandler;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.util.DiscordException;

import javax.security.auth.login.LoginException;

/**
 * @author Ian
 */
public class Bot {

    private IDiscordClient client;

    public void login() throws LoginException, InterruptedException, DiscordException {
        ClientBuilder builder = new ClientBuilder()
                .withToken(Discord.getConfig().getString("token"))
                .setMaxReconnectAttempts(4)
                .setDaemon(true);
        client = builder.login();
        EventDispatcher dispatcher = client.getDispatcher();
        dispatcher.registerListener(new ChatHandler());
    }

    public IDiscordClient getClient() {
        return client;
    }
}
