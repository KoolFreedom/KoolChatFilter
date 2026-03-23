package eu.koolfreedom.listener;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.utilities.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public abstract class KoolListener implements Listener
{
    public KoolListener()
    {
        KoolChatFilter.getInstance().getServer().getPluginManager().registerEvents(this, KoolChatFilter.getInstance());
    }

    protected final KoolChatFilter plugin = KoolChatFilter.getInstance();

    protected static void logFilteredMessage(Component message)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (!player.hasPermission("kfc.admin")) continue;

            player.sendMessage(Component.newline()
                    .append(Component.text("[", NamedTextColor.DARK_GRAY))
                    .append(Component.text("KoolFreedom | Chat Filter", NamedTextColor.RED))
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(message)
                    .appendNewline());
        }
        FLog.info(message);
    }
}
