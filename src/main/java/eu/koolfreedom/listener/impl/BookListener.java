package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;

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
            if (FUtil.containsBlockedWord(page))
            {

                event.setCancelled(true);

                Bukkit.getScheduler().runTask(KoolChatFilter.getInstance(), () ->
                {
                    if (!player.isOnline()) return;

                    IdiotsList.get().banPlayer(player, "Hate Speech (Book Page)");
                    IdiotsList.get().save();
                    IdiotsList.get().reload();

                    FUtil.staffAction(Bukkit.getConsoleSender(),
                            "Permanently banning <player> for filtered book content",
                            Placeholder.unparsed("player", player.getName()));

                    if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV"))
                    {
                        Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                "discord bcast **Player " + player.getName()
                                        + " has been permanently banned for filtered book content**"
                        );
                    }

                    player.kick(FUtil.miniMessage("<red>You shouldn't have done that."));
                });
                return;
            }
        }
    }
}
