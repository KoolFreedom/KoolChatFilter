package eu.koolfreedom;

import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.command.impl.CrashCommand;
import eu.koolfreedom.listener.impl.*;
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
    public AnvilListener anvilListener;
    public BookListener bookListener;
    public SignListener signListener;
    public CommandPreProcessListener commandPreProcessListener;

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

        commandLoader = new CommandLoader(CrashCommand.class);
        commandLoader.loadCommands();
        FLog.info("Loaded {} commands,", commandLoader.getKoolCommands().size());

        reloadBansConfig();
        FLog.info("Loaded {} people in the idiot file", IdiotsList.get().getBansCount());

        loadListeners();
        FLog.info("Loaded listeners");
    }

    @Override
    public void onDisable()
    {
        FLog.info("Disabled KoolChatFilter");
        IdiotsList.get().save();
    }

    private void loadListeners()
    {
        chatListener = new ChatListener();
        playerListener = new PlayerListener();
        anvilListener = new AnvilListener();
        bookListener = new BookListener();
        commandPreProcessListener = new CommandPreProcessListener();
        signListener = new SignListener();
    }

    private void reloadBansConfig()
    {
        IdiotsList.get().reload();
    }
}
