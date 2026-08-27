package eu.koolfreedom.listener.impl;

import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.filter.FilterResult;
import eu.koolfreedom.filter.FilterService;
import eu.koolfreedom.filter.FilterSource;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FLog;
import eu.koolfreedom.utilities.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FilterListener extends KoolListener
{
    private final FilterService service;
    // This is a set of players who have been punished for chat messages,
    // to prevent multiple punishments for the same message.
    private final Set<UUID> chatPunishments = ConcurrentHashMap.newKeySet();

    public FilterListener(FilterService service)
    {
        this.service = service;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        String message = FUtil.plainText(event.message());
        FilterResult result = service.check(message);
        if (!result.matched()) return;

        event.setCancelled(true);
        punishChatOnce(event.getPlayer(), message, result);
    }

    /**
     * Handles the legacy chat event to ensure that the canceled AsyncChatEvent still doesn't show in DiscordSRV bridge.
     * Why do I have to do this? Because DiscordSRV is a piece of garbage and doesn't respect AsyncChatEvent.
     */
    @EventHandler @SuppressWarnings("deprecation")
    public void onLegacyChat(AsyncPlayerChatEvent event)
    {
        String message = event.getMessage();
        FilterResult result = service.check(message);
        if (!result.matched()) return;

        event.setCancelled(true);
        punishChatOnce(event.getPlayer(), message, result);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        String command = event.getMessage();
        FilterResult result = service.check(command);
        if (!result.matched()) return;

        event.setCancelled(true);
        punish(event.getPlayer(), FilterSource.COMMAND, command, result);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event)
    {
        for (Component line : event.lines())
        {
            String text = FUtil.plainText(line);
            FilterResult result = service.check(text);
            if (!result.matched()) continue;

            event.setCancelled(true);
            punish(event.getPlayer(), FilterSource.SIGN, text, result);
            return;
        }
    }

    @EventHandler
    public void onBookEdit(PlayerEditBookEvent event)
    {
        for (Component page : event.getNewBookMeta().pages())
        {
            String text = FUtil.plainText(page);
            FilterResult result = service.check(text);
            if (!result.matched()) continue;

            event.setCancelled(true);
            punish(event.getPlayer(), FilterSource.BOOK, text, result);
            return;
        }
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event)
    {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (event.getResult() == null || !event.getResult().hasItemMeta()) return;
        if (!event.getResult().getItemMeta().hasDisplayName()) return;

        String name = FUtil.plainText(event.getResult().getItemMeta().displayName());
        FilterResult result = service.check(name);
        if (!result.matched()) return;

        event.setResult(null);
        punish(player, FilterSource.ANVIL, name, result);
    }

    private void punish(Player player, FilterSource source, String content, FilterResult result)
    {
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            if (!player.isOnline()) return;

            String reason = "Hate Speech (" + source.name() + ")";
            PermBansList.get().banPlayer(player, reason);
            PermBansList.get().save();
            PermBansList.get().reload();

            FilterService.crashPlayer(player);

            FLog.info("Filter matched {0} for {1} in {2}: {3}",
                    result.expression(), player.getName(), source.name(), content);
            FUtil.staffAction(Bukkit.getConsoleSender(), "<source> filter triggered by <player> | Player has been permanently banned.",
                    Placeholder.unparsed("source", source.name()),
                    Placeholder.unparsed("player", player.getName()));
            FUtil.broadcast("kfc.admin",
                    "<source> filter matched <rule> for <player>: <content>",
                    Placeholder.unparsed("source", source.name()),
                    Placeholder.unparsed("rule", result.expression()),
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.unparsed("content", content));
            player.kick(FUtil.miniMessage("<dark_red>Prohibited language detected.</dark_red>"));
        });
    }

    /**
     * Punishes a player for a chat message, but only once per second to prevent multiple punishments for the same message.
     */
    private void punishChatOnce(Player player, String content, FilterResult result)
    {
        if (!chatPunishments.add(player.getUniqueId())) return;

        punish(player, FilterSource.CHAT, content, result);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> chatPunishments.remove(player.getUniqueId()), 20L);
    }
}