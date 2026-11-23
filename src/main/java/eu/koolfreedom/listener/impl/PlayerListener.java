package eu.koolfreedom.listener.impl;

import eu.koolfreedom.banning.*;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.net.InetAddress;
import java.util.Locale;

public final class PlayerListener extends KoolListener
{
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPermBannedPlayerLogin(AsyncPlayerPreLoginEvent event)
    {
        final PermBansList bans = PermBansList.get();
        final String name = event.getName().toLowerCase(Locale.ROOT);
        final String uuid = event.getUniqueId().toString().toLowerCase(Locale.ROOT);

        final BanMatch match = findBanMatch(bans, name, uuid, event.getAddress());
        if (match == null) return;

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                bans.createKickMessage(bans, match));

        FUtil.broadcast("koolchatfilter.staff",
                "<#ffb373><player><gray> tried joining, but they're banned!",
                Placeholder.unparsed("player", event.getName()));
    }

    private BanMatch findBanMatch(PermBansList bans, String name, String uuid, InetAddress address)
    {
        if (bans.isNameBanned(name))
        {
            return new BanMatch(name, "username");
        }

        if (bans.isUuidBanned(uuid))
        {
            return new BanMatch(bans.findBannedNameByUuid(uuid).orElse(name), "UUID");
        }

        if (address != null)
        {
            final String ip = address.getHostAddress();
            if (bans.isIpBanned(ip))
            {
                return new BanMatch(bans.findBannedNameByIp(ip).orElse(name), "IP address");
            }
        }

        return null;
    }
}
