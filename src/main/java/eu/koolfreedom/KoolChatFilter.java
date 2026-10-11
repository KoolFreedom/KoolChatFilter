package eu.koolfreedom;

import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.command.impl.CrashCommand;
import eu.koolfreedom.config.MainConfig;
import eu.koolfreedom.listener.impl.FilterListener;
import eu.koolfreedom.filter.FilterService;
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
    private PlayerListener playerListener;
    private FilterService filterService;
    private FilterListener filterListener;

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

        MainConfig.load();
        FLog.info("Loaded main configuration");

        commandLoader = new CommandLoader(CrashCommand.class);
        commandLoader.loadCommands();
        FLog.info("Loaded {} commands,", commandLoader.getKoolCommands().size());

        filterService = new FilterService();
        FilterService.reload();

        reloadBansConfig();

        loadListeners();
        FLog.info("Loaded listeners");
    }

    @Override
    public void onDisable()
    {
        FLog.info("Disabled KoolChatFilter");
        PermBansList.get().save();
    }

    private void loadListeners()
    {
        playerListener = new PlayerListener();
        filterListener = new FilterListener(filterService);
    }

    private void reloadBansConfig()
    {
        PermBansList.get().reload();
    }
}
