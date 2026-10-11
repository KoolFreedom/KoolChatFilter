package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.config.MainConfig;
import eu.koolfreedom.filter.FilterService;
import eu.koolfreedom.utilities.BuildProperties;
import eu.koolfreedom.utilities.FLog;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@CommandParameters(
        name = "koolchatfilter",
        description = "Gives a description about the plugin or reloads it",
        usage = "/<command> [reload]",
        aliases = {"kfc", "koolfilter"}
)
public class KoolChatFilterCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String s, String[] args)
    {
        if (args.length == 0 || !sender.hasPermission("kfc.admin"))
        {
            final String authors = String.join(", ", plugin.getPluginMeta().getAuthors());
            BuildProperties build = plugin.getBuildMeta();
            msg(sender,
                    "<gradient:#38d6c8:#8eb8ff><bold>KoolChatFilter</bold> <dark_gray>• <gray>Chat filter plugin");
            msg(sender, "<gray>Version <white>" + build.getVersion() + "." + build.getNumber());
            msg(sender, "<gray>Built <white>" + build.getDate() + " <dark_gray>by <white>" + build.getAuthor());
            msg(sender, "<gray>Authors <white><authors>",
                    Placeholder.unparsed("authors", authors));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            try
            {
                MainConfig.load();
                FilterService.reload();
                msg(sender, "<#a6da95>Configuration successfully reloaded");
            }
            catch (Exception e)
            {
                FLog.error("Failed to load configuration", e);
                msg(sender, "<#d20f39>An error occurred whilst attempting to reload the configuration.");
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args)
    {
        if (sender.hasPermission("kfc.command.reload") && args.length == 1)
        {
            return Collections.singletonList("reload");
        }
        else
        {
            return List.of();
        }
    }
}
