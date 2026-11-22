package eu.koolfreedom.command.impl;

import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.utilities.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@CommandParameters(name = "obliterate", description = "Force remove someone from the server (punishment stored in plugin DB)", usage = "/<command> <player>", aliases = {"execute"})
public class ObliterateCommand extends KoolCommand
{
    final PermBansList banManager = PermBansList.get();

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
            return false;

        final Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return false;
        }

        broadcast("<red><player> shouldn't have done that :)",
                Placeholder.unparsed("player", target.getName()));

        target.setFireTicks(200);
        for (int i = 0; i < 30; i++)
        {
            target.getWorld().strikeLightningEffect(target.getLocation());
        }

        if (target.isOp()) target.setOp(false);
        if (target.isWhitelisted()) target.setWhitelisted(false);

        final String reason = args.length > 1 ? " (" + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) + ")" : "";
        banManager.banPlayer(target, "You've met with a terrible fate, haven't you? " + reason);
        FUtil.staffAction(sender, "Banned <player>",
                Placeholder.unparsed("player", target.getName()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}
