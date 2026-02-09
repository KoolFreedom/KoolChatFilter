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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandPreProcessListener extends KoolListener
{
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        Player player = event.getPlayer();
        String msg = event.getMessage(); // full command including leading slash

        // Remove the leading slash
        String content = msg.startsWith("/") ? msg.substring(1) : msg;

        // Check the entire command, NOT just args
        FilterResult result = FilterEngine.check(content);
        if (!result.matched())
        {
            return; // do nothing
        }

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(KoolChatFilter.getInstance(), () ->
        {
            if (!player.isOnline())
            {
                return; // do nothing... cause what else could you do
            }

            PermBansList.get().banPlayer(player, "Hate Speech (via Commands)");
            PermBansList.get().save();
            PermBansList.get().reload();

            CosmeticUtil.staffAlert(player, ViolationSource.Command);
            KoolChatFilter.filterLogger(FUtil.miniMessage("<red>Player <player> has been permanently banned for command: /<command>",
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.unparsed("command", content)));
            CosmeticUtil.discordAlert(player, ViolationSource.Command);
            CosmeticUtil.crashPlayer(player);

            player.kick(CosmeticUtil.kickMessage(ViolationSource.Command));
        });
    }

}
