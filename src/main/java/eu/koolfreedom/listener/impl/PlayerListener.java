package eu.koolfreedom.listener.impl;

import eu.koolfreedom.banning.PermBansList;
import eu.koolfreedom.banning.PunishmentType;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import java.net.InetAddress;

@SuppressWarnings({"ConstantConditions"})
public class PlayerListener extends KoolListener
{
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPermBannedPlayerLogin(AsyncPlayerPreLoginEvent event)
    {
        PermBansList permBansList = PermBansList.get();

        String currentName = event.getName().toLowerCase();
        String uuid = event.getUniqueId().toString();

        InetAddress addr = event.getAddress();
        String ip = addr != null ? addr.getHostAddress() : null;

        boolean nameBanned = permBansList.isNameBanned(currentName);
        boolean uuidBanned = permBansList.isUuidBanned(uuid);
        boolean ipBanned = ip != null && permBansList.isIpBanned(ip);

        if (!(nameBanned || uuidBanned || ipBanned)) {
            return; // not banned
        }

        String entryName;
        if (nameBanned)
        {
            entryName = currentName;
        }
        else if (uuidBanned)
        {
            entryName = permBansList.findBannedNameByUuid(uuid).orElse(currentName);
        }
        else
        {
            entryName = permBansList.findBannedNameByIp(ip).orElse(currentName);
        }

        String banType = nameBanned ? "username" : uuidBanned ? "UUID" : "IP address";
        Component reason = permBansList.getReasonComponent(entryName);

        String banDuration = permBansList.getEntry(entryName)
                .filter(entry -> entry.getPunishment() == PunishmentType.TEMPORARY_BAN)
                .map(entry -> "temporarily (expires in "
                        + PermBansList.formatDuration(entry.getExpiresAtMillis() - System.currentTimeMillis()) + ") ")
                .orElse("indefinitely ");
        Component kickMessage = FUtil.miniMessage("""
            <red>Your <ban_type> is <ban_duration>banned from this server!
            <red>Reason:</red> <gold><reason>
            <red>Appeal at:</red> <gold><ban_url>
            """,
                Placeholder.component("reason", reason),
                Placeholder.unparsed("ban_type", banType),
                Placeholder.unparsed("ban_duration", banDuration),
                Placeholder.unparsed("ban_url", ConfigEntry.BAN_URL.getString())
        );

        // disallow login
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);

        // Broadcasts to admins
        FUtil.broadcast("kfc.admin",
                "<#ffb373><player><gray> tried joining, but they're banned!",
                Placeholder.unparsed("player", event.getName())
        );
    }
}