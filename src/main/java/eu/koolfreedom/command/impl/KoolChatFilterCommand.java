package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
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
@SuppressWarnings("UnstableApiUsage")
public class KoolChatFilterCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String s, String[] args)
    {
        if (args.length == 0 || !sender.hasPermission("kfc.command.reload"))
        {
            BuildProperties build = plugin.getBuildMeta();
            msg(sender, "<white><b>KoolChatFilter - A very mean chat filter plugin");
            msg(sender, "<gray>Version <white><version>.<build>", Placeholder.unparsed("version", build.getVersion()),
                    Placeholder.unparsed("build", build.getNumber()));
            msg(sender, "<gray>Compiled on <white><date></white> by <white><builder></white>.",
                    Placeholder.unparsed("date", build.getDate()),
                    Placeholder.unparsed("builder", build.getAuthor()));
            msg(sender, "<gray>Author: <white><authors>",
                    Placeholder.unparsed("authors", String.valueOf(plugin.getPluginMeta().getAuthors())));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            try
            {
                KoolChatFilter.getInstance().reloadConfig();
                msg(sender, "<green>VenomCore successfully reloaded");
            }
            catch (Exception e)
            {
                FLog.error("Failed to load configuration", e);
                msg(sender, "<red>An error occurred whilst attempting to reload the configuration.");
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
