package eu.koolfreedom.banning;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.utilities.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PermBansList
{
    private static PermBansList instance;
    private final File file;
    private final YamlConfiguration config;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private PermBansList()
    {
        KoolChatFilter plugin = KoolChatFilter.getInstance();
        this.file = new File(plugin.getDataFolder(), "idiots.yml");
        if (!file.exists())
        {
            plugin.saveResource("idiots.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public static PermBansList get()
    {
        if (instance == null)
        {
            instance = new PermBansList();
        }
        return instance;
    }

    public void save()
    {
        try
        {
            config.save(file);
        } catch (IOException e) {
            FLog.error("Failed to save permbans file!", e);
        }
    }

    public void reload()
    {
        try
        {
            config.load(file);
        } catch (Exception e)
        {
            FLog.error("Failed to reload permbans file!", e);
        }
    }

    public int getBansCount()
    {
        return config.getKeys(false).size();
    }

    // ======================================================
    // Ban Management
    // ======================================================

    public boolean isBanned(Player player) {
        String ip = null;
        if (player.getAddress() != null) {
            ip = player.getAddress().getAddress().getHostAddress();
        }

        return isNameBanned(player.getName())
                || isUuidBanned(player.getUniqueId().toString())
                || (ip != null && isIpBanned(ip));
    }

    public boolean isNameBanned(String name)
    {
        return config.contains(name.toLowerCase());
    }

    public boolean isUuidBanned(String uuid) {
        for (String key : config.getKeys(false)) {
            String stored = config.getString(key + ".uuid");
            if (stored != null && stored.equalsIgnoreCase(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isIpBanned(String ip) {
        for (String key : config.getKeys(false)) {
            List<String> ips = config.getStringList(key + ".ips");
            if (ips.contains(ip)) {
                return true;
            }
        }
        return false;
    }

    // ======================================================
    // Get Ban Info
    // ======================================================

    public String getReason(String name) {
        return config.getString(name.toLowerCase() + ".reason", "You've met with a terrible fate, haven't you?");
    }

    public Component getReasonComponent(String name) {
        String reason = getReason(name);
        if (reason.startsWith("<")) {
            // MiniMessage format
            return mm.deserialize(reason);
        } else {
            // Legacy color format (&c, &4, etc.)
            return LegacyComponentSerializer.legacyAmpersand().deserialize(reason);
        }
    }

    // ======================================================
    // Ban / Unban
    // ======================================================

    public void banPlayer(Player player, String reason) {
        String name = player.getName().toLowerCase();
        String uuid = player.getUniqueId().toString();
        String ip = Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();

        config.set(name + ".uuid", uuid);

        List<String> ips = config.getStringList(name + ".ips");
        if (!ips.contains(ip)) {
            ips.add(ip);
        }
        config.set(name + ".ips", ips);

        config.set(name + ".reason", reason != null ? reason : "No reason provided.");
        save();
    }

    public void unbanPlayer(String name) {
        name = name.toLowerCase();
        if (config.contains(name)) {
            config.set(name, null);
            save();
        }
    }

    public Optional<String> findBannedNameByIp(String ip) {
        for (String key : config.getKeys(false)) {
            if (config.getStringList(key + ".ips").contains(ip)) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }

    public Optional<String> findBannedNameByUuid(String uuid) {
        for (String key : config.getKeys(false)) {
            if (uuid.equalsIgnoreCase(config.getString(key + ".uuid"))) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }
}
