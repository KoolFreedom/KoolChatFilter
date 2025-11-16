package eu.koolfreedom;

import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.command.impl.CrashCommand;
import eu.koolfreedom.listener.impl.ChatListener;
import eu.koolfreedom.listener.impl.PlayerListener;
import eu.koolfreedom.utilities.BuildProperties;
import eu.koolfreedom.utilities.FLog;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class KoolChatFilter extends JavaPlugin
{
    @Getter
    private static KoolChatFilter instance;
    private BuildProperties buildMeta;
    private CommandLoader commandLoader;
    public ChatListener chatListener;
    public PlayerListener playerListener;

    @Override
    public void onLoad()
    {
        instance = this;
        buildMeta = new BuildProperties();
    }

    @Override
    public void onEnable()
    {
        FLog.info("Created by gamingto12");
        FLog.info("Version {}.{}", buildMeta.getVersion(), buildMeta.getNumber());
        FLog.info("Compiled {} by {}", buildMeta.getDate(), buildMeta.getAuthor());

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
        chatListener = new ChatListener();
        playerListener = new PlayerListener();
    }
}
