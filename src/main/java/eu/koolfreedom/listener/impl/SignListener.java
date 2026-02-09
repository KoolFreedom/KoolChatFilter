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
import org.bukkit.event.block.SignChangeEvent;

public class SignListener extends KoolListener
{

    @EventHandler @SuppressWarnings("deprecation")
    public void onSignWrite(SignChangeEvent event)
    {
        Player player = event.getPlayer();

        for (String line : event.getLines())
        {
            FilterResult result = FilterEngine.check(line);
            if (result.matched())
            {
                event.setCancelled(true);

                Bukkit.getScheduler().runTask(KoolChatFilter.getInstance(), () ->
                {
                    if (!player.isOnline()) return;

                    PermBansList.get().banPlayer(player, "Hate Speech (Sign)");
                    PermBansList.get().save();
                    PermBansList.get().reload();

                    CosmeticUtil.staffAlert(player, ViolationSource.Sign);
                    KoolChatFilter.filterLogger(FUtil.miniMessage("<red>Player <player> has been permanently banned for sign text: '<line>'",
                            Placeholder.unparsed("player", player.getName()),
                            Placeholder.unparsed("line", line)));
                    CosmeticUtil.discordAlert(player, ViolationSource.Sign);
                    CosmeticUtil.crashPlayer(player);

                    player.kick(CosmeticUtil.kickMessage(ViolationSource.Sign));
                });
                return;
            }
        }
    }
}
