package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.filter.FilterEngine;
import eu.koolfreedom.filter.FilterResult;
import eu.koolfreedom.listener.KoolListener;

import eu.koolfreedom.utilities.FUtil;
import eu.koolfreedom.utilities.extra.CosmeticUtil;
import eu.koolfreedom.utilities.extra.ViolationSource;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener extends KoolListener {
    @EventHandler
    @SuppressWarnings("deprecation")
    private void onPlayerChatMessageThatIsASlur(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String rawMessage = event.getMessage();

        FilterResult result = FilterEngine.check(rawMessage);
        if (!result.matched()) return; // safe message

        // Cancel message for all viewers
        event.setCancelled(true);

        // Echo back the "unfiltered format" so players see what they said
        player.sendMessage(String.format(event.getFormat(), player.getDisplayName(), rawMessage));

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            PermBansList.get().banPlayer(player, "Hate Speech");
            PermBansList.get().save();
            PermBansList.get().reload();

            CosmeticUtil.staffAlert(player, ViolationSource.Chat);
            KoolChatFilter.filterLogger(FUtil.miniMessage("<red>Player <player> has been permanently banned for saying: '<word>'",
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.unparsed("word", rawMessage)));
            CosmeticUtil.discordAlert(player, ViolationSource.Chat);
            CosmeticUtil.crashPlayer(player);

            player.kick(CosmeticUtil.kickMessage(ViolationSource.Chat));
        });
    }
}
