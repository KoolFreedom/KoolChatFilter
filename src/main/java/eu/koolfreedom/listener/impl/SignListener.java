package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
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

                    IdiotsList.get().banPlayer(player, "Hate Speech (Sign)");
                    IdiotsList.get().save();
                    IdiotsList.get().reload();

                    CosmeticUtil.staffAlert(player, ViolationSource.SIGN);
                    CosmeticUtil.discordAlert(player, ViolationSource.SIGN);
                    CosmeticUtil.crashPlayer(player);

                    player.kick(CosmeticUtil.kickMessage(ViolationSource.SIGN));
                });
                return;
            }
        }
    }
}
