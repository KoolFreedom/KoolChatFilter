package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.utilities.extra.CosmeticUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "crash", description = "Crash people's clients.", usage = "/<command> [player]",
        aliases = {"370"})
public class CrashCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            msg(sender, "<red>You shouldn't have done that.");

            if (playerSender != null) {
                CosmeticUtil.crashPlayer(playerSender);
            } else {
                msg(sender, "<red>...but since you're console, you get to live.");
            }

        }
        else
        {
            Player player = Bukkit.getPlayer(args[0]);

            if (player != null)
            {
                CosmeticUtil.crashPlayer(player);
                msg(sender, "<green>Your wish is my command.");
            }
            else
            {
                if (playerSender == null)
                {
                    msg(sender, "<red>We couldn't find that player. Since you're doing this from console, we can't reflect it back at you :(");
                }
                else
                {
                    msg(sender, "<red>We couldn't find that player, so we're going to do it to you instead :)");
                    CosmeticUtil.crashPlayer(playerSender);
                }
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}