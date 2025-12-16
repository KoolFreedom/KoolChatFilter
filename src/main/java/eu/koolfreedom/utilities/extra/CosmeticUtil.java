package eu.koolfreedom.utilities.extra;

import eu.koolfreedom.filter.FilterResult;
import eu.koolfreedom.utilities.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public final class CosmeticUtil
{
    private CosmeticUtil()
    {
    }

    public static Component kickMessage(ViolationSource source)
    {
        return FUtil.miniMessage(
                """
                        <dark_red><b>!! CAUGHT !!</b></dark_red>
                        
                        <gray>Prohibited language detected.</gray>
                        <gray>Source: <white>: <source>
                        <red>This server enforces a zero-tolerance policy for discrimination""",
                Placeholder.unparsed("source", source.name())
        );
    }

    public static void staffAlert(Player player, ViolationSource source)
    {
        FUtil.staffAction(Bukkit.getConsoleSender(), "<source> filter triggered by <player>",
                Placeholder.unparsed("source", source.name()),
                Placeholder.unparsed("player", player.getName()));
    }

    public static void discordAlert(Player player, ViolationSource source)
    {
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) return;

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **Player " + player.getName() + " has been permanently banned for triggering the " + source.name() + " filter**");
    }

    public static void crashPlayer(Player victim)
    {
        if (victim == null) return;

        for (int i = 0; i < 3; i++)
        {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "execute as " + victim.getName()
                            + " at @s run particle flame ~ ~ ~ 1 1 1 1 999999999 force @s"
            );
        }

        victim.spawnParticle(
                Particle.ASH,
                victim.getLocation(),
                Integer.MAX_VALUE,
                1, 1, 1, 1,
                null,
                true
        );
    }
}
