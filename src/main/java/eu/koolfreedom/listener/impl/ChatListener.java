package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.filter.FilterEngine;
import eu.koolfreedom.filter.FilterResult;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;

import eu.koolfreedom.utilities.extra.CosmeticUtil;
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

        FilterResult result = FilterEngine.check(rawMessage);
        if(!result.matched()) return; // safe message

        // Cancel message for all viewers
        event.setCancelled(true);

        // Echo back the "unfiltered format" so players see what they said
        player.sendMessage(String.format(event.getFormat(), player.getDisplayName(), rawMessage));

        Bukkit.getScheduler().runTask(plugin () -> {
            // TODO: implement the rest of the logic
    })
    }
}
