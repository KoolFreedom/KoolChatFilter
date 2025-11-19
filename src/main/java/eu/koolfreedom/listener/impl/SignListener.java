package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;

public class SignListener extends KoolListener {

    @EventHandler @SuppressWarnings("deprecation")
    public void onSignWrite(SignChangeEvent event) {
        Player player = event.getPlayer();

        for (String line : event.getLines()) {
            if (FUtil.containsBlockedWord(line)) {
                event.setCancelled(true);

                Bukkit.getScheduler().runTask(KoolChatFilter.getInstance(), () -> {
                    if (!player.isOnline()) return;

                    IdiotsList.get().banPlayer(player, "Hate Speech (Sign)");
                    IdiotsList.get().save();
                    IdiotsList.get().reload();

                    FUtil.staffAction(Bukkit.getConsoleSender(),
                            "Permanently banning <player> for writing a filtered word on a sign",
                            Placeholder.unparsed("player", player.getName()));

                    Bukkit.dispatchCommand(
                            Bukkit.getConsoleSender(),
                            "discord bcast **Player " + player.getName() + " has been permanently banned for filtered sign text**"
                    );

                    player.kick(FUtil.miniMessage("<red>You shouldn't have done that."));
                });
                return;
            }
        }
    }
}
