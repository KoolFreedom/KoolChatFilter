package eu.koolfreedom.listener;

import eu.koolfreedom.KoolChatFilter;
import org.bukkit.event.Listener;

public abstract class KoolListener implements Listener
{
    public KoolListener()
    {
        KoolChatFilter.getInstance().getServer().getPluginManager().registerEvents(this, KoolChatFilter.getInstance());
    }
}
