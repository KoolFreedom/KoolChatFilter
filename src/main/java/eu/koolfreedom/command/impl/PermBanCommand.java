package eu.koolfreedom.command.impl;

import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.banning.PunishmentType;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.utilities.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@CommandParameters(
        name = "permbans",
        description = "Adds, removes, or reloads permanent bans",
        usage = "/<command> [reload|add <name> <reason>|remove <name>]",
        aliases = {"indefbans", "indefban", "permban"}
)
public class PermBanCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String s, String[] args)
    {
        PermBansList bans = PermBansList.get();
        if (args.length == 0)
        {
            msg(sender, "<gray>There are <count> permbans.",
                    Placeholder.unparsed("count", String.valueOf(bans.getBansCount())));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            bans.reload();
            msg(sender, "<green>Reloaded local punishment entries");
            return true;
        }

        if (args.length < 2)
        {
            msg(sender, "<red>Usage: /permbans <add|remove> <name> [reason]");
            return true;
        }

        String name = args[1];
        if (args[0].equalsIgnoreCase("add"))
        {
            if (args.length < 3)
            {
                msg(sender, "<red>Usage: /permbans add <name> <reason>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(name);
            if (target == null)
            {
                msg(sender, playerNotFound);
                return true;
            }

            if (bans.isNameBanned(target.getName()) || bans.isUuidBanned(target.getUniqueId().toString()))
            {
                msg(sender, "<yellow><player> is already permanently banned.",
                        Placeholder.unparsed("player", target.getName()));
                return true;
            }

            String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            bans.banPlayer(target, reason, PunishmentType.PERMANENT_BAN);
            target.kick(Component.text("You have been permanently banned.\nReason: " + reason));
            msg(sender, "<green>Permanently banned <player>: <reason>",
                    Placeholder.unparsed("player", target.getName()),
                    Placeholder.unparsed("reason", reason));
            return true;
        }

        if (args[0].equalsIgnoreCase("remove"))
        {
            try
            {
                if (bans.unbanPlayer(name))
                {
                    msg(sender, "<green>Unbanned player: <player>", Placeholder.unparsed("player", name));
                    FLog.debug("Cleared {}'s permanent ban entry", name);
                }
                else
                {
                    msg(sender, "<red>Player <player> was not found in the permanent ban list.",
                            Placeholder.unparsed("player", name));
                }
            }
            catch (Exception e)
            {
                FLog.error("Error unbanning player", e);
                msg(sender, "<red>An error occurred while unbanning the player.");
            }
            return true;
        }

        msg(sender, "<red>Usage: /permbans <add|remove> <name>");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        if (!sender.hasPermission("koolchatfilter.admin")) return null;

        if (args.length == 1)
        {
            return Arrays.asList("reload", "add", "remove");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")))
        {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        return null;
    }
}
