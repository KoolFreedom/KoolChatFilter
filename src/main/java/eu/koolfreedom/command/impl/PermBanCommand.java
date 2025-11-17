package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.File;
import java.util.List;

@CommandParameters(
        name = "permbans",
        description = "Removes or reloads the idiots file",
        usage = "/<command> [reload]",
        aliases = {"idiotslist", "indefbans", "idiots"}
)
public class PermBanCommand extends KoolCommand
{
    private final File file = new File(KoolChatFilter.getInstance().getDataFolder(), "idiots.yml");

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String s, String[] args)
    {
        IdiotsList indefBans = IdiotsList.get();
        if (args.length == 0 || !sender.hasPermission("kfc.command.banlist.reload"))
        {
            msg(sender, "<gray>There are <count> idiots.",
                    Placeholder.unparsed("count", String.valueOf(indefBans.getBansCount())));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            indefBans.reload();
            msg(sender, "<green>Reloaded the idiots file");
            return true;
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        return args.length == 1 && sender.hasPermission("kfc.command.banlist.reload") ? List.of("reload") : null;
    }
}
