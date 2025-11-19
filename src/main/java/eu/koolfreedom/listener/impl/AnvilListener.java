package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AnvilListener extends KoolListener {

    @EventHandler @SuppressWarnings("deprecation")
    public void onAnvilRename(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory inv)) return;
        if (event.getSlot() != 2) return; // result slot only

        Player player = (Player) event.getWhoClicked();
        ItemStack item = inv.getItem(2);

        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String name = meta.getDisplayName();

        if (!FUtil.containsBlockedWord(name)) return;

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(KoolChatFilter.getInstance(), () -> {
            if (!player.isOnline()) return;

            IdiotsList.get().banPlayer(player, "Hate Speech (Anvil Rename)");
            IdiotsList.get().save();
            IdiotsList.get().reload();

            FUtil.staffAction(Bukkit.getConsoleSender(),
                    "Permanently banning <player> for renaming an item to a filtered word via anvil",
                    Placeholder.unparsed("player", player.getName()));

            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "discord bcast **Player " + player.getName() + " has been permanently banned for filtered anvil item name**"
            );

            player.kick(FUtil.miniMessage("<red>You shouldn't have done that."));
        });
    }
}
