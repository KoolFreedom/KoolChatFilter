package eu.koolfreedom;

import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.command.impl.KoolChatFilterCommand;
import eu.koolfreedom.config.MainConfig;
import eu.koolfreedom.listener.impl.FilterListener;
import eu.koolfreedom.filter.FilterService;
import eu.koolfreedom.listener.impl.*;
import eu.koolfreedom.utilities.BuildProperties;
import eu.koolfreedom.utilities.FLog;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
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
        FLog.info("Created by gamingto12 and 0x7694C9");
        FLog.info("Version {}.{}", buildMeta.getVersion(), buildMeta.getNumber());
        FLog.info("Compiled {} by {}", buildMeta.getDate(), buildMeta.getAuthor());

        MainConfig.load();
        FLog.info("Loaded main configuration");

        // https://bstats.org/plugin/bukkit/KoolChatFilter/35498
        try
        {
            new Metrics(this, 35498);
            FLog.info("Enabled Metrics");
        }
        catch (Exception e)
        {
            FLog.error("Could not start Metrics", e);
        }

        commandLoader = new CommandLoader(KoolChatFilterCommand.class);
        commandLoader.loadCommands();
        FLog.info("Loaded {} commands,", commandLoader.getKoolCommands().size());

        filterService = new FilterService();
        FilterService.reload();

        PermBansList.get().reload();
        FLog.info("Reloaded bans");

        loadListeners();
        FLog.info("Loaded listeners");
    }

    @Override
    public void onDisable()
    {
        FLog.info("Disabled KoolChatFilter");
        PermBansList.get().closeDatabase();
    }

    private void loadListeners()
    {
        playerListener = new PlayerListener();
        filterListener = new FilterListener(filterService);
    }
}
