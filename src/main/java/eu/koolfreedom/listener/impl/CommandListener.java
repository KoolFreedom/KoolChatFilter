package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener extends KoolListener
{
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        Player player = event.getPlayer();
        String msg = event.getMessage(); // full command including leading slash

        // Remove the leading slash
        String content = msg.startsWith("/") ? msg.substring(1) : msg;

        // Check the entire command, NOT just args
        if (!FUtil.containsBlockedWord(content)) {
            return;
        }

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(KoolChatFilter.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }

            IdiotsList.get().banPlayer(player, "Hate Speech (via Commands)");
            IdiotsList.get().save();
            IdiotsList.get().reload();

            FUtil.staffAction(
                    Bukkit.getConsoleSender(),
                    "Permanently banning <player> for sending a filtered word through command",
                    Placeholder.unparsed("player", player.getName())
            );

            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "discord bcast **Player " + player.getName() + " has been permanently banned for filtered command content**"
            );

            player.kick(FUtil.miniMessage("<red>You shouldn't have done that."));
        });
    }

}
