package eu.koolfreedom.listener.impl;

import com.destroystokyo.paper.profile.PlayerProfile;
import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.banning.PunishmentType;
import eu.koolfreedom.config.ConfigEntry;
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
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FilterListener extends KoolListener
{
    private final FilterService service;
    private final Set<UUID> muteNotices = ConcurrentHashMap.newKeySet();

    public FilterListener(FilterService service)
    {
        this.service = service;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        if (rejectMutedChat(event.getPlayer()))
        {
            event.setCancelled(true);
            return;
        }

        final Player player = event.getPlayer();
        final String message = FUtil.plainText(event.message());
        final FilterResult result = service.check(message);
        if (!result.matched()) return;

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> punish(player, FilterSource.CHAT, message, result));
    }

    /**
     * This is only here because AsyncChatEvent does not respect AsyncPlayerChatEvent. This matters a lot because if a server is using DiscordSRV,
     *   then whatever filtered text comes through will still show in the Discord bridge. The point of this plugin is to prevent that text from showing at all.
     */
    @EventHandler @SuppressWarnings("deprecation")
    public void onLegacyChat(AsyncPlayerChatEvent event)
    {
        if (rejectMutedChat(event.getPlayer()))
        {
            event.setCancelled(true);
            return;
        }

        final String message = event.getMessage();
        final FilterResult result = service.check(message);
        if (!result.matched()) return;

        event.setCancelled(true); // silently cancel the event | punishment already handled by ACE event listener.
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        final String command = event.getMessage();
        final FilterResult result = service.check(command);
        if (!result.matched()) return;

        event.setCancelled(true);
        punish(event.getPlayer(), FilterSource.COMMAND, command, result);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event)
    {
        for (Component line : event.lines())
        {
            final String text = FUtil.plainText(line);
            final FilterResult result = service.check(text);
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
            final String text = FUtil.plainText(page);
            final FilterResult result = service.check(text);
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

        final String name = FUtil.plainText(event.getResult().getItemMeta().displayName());
        final FilterResult result = service.check(name);
        if (!result.matched()) return;

        event.setResult(null);
        punish(player, FilterSource.ANVIL, name, result);
    }

    /** Filter player usernames in the event that this plugin is used on a cracked/non-premium server */
    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {
        final PlayerProfile profile = event.getPlayerProfile();
        final String playerName = profile.getName();

        // Use the same filter as everything else.
        final FilterResult result = service.check(playerName);
        if (!result.matched()) return;

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, PermBansList.get().getUsernameKickMessage());
    }

    private void punish(Player player, FilterSource source, String content, FilterResult result)
    {
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            if (!player.isOnline()) return;

            final String reason = ConfigEntry.FILTER_REASON.getString();
            final PermBansList banList = PermBansList.get();
            final PunishmentType punishment = banList.getConfiguredPunishment();
            long duration = banList.getConfiguredDurationMillis();
            if (banList.shouldApplyPunishment(player, reason, punishment, duration))
            {
                FLog.error("Could not apply configured {} punishment to {}", punishment, player.getName());
                return;
            }

            FUtil.staffAction(Bukkit.getConsoleSender(), "<source> filter triggered by <player> | Punishment: <punishment>.",
                    Placeholder.unparsed("source", source.name()),
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.unparsed("punishment", punishment.name()));
            FUtil.broadcast("koolchatfilter.staff",
                    "<red><source> filter matched <rule> for <player> (<punishment>): <content>",
                    Placeholder.unparsed("source", source.name()),
                    Placeholder.unparsed("rule", result.expression()),
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.unparsed("punishment", punishment.name()),
                    Placeholder.unparsed("content", content));

            switch (punishment)
            {
                case PERMANENT_BAN, TEMPORARY_BAN, KICK ->
                        player.kick(FUtil.miniMessage("<dark_red>Prohibited language detected.</dark_red>"));
                case MUTE -> player.sendMessage(FUtil.miniMessage("<red>You have been muted for prohibited language.</red>"));
                case WARNING -> player.sendMessage(FUtil.miniMessage(
                        "<red>Warning: prohibited language detected.</red>"));
                case LOG_ONLY -> {}
            }
        });
    }

    private boolean rejectMutedChat(Player player)
    {
        if (!PermBansList.get().isMuted(player.getUniqueId().toString())) return false;
        final UUID uuid = player.getUniqueId();
        if (muteNotices.add(uuid))
        {
            Bukkit.getScheduler().runTask(plugin, () ->
            {
                if (player.isOnline())
                {
                    player.sendMessage(FUtil.miniMessage("<red>You are muted.</red>"));
                }
            });
            Bukkit.getScheduler().runTaskLater(plugin, () -> muteNotices.remove(uuid), 20L);
        }
        return true;
    }
}