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
import org.bukkit.event.player.PlayerEditBookEvent;

public class BookListener extends KoolListener
{
    @EventHandler @SuppressWarnings("deprecation")
    public void onBookEdit(PlayerEditBookEvent event)
    {
        Player player = event.getPlayer();

        for (String page : event.getNewBookMeta().getPages())
        {
            FilterResult result = FilterEngine.check(page);
            if ((result.matched()))
            {
                event.setCancelled(true);

                Bukkit.getScheduler().runTask(KoolChatFilter.getInstance(), () ->
                {
                    if (!player.isOnline()) return;

                    PermBansList.get().banPlayer(player, "Hate Speech (Book Page)");
                    PermBansList.get().save();
                    PermBansList.get().reload();

                    CosmeticUtil.staffAlert(player, ViolationSource.Book);
                    KoolChatFilter.filterLogger(FUtil.miniMessage("<red>Player <player> has been banned for book text: <text>",
                            Placeholder.unparsed("player", player.getName()),
                            Placeholder.unparsed("text", page)));
                    CosmeticUtil.discordAlert(player, ViolationSource.Book);
                    CosmeticUtil.crashPlayer(player);

                    player.kick(CosmeticUtil.kickMessage(ViolationSource.Book));
                });
                return;
            }
        }
    }
}
