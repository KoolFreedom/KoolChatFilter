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

        FilterResult result = FilterEngine.check(name);
        if (!result.matched()) return; // safe message

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return; // can't do anything :(

            PermBansList.get().banPlayer(player, "Hate Speech (Item Rename via Anvil)");
            PermBansList.get().save();
            PermBansList.get().reload();

            CosmeticUtil.staffAlert(player, ViolationSource.Anvil);
            KoolChatFilter.filterLogger(FUtil.miniMessage("<red>Player <player> has been permanently banned for renaming their item to <message>",
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.unparsed("message", name)));
            CosmeticUtil.discordAlert(player, ViolationSource.Anvil);
            CosmeticUtil.crashPlayer(player);
            player.kick(CosmeticUtil.kickMessage(ViolationSource.Anvil));
        });
    }
}
