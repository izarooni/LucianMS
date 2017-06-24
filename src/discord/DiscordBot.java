package discord;

import discord.event.ChatHandler;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageHistory;
import sx.blah.discord.util.MessageList;

import javax.security.auth.login.LoginException;

/**
 * @author Ian
 */
public class DiscordBot {

    private IDiscordClient client;

    public void login() throws LoginException, InterruptedException, DiscordException {
        ClientBuilder builder = new ClientBuilder()
                .withToken(Discord.getConfig().getString("token"))
                .setMaxReconnectAttempts(4)
                .setMaxMessageCacheCount(50)
                .setDaemon(true);
        client = builder.build();
        EventDispatcher dispatcher = client.getDispatcher();
        dispatcher.registerListener(new ChatHandler());
        MessageList.setEfficiency(client, MessageList.EfficiencyLevel.HIGH);

        client.login();
    }

    public IDiscordClient getClient() {
        return client;
    }
}
