package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

@CommandParameters(name = "obliterate", description = "Unleash hell upon someone.", usage = "/<command> <player> <reason>", aliases = {"doom"})
public class ObliterateCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        for (int i = 0; i < 30; i++)
        {
            target.getWorld().strikeLightningEffect(target.getLocation());
        }

        target.setFireTicks(200);
        target.setGameMode(GameMode.ADVENTURE);

        broadcast("<red><sender> is swinging the Russian Hammer over <target>!",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("target", target.getName()));

        Bukkit.getScheduler().runTaskLater(KoolChatFilter.getInstance(), () -> broadcast("<red><target> will be completely eviscerated!",
                Placeholder.unparsed("target", target.getName())), 2);

        Bukkit.getScheduler().runTaskLater(KoolChatFilter.getInstance(), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " clear");
            if (target.isOp()) target.setOp(false);
            if (target.isWhitelisted()) target.setWhitelisted(false);
        }, 2);

        Bukkit.getScheduler().runTaskLater(KoolChatFilter.getInstance(), () -> target.setHealth(0), 10);

        Bukkit.getScheduler().runTaskLater(KoolChatFilter.getInstance(), () -> broadcast("<red><target> has been eradicated from existence!",
                Placeholder.unparsed("target", target.getName())), 30);

        for (int i = 0; i < 3; i++) {
            Bukkit.getScheduler().runTaskLater(KoolChatFilter.getInstance(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as \"" + target.getName() + "\" at @s run particle flame ~ ~ ~ 1 1 1 1 999999999 force @s"), 30);
        }

        String reason = args.length > 1 ? " (" + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) + ")" : "";
        Bukkit.getScheduler().runTaskLater(KoolChatFilter.getInstance(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tempban " + target.getName() + " 999y You've met with a terrible fate, haven't you, " + target.getName() + "?" + reason), 38);
        return true;
    }

}
