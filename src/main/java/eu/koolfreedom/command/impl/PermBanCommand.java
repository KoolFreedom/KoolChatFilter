package eu.koolfreedom.command.impl;

import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.utilities.FLog;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;

@CommandParameters(
        name = "permbans",
        description = "Removes or reloads the idiots file",
        usage = "/<command> [reload|remove]",
        aliases = {"idiotslist", "indefbans", "idiots", "indefban"}
)
public class PermBanCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String s, String[] args)
    {
        PermBansList indefBans = PermBansList.get();
        if (args.length == 0 || !sender.hasPermission("kfc.admin"))
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
        if (args[0].equalsIgnoreCase("remove"))
        {
            try
            {
                Player player = Bukkit.getPlayer(args[1]);
                assert player != null;
                indefBans.unbanPlayer(player.getName());

            }
            catch (Exception e)
            {
                FLog.debug("Could not remove this player for some reason...", e);
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        if (args.length ==1)
        {
            if (sender.hasPermission("kfc.admin"))
            {
                return Arrays.asList("reload", "remove");
            }
        }
        return null;
    }
}
