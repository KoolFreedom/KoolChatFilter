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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PermBansList
{
    private static PermBansList instance;
    private final File file;
    private final YamlConfiguration config;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Configuration path constants for consistency and maintainability
    private static final String UUID_KEY = "uuid";
    private static final String IPS_KEY = "ips";
    private static final String REASON_KEY = "reason";
    private static final String DEFAULT_REASON = "You've met with a terrible fate, haven't you?";

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
            FLog.info("Loaded {} entries", getBansCount());
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
    // Helper Methods
    // ======================================================

    /**
     * Builds a configuration path for a ban entry key.
     *
     * @param name the player name (will be lowercased)
     * @param subkey the subkey (uuid, ips, reason)
     * @return the full configuration path
     */
    private String buildPath(String name, String subkey) {
        return name.toLowerCase() + "." + subkey;
    }

    /**
     * Gets the UUID stored for a banned player.
     */
    private Optional<String> getStoredUuid(String name) {
        String uuid = config.getString(buildPath(name, UUID_KEY));
        return Optional.ofNullable(uuid);
    }

    /**
     * Gets the IPs associated with a banned entry (safe copy).
     */
    @SuppressWarnings("ConstantConditions")
    private List<String> getStoredIps(String name) {
        List<String> ips = config.getStringList(buildPath(name, IPS_KEY));
        return ips != null ? ips : Collections.emptyList();
    }

    // ======================================================
    // Ban Management
    // ======================================================

    public boolean isBanned(Player player) {
        if (player == null) {
            return false;
        }

        String name = player.getName();
        String uuid = player.getUniqueId().toString();
        String ip = getPlayerIp(player);

        return isNameBanned(name)
                || isUuidBanned(uuid)
                || (ip != null && isIpBanned(ip));
    }

    /**
     * Safely extracts the player's IP address.
     */
    private String getPlayerIp(Player player) {
        try {
            return player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
        } catch (Exception e) {
            FLog.warning("Failed to get IP for player {}", player.getName());
            return null;
        }
    }

    public boolean isNameBanned(String name) {
        return name != null && config.contains(name.toLowerCase());
    }

    public boolean isUuidBanned(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }

        final String uuidLower = uuid.toLowerCase();
        return config.getKeys(false).stream()
                .anyMatch(key -> {
                    String stored = getStoredUuid(key).orElse(null);
                    return stored != null && stored.equalsIgnoreCase(uuidLower);
                });
    }

    public boolean isIpBanned(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        return config.getKeys(false).stream()
                .anyMatch(key -> getStoredIps(key).contains(ip));
    }

    // ======================================================
    // Get Ban Info
    // ======================================================

    public String getReason(String name) {
        return config.getString(buildPath(name, REASON_KEY), DEFAULT_REASON);
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

    /**
     * Bans a player by name, UUID, and IP address.
     *
     * @param player the player to ban
     * @param reason the ban reason (null will use default)
     * @throws IllegalArgumentException if player is null or has no valid IP
     */
    public void banPlayer(Player player, String reason) {
        Objects.requireNonNull(player, "Player cannot be null");

        String name = player.getName().toLowerCase();
        String uuid = player.getUniqueId().toString();
        String ip = getPlayerIp(player);

        if (ip == null) {
            throw new IllegalArgumentException("Cannot ban player without a valid IP address");
        }

        config.set(buildPath(name, UUID_KEY), uuid);

        // Update IPs list (avoid adding duplicates)
        List<String> ips = new java.util.ArrayList<>(getStoredIps(name));
        if (!ips.contains(ip)) {
            ips.add(ip);
            config.set(buildPath(name, IPS_KEY), ips);
        }

        config.set(buildPath(name, REASON_KEY), reason != null ? reason : DEFAULT_REASON);
        save();
    }

    /**
     * Unbans a player by name.
     *
     * @param name the player name to unban
     * @return true if the player was banned and successfully unbanned, false if not found
     */
    public boolean unbanPlayer(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        name = name.toLowerCase();
        if (config.contains(name)) {
            config.set(name, null);
            save();
            return true;
        }
        return false;
    }

    /**
     * Unbans a player by UUID.
     *
     * @param uuid the player UUID to unban
     * @return true if the player was found and unbanned, false otherwise
     */
    public boolean unbanPlayerByUuid(String uuid) {
        Optional<String> bannedName = findBannedNameByUuid(uuid);
        return bannedName.filter(this::unbanPlayer).isPresent();
    }

    /**
     * Unbans a player by IP address.
     *
     * @param ip the player IP to unban
     * @return true if the player was found and unbanned, false otherwise
     */
    public boolean unbanPlayerByIp(String ip) {
        Optional<String> bannedName = findBannedNameByIp(ip);
        return bannedName.filter(this::unbanPlayer).isPresent();
    }

    /**
     * Finds a banned player by IP address.
     *
     * @param ip the IP address to search for
     * @return an Optional containing the banned name if found
     */
    public Optional<String> findBannedNameByIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return Optional.empty();
        }

        return config.getKeys(false).stream()
                .filter(key -> getStoredIps(key).contains(ip))
                .findFirst();
    }

    /**
     * Finds a banned player by UUID.
     *
     * @param uuid the UUID to search for
     * @return an Optional containing the banned name if found
     */
    public Optional<String> findBannedNameByUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return Optional.empty();
        }

        final String uuidLower = uuid.toLowerCase();
        return config.getKeys(false).stream()
                .filter(key -> {
                    String stored = getStoredUuid(key).orElse(null);
                    return stored != null && stored.equalsIgnoreCase(uuidLower);
                })
                .findFirst();
    }
}
