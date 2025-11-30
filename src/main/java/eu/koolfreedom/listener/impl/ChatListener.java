package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener extends KoolListener
{
    @EventHandler @SuppressWarnings("deprecation")
    private void onPlayerChatMessageThatIsASlur(AsyncPlayerChatEvent event)
    {
        Player player = event.getPlayer();
        String rawMessage = event.getMessage();

        // All filtering logic now in FilterUtil
        if (!FUtil.containsBlockedWord(rawMessage)) {
            return; // safe message
        }

        // Cancel message for all viewers
        event.setCancelled(true);

        // Echo back the "unfiltered format" so players see what they said
        player.sendMessage(String.format(event.getFormat(), player.getDisplayName(), rawMessage));

        String reason = "<red>You shouldn't have done that.";

        // Switch to main thread for banning/kicking
        Bukkit.getScheduler().runTask(KoolChatFilter.getInstance(), () ->
        {
            if (!player.isOnline())
            {
                // Fallback: if they left mid-message
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "tempban " + player.getName() + " 999y Hate Speech"
                );
                return;
            }

            // Add to idiots list
            IdiotsList.get().banPlayer(player, "Hate Speech");
            IdiotsList.get().save();
            IdiotsList.get().reload();

            // Staff broadcast
            FUtil.staffAction(
                    Bukkit.getConsoleSender(),
                    "Permanently banning <player> for saying a filtered word",
                    Placeholder.unparsed("player", player.getName())
            );

            // Discord broadcast
            if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV"))
            {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "discord bcast **Player " + player.getName()
                                + " has been permanently banned for saying a filtered word**"
                );
            }

            // Fun effect + kick
            crashPlayer(player);
            player.kick(FUtil.miniMessage(reason));
        });
    }

    private void crashPlayer(Player victim)
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
