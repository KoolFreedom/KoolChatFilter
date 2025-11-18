package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.utilities.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.text.Normalizer;
import java.util.List;

public class ChatListener extends KoolListener
{
    @EventHandler @SuppressWarnings("deprecation")
    private void onPlayerChatMessageThatIsASlur(AsyncPlayerChatEvent event)
    {
        Player player = event.getPlayer();

        // Normalize the message
        String rawMessage = event.getMessage();
        String message = normalize(rawMessage).toLowerCase();
        String cleanMessage = stripNonLetters(message);

        List<String> blockedWords = KoolChatFilter.getInstance().getConfig().getStringList("filtered-stuff");

        for (String word : blockedWords)
        {
            String cleanWord = stripNonLetters(normalize(word.toLowerCase()));

            if (message.contains(word.toLowerCase()) || cleanMessage.contains(cleanWord))
            {
                event.setCancelled(true);
                player.sendMessage(String.format(event.getFormat(), player.getDisplayName(), rawMessage));

                String reason = "<red>You shouldn't have done that.";

                // Schedule on main thread
                Bukkit.getScheduler().runTask(KoolChatFilter.getInstance(), () -> {
                    if (player.isOnline())
                    {
                        // Add to the idiot list
                        IdiotsList.get().banPlayer(player, "Hate Speech");
                        IdiotsList.get().save();
                        IdiotsList.get().reload();

                        // Broadcast
                        FUtil.staffAction(Bukkit.getConsoleSender(), "Permanently banning <player> for saying a filtered word",
                                Placeholder.unparsed("player", player.getName()));
                        Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                "discord bcast **Player " + player.getName() + " has been permanently banned for saying a filtered word**");

                        // Kick player safely
                        crashPlayer(player);
                        player.kick(FUtil.miniMessage(reason));
                    }
                    else
                    {
                        Bukkit.getServer().dispatchCommand(
                                Bukkit.getConsoleSender(),
                                "tempban " + player.getName() + " 999y Hate Speech"
                        );
                    }
                });

                return;
            }
        }
    }

    public static String stripNonLetters(String input) {
        return input.replaceAll("[^a-zA-Z]", "");
    }

    public static String normalize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // remove accents and diacritical marks
        return normalized.replaceAll("\\p{M}", "");
    }

    private void crashPlayer(Player victim)
    {
        if (victim == null)
        {
            return; // do nothing
        }

        for (int i = 0; i < 3; i++) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + victim.getName() + " at @s run particle flame ~ ~ ~ 1 1 1 1 999999999 force @s");
        }
        victim.spawnParticle(Particle.ASH, victim.getLocation(), Integer.MAX_VALUE, 1, 1, 1, 1, null, true);
    }
}
