package eu.koolfreedom;

import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.command.impl.CrashCommand;
import eu.koolfreedom.filter.FilterEngine;
import eu.koolfreedom.listener.impl.*;
import eu.koolfreedom.utilities.BuildProperties;
import eu.koolfreedom.utilities.FLog;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

        FilterEngine.reload();

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
        chatListener = new ChatListener();
        playerListener = new PlayerListener();
        anvilListener = new AnvilListener();
        bookListener = new BookListener();
        commandPreProcessListener = new CommandPreProcessListener();
        signListener = new SignListener();
    }

    private void reloadBansConfig()
    {
        PermBansList.get().reload();
    }

    public static void filterLogger(Component message)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (!player.hasPermission("kfc.admin")) continue;

            player.sendMessage(Component.newline()
                    .append(Component.text("[", NamedTextColor.DARK_GRAY))
                    .append(Component.text("KoolFreedom | Chat Filter", NamedTextColor.RED))
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(message)
                    .appendNewline());
        }
        FLog.info(message);
    }
}
