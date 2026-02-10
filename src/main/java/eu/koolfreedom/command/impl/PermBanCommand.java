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
        usage = "/<command> [reload|remove <type> <value>]",
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
            if (args.length < 3)
            {
                msg(sender, "<red>Usage: /permbans remove <name|uuid|ip> <value>");
                return true;
            }

            String type = args[1].toLowerCase();
            String value = args[2];
            boolean unbanned = false;

            try
            {
                if (type.equalsIgnoreCase("name"))
                {
                    unbanned = indefBans.unbanPlayer(value);
                    if (unbanned)
                    {
                        msg(sender, "<green>Unbanned player: <player>",
                                Placeholder.unparsed("player", value));
                    }
                    else
                    {
                        msg(sender, "<red>Player <player> was not found in ban list.",
                                Placeholder.unparsed("player", value));
                    }
                }
                else if (type.equalsIgnoreCase("uuid"))
                {
                    unbanned = indefBans.unbanPlayerByUuid(value);
                    if (unbanned)
                    {
                        msg(sender, "<green>Unbanned player by UUID: <uuid>",
                                Placeholder.unparsed("uuid", value));
                    }
                    else
                    {
                        msg(sender, "<red>No player found with UUID: <uuid>",
                                Placeholder.unparsed("uuid", value));
                    }
                }
                else if (type.equalsIgnoreCase("ip"))
                {
                    unbanned = indefBans.unbanPlayerByIp(value);
                    if (unbanned)
                    {
                        msg(sender, "<green>Unbanned player by IP: <ip>",
                                Placeholder.unparsed("ip", value));
                    }
                    else
                    {
                        msg(sender, "<red>No player found with IP: <ip>",
                                Placeholder.unparsed("ip", value));
                    }
                }
                else
                {
                    msg(sender, "<red>Invalid type. Use: name, uuid, or ip");
                }
            }
            catch (Exception e)
            {
                FLog.debug("Error unbanning player", e);
                msg(sender, "<red>An error occurred while unbanning the player.");
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        if (!sender.hasPermission("kfc.admin"))
        {
            return null;
        }

        if (args.length == 1)
        {
            return Arrays.asList("reload", "remove");
        }

        // if "remove", show unban types
        if (args.length == 2 && args[0].equalsIgnoreCase("remove"))
        {
            return Arrays.asList("name", "uuid", "ip");
        }

        // depending on type, provide suggestions
        if (args.length == 3 && args[0].equalsIgnoreCase("remove"))
        {
            String type = args[1].toLowerCase();
            if (type.equalsIgnoreCase("name"))
            {
                // could filter banned players but i'm not smart lol
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
            }
            // For UUID and IP, we can't easily tab-complete without showing all bans
            // So we return null to let the user type freely
        }

        return null;
    }
}
