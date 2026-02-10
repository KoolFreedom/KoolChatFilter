package eu.koolfreedom.listener.impl;

import eu.koolfreedom.banning.PermBansList;
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
        PermBansList idot = PermBansList.get();

        String currentName = event.getName().toLowerCase();
        String uuid = event.getUniqueId().toString();

        InetAddress addr = event.getAddress();
        String ip = addr != null ? addr.getHostAddress() : null;

        boolean nameBanned = idot.isNameBanned(currentName);
        boolean uuidBanned = idot.isUuidBanned(uuid);
        boolean ipBanned = ip != null && idot.isIpBanned(ip);

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
            entryName = idot.findBannedNameByUuid(uuid).orElse(currentName);
        }
        else
        {
            entryName = idot.findBannedNameByIp(ip).orElse(currentName);
        }

        String banType = nameBanned ? "username" : uuidBanned ? "UUID" : "IP address";
        Component reason = idot.getReasonComponent(entryName);

        Component kickMessage = FUtil.miniMessage("""
            <red>Your <ban_type> is indefinitely banned from this server!
            <red>Reason:</red> <gold><reason>
            <red>Appeal at:</red> <gold><ban_url>
            """,
                Placeholder.component("reason", reason),
                Placeholder.unparsed("ban_type", banType),
                Placeholder.unparsed("ban_url", ConfigEntry.BAN_URL.getString())
        );

        // disallow login
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);

        // Broadcasts to admins
        FUtil.broadcast("kfc.admin",
                "<#ffb373><player><gray> tried joining, but they're on the idiots list.",
                Placeholder.unparsed("player", event.getName())
        );
    }
}
