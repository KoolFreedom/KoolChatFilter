package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.banning.IdiotsList;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

@CommandParameters(
        name = "permbans",
        description = "Removes or reloads the idiots file",
        usage = "/<command> [remove|reload] (player)",
        aliases = {"idiotslist", "idiotlist", "idotlist"}
)
public class PermBanCommand extends KoolCommand
{
    private final File file = new File(KoolChatFilter.getInstance().getDataFolder(), "idiots.yml");
    private final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String s, String[] args)
    {
        IdiotsList idots = IdiotsList.get();
        if (args.length == 0)
        {
            return false;
        }

        String sub = args[0].toLowerCase();

        switch (sub)
        {
            case "reload" -> {
                idots.reload();
                msg(sender, "<green>Reloaded idiots file");
            }

            case "remove" -> {
                if (args.length < 2)
                {
                    return false;
                }

                String target = args[1].toLowerCase();
                if (config.contains(target))
                {
                    idots.unbanPlayer(target);
                    msg(sender, "<gray>Removed player from idiots file");
                    idots.reload();
                }
                else
                {
                    msg(sender, "<red>Could not find that idiot");
                }
            }
        }

        return true;
    }
}
