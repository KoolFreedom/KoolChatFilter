package eu.koolfreedom;

import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.command.impl.CrashCommand;
import eu.koolfreedom.listener.ChatListener;
import eu.koolfreedom.listener.PlayerListener;
import eu.koolfreedom.utilities.FLog;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class KoolChatFilter extends JavaPlugin
{
    @Getter
    private static KoolChatFilter instance;
    private CommandLoader commandLoader;
    public ChatListener chatListener;
    public PlayerListener playerListener;

    @Override
    public void onLoad()
    {
        instance = this;
    }

    @Override
    public void onEnable()
    {
        FLog.info("Created by gamingto12");

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        commandLoader = new CommandLoader(CrashCommand.class);
        commandLoader.loadCommands();

        loadListeners();
    }

    @Override
    public void onDisable()
    {
        FLog.info("Disabled KoolChatFilter");
    }

    private void loadListeners()
    {
        chatListener = new ChatListener(this);
        playerListener = new PlayerListener(this);
    }
}
