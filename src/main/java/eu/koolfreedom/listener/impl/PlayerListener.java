package eu.koolfreedom.listener.impl;

import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;
import eu.koolfreedom.utilities.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;

@SuppressWarnings("ConstantConditions")
public class PlayerListener extends KoolListener
{
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPermBannedPlayerLogin(PlayerLoginEvent event)
    {
        Player player = event.getPlayer();
        IdiotsList idot = IdiotsList.get();

        String currentName = player.getName().toLowerCase();
        String uuid = player.getUniqueId().toString();
        String ip = null;
        if (event.getAddress() != null)
        {
            ip = event.getAddress().getHostAddress();
        }

        // Check how they're banned
        boolean nameBanned = idot.isNameBanned(currentName);
        boolean uuidBanned = idot.isUuidBanned(uuid);
        boolean ipBanned = ip != null && idot.isIpBanned(ip);

        if (!(nameBanned || uuidBanned || ipBanned))
        {
            return; // noting
        }

        // Resolve the YAML key
        String entryName;
        if (nameBanned)
        {
            entryName = currentName;
        }
        else if (uuidBanned)
        {
            entryName = idot.findBannedNameByUuid(uuid).orElse(currentName);
        }
        else if (ipBanned)
        {
            entryName = idot.findBannedNameByIp(ip).orElse(currentName);
        }
        else
        {
            entryName = currentName; // Fallback entry
        }

        // Determine how they're banned to make the reason correct
        String banType = nameBanned ? "username" : uuidBanned ? "UUID" : "IP address";

        Component reason = idot.getReasonComponent(entryName);

        // Build kick message
        Component kickMessage = FUtil.miniMessage("""
        <red>Your <ban_type> is indefinitely banned from this server!
        <red>Reason:</red> <gold><reason>
        <red>Appeal at:</red> <gold>https://discord.gg/MTYrSgVkmd
        """,
                Placeholder.component("reason", reason),
                Placeholder.unparsed("ban_type", banType)
        );

        event.disallow(PlayerLoginEvent.Result.KICK_BANNED, kickMessage);

        // Broadcasts to admins
        String playerName = event.getPlayer().getName();
        FUtil.broadcast("kfc.admin",
                "<#ffb373><player><gray> tried joining, but they're on the idiots list.", Placeholder.unparsed("player", playerName));
        FLog.info(FUtil.miniMessage("<#ffb373><player><gray> tried joining, but they're on the idiots list.", Placeholder.unparsed("player", playerName)));
    }
}
