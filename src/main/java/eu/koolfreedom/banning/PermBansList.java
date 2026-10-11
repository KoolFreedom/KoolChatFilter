package eu.koolfreedom.banning;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.utilities.FLog;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stores local player punishments in SQLite and provides an optional command bridge to
 * external punishment plugins. The in-memory indexes keep login and chat checks fast.
 */
public final class PermBansList
{
    private static final String DEFAULT_REASON = "You've met with a terrible fate, haven't you?";
    private static final long DEFAULT_DURATION_MILLIS = 7L * 24 * 60 * 60 * 1000;
    private static PermBansList instance;

    /** Punishments that can be selected for filtered content and the obliterate command. */
    public enum PunishmentType
    {
        PERMANENT_BAN,
        TEMPORARY_BAN,
        MUTE,
        KICK,
        WARNING,
        LOG_ONLY
    }

    @Getter
    public static final class BanEntry
    {
        private final String name;
        private final String uuid;
        private final Set<String> ips;
        private final String reason;
        private final PunishmentType punishment;
        private final long expiresAtMillis;

        private BanEntry(String name, String uuid, Set<String> ips, String reason,
                         PunishmentType punishment, long expiresAtMillis)
        {
            this.name = name;
            this.uuid = uuid;
            this.ips = Set.copyOf(ips);
            this.reason = reason;
            this.punishment = punishment;
            this.expiresAtMillis = expiresAtMillis;
        }

        private boolean isActive()
        {
            return expiresAtMillis == 0 || expiresAtMillis > System.currentTimeMillis();
        }

        private boolean isBan()
        {
            return punishment == PunishmentType.PERMANENT_BAN
                    || punishment == PunishmentType.TEMPORARY_BAN;
        }
    }

    private record Index(Map<String, BanEntry> names, Map<String, BanEntry> uuids, Map<String, BanEntry> ips) {
            private Index(Map<String, BanEntry> names, Map<String, BanEntry> uuids, Map<String, BanEntry> ips) {
                this.names = Collections.unmodifiableMap(names);
                this.uuids = Collections.unmodifiableMap(uuids);
                this.ips = Collections.unmodifiableMap(ips);
            }

            private static Index empty() {
                return new Index(new HashMap<>(), new HashMap<>(), new HashMap<>());
            }
        }

    private final File legacyFile;
    private final Connection connection;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private volatile Index index = Index.empty();

    private PermBansList()
    {
        KoolChatFilter plugin = KoolChatFilter.getInstance();
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs())
        {
            throw new IllegalStateException("Unable to create plugin data directory");
        }

        legacyFile = new File(plugin.getDataFolder(), "bans.yml");
        try
        {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + new File(plugin.getDataFolder(), "punishments.db").getAbsolutePath());
            initializeDatabase();
            migrateLegacyBans();
            refreshIndex();
        }
        catch (ClassNotFoundException | SQLException e)
        {
            throw new IllegalStateException("Failed to initialize SQLite punishment storage", e);
        }
    }

    /** Returns the shared punishment store, initializing the database on first use. */
    public static synchronized PermBansList get()
    {
        if (instance == null)
        {
            instance = new PermBansList();
        }
        return instance;
    }

    private static String normalizeName(String name)
    {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private void initializeDatabase() throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS punishments (
                        name TEXT PRIMARY KEY COLLATE NOCASE,
                        uuid TEXT,
                        reason TEXT NOT NULL,
                        type TEXT NOT NULL,
                        expires_at INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS punishment_ips (
                        name TEXT NOT NULL COLLATE NOCASE,
                        ip TEXT NOT NULL,
                        PRIMARY KEY (name, ip),
                        FOREIGN KEY (name) REFERENCES punishments(name) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS metadata (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_punishments_uuid ON punishments(uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_punishment_ips_ip ON punishment_ips(ip)");
        }
    }

    private void migrateLegacyBans() throws SQLException
    {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT value FROM metadata WHERE key = 'legacy_bans_migrated'");
             ResultSet result = query.executeQuery())
        {
            if (result.next()) return;
        }

        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(legacyFile);
        for (String name : legacy.getKeys(false))
        {
            ConfigurationSection section = legacy.getConfigurationSection(name);
            if (section == null) continue;

            String normalized = normalizeName(name);
            if (normalized.isEmpty()) continue;
            upsertPunishment(normalized, section.getString("uuid"),
                    section.getString("reason", DEFAULT_REASON), PunishmentType.PERMANENT_BAN, 0);
            for (String ip : section.getStringList("ips"))
            {
                addIp(normalized, ip);
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO metadata(key, value) VALUES('legacy_bans_migrated', 'true')"))
        {
            insert.executeUpdate();
        }
    }

    private void refreshIndex()
    {
        Map<String, BanEntry> names = new HashMap<>();
        Map<String, BanEntry> uuids = new HashMap<>();
        Map<String, BanEntry> ips = new HashMap<>();
        Set<String> expired = new HashSet<>();
        Map<String, Set<String>> ipsByName = new HashMap<>();

        try (Statement ipStatement = connection.createStatement();
             ResultSet ipRows = ipStatement.executeQuery("SELECT name, ip FROM punishment_ips"))
        {
            while (ipRows.next())
            {
                ipsByName.computeIfAbsent(normalizeName(ipRows.getString("name")), ignored -> new HashSet<>())
                        .add(ipRows.getString("ip"));
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to load punishment IP index", e);
        }

        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT name, uuid, reason, type, expires_at FROM punishments"))
        {
            while (rows.next())
            {
                String name = normalizeName(rows.getString("name"));
                String uuid = rows.getString("uuid");
                String reason = rows.getString("reason");
                PunishmentType type;
                try
                {
                    type = PunishmentType.valueOf(rows.getString("type"));
                }
                catch (IllegalArgumentException e)
                {
                    FLog.error("Ignoring punishment with unknown type for {}", name, e);
                    continue;
                }
                long expiresAt = rows.getLong("expires_at");
                BanEntry entry = new BanEntry(name, uuid,
                        ipsByName.getOrDefault(name, Collections.emptySet()), reason, type, expiresAt);
                if (!entry.isActive())
                {
                    expired.add(name);
                    continue;
                }

                names.put(name, entry);
                if (uuid != null && !uuid.isEmpty()) uuids.putIfAbsent(uuid.toLowerCase(Locale.ROOT), entry);
                if (entry.isBan())
                {
                    for (String ip : entry.getIps()) ips.putIfAbsent(ip, entry);
                }
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to load punishment index", e);
        }

        for (String name : expired)
        {
            try
            {
                deletePunishment(name);
            }
            catch (SQLException e)
            {
                FLog.error("Failed to remove expired punishment for {}", name, e);
            }
        }
        index = new Index(names, uuids, ips);
    }

    private void upsertPunishment(String name, String uuid, String reason,
                                  PunishmentType type, long expiresAtMillis) throws SQLException
    {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO punishments(name, uuid, reason, type, expires_at)
                VALUES(?, ?, ?, ?, ?)
                ON CONFLICT(name) DO UPDATE SET
                    uuid = excluded.uuid,
                    reason = excluded.reason,
                    type = excluded.type,
                    expires_at = excluded.expires_at
                """))
        {
            insert.setString(1, name);
            insert.setString(2, uuid);
            insert.setString(3, reason);
            insert.setString(4, type.name());
            insert.setLong(5, expiresAtMillis);
            insert.executeUpdate();
        }
    }

    private void addIp(String name, String ip) throws SQLException
    {
        if (ip == null || ip.isBlank()) return;
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT OR IGNORE INTO punishment_ips(name, ip) VALUES(?, ?)"))
        {
            insert.setString(1, name);
            insert.setString(2, ip.trim());
            insert.executeUpdate();
        }
    }

    private void deletePunishment(String name) throws SQLException
    {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM punishments WHERE name = ?"))
        {
            delete.setString(1, normalizeName(name));
            delete.executeUpdate();
        }
    }

    /** Rebuilds the in-memory indexes from SQLite and removes expired local punishments. */
    public synchronized void reload()
    {
        refreshIndex();
        FLog.info("Loaded {} local ban entries", getBansCount());
    }

    public synchronized void close()
    {
        try
        {
            connection.close();
            instance = null;
        }
        catch (SQLException e)
        {
            FLog.error("Failed to close punishment database", e);
        }
    }

    public int getBansCount()
    {
        return (int) index.names.values().stream()
                .filter(entry -> entry.isBan() && entry.isActive())
                .count();
    }

    /** Reads the filter punishment setting, falling back to a permanent ban when invalid. */
    public PunishmentType getConfiguredPunishment()
    {
        String configured = ConfigEntry.FILTER_PUNISHMENT.getString();
        try
        {
            return PunishmentType.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        }
        catch (RuntimeException e)
        {
            FLog.warning("Invalid filter punishment '{}'; using PERMANENT_BAN", configured);
            return PunishmentType.PERMANENT_BAN;
        }
    }

    /** Reads the configured duration for temporary bans and mutes. */
    public long getConfiguredDurationMillis()
    {
        String configured = ConfigEntry.FILTER_PUNISHMENT_DURATION.getString();
        try
        {
            return parseDurationMillis(configured);
        }
        catch (IllegalArgumentException e)
        {
            FLog.warning("Invalid filter punishment duration '{}'; using 7d", configured);
            return DEFAULT_DURATION_MILLIS;
        }
    }

    public static String formatDuration(long millis)
    {
        long seconds = Math.max(1, millis / 1000);
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }

    private static String formatCommandDuration(long millis)
    {
        if (millis % 86_400_000L == 0) return millis / 86_400_000L + "d";
        if (millis % 3_600_000L == 0) return millis / 3_600_000L + "h";
        if (millis % 60_000L == 0) return millis / 60_000L + "m";
        return millis / 1000L + "s";
    }

    private static long parseDurationMillis(String input)
    {
        if (input == null || !input.toLowerCase(Locale.ROOT).matches("\\d+[smhd]"))
        {
            throw new IllegalArgumentException("Expected a duration such as 30m or 7d");
        }
        String normalized = input.toLowerCase(Locale.ROOT);
        long value = Long.parseLong(normalized.substring(0, normalized.length() - 1));
        long multiplier = switch (normalized.charAt(normalized.length() - 1))
        {
            case 's' -> 1000L;
            case 'm' -> 60_000L;
            case 'h' -> 3_600_000L;
            case 'd' -> 86_400_000L;
            default -> throw new IllegalArgumentException("Unsupported duration unit");
        };
        if (value <= 0 || value > Long.MAX_VALUE / multiplier)
        {
            throw new IllegalArgumentException("Duration is out of range");
        }
        return value * multiplier;
    }

    public boolean isBanned(Player player)
    {
        if (player == null) return false;
        if (isNameBanned(player.getName()) || isUuidBanned(player.getUniqueId().toString())) return true;
        return player.getAddress() != null
                && player.getAddress().getAddress() != null
                && isIpBanned(player.getAddress().getAddress().getHostAddress());
    }

    public boolean isNameBanned(String name)
    {
        BanEntry entry = index.names.get(normalizeName(name));
        return entry != null && entry.isBan() && entry.isActive();
    }

    public boolean isUuidBanned(String uuid)
    {
        BanEntry entry = uuid == null ? null : index.uuids.get(uuid.toLowerCase(Locale.ROOT));
        return entry != null && entry.isBan() && entry.isActive();
    }

    public boolean isIpBanned(String ip)
    {
        BanEntry entry = ip == null ? null : index.ips.get(ip.trim());
        return entry != null && entry.isActive();
    }

    public boolean isMuted(String uuid)
    {
        BanEntry entry = uuid == null ? null : index.uuids.get(uuid.toLowerCase(Locale.ROOT));
        return entry != null && entry.getPunishment() == PunishmentType.MUTE && entry.isActive();
    }

    public Optional<BanEntry> getEntry(String name)
    {
        return Optional.ofNullable(index.names.get(normalizeName(name)));
    }

    public String getReason(String name)
    {
        return getEntry(name).map(BanEntry::getReason).orElse(DEFAULT_REASON);
    }

    public Component getReasonComponent(String name)
    {
        String reason = getReason(name);
        return reason.startsWith("<")
                ? miniMessage.deserialize(reason)
                : LegacyComponentSerializer.legacyAmpersand().deserialize(reason);
    }

    /**
     * Applies a punishment locally or delegates it to the configured external command.
     * KICK, WARNING, and LOG_ONLY require no stored record; callers perform their player-facing action.
     *
     * @return whether the punishment was stored or its external command was handled
     */
    public synchronized boolean applyPunishment(Player player, String reason, PunishmentType punishment,
                                                long durationMillis)
    {
        if (player == null || punishment == null) return false;
        String name = normalizeName(player.getName());
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress() == null || player.getAddress().getAddress() == null
                ? null : player.getAddress().getAddress().getHostAddress();
        String safeReason = reason == null ? DEFAULT_REASON : reason;
        if ((punishment == PunishmentType.TEMPORARY_BAN || punishment == PunishmentType.MUTE)
                && (durationMillis <= 0 || durationMillis > Long.MAX_VALUE - System.currentTimeMillis()))
        {
            FLog.error("Invalid duration for {} punishment on {}", punishment, name);
            return false;
        }

        String externalCommand = switch (punishment)
        {
            case PERMANENT_BAN -> ConfigEntry.FILTER_BAN_COMMAND.getString();
            case TEMPORARY_BAN -> ConfigEntry.FILTER_TEMPBAN_COMMAND.getString();
            case MUTE -> ConfigEntry.FILTER_MUTE_COMMAND.getString();
            default -> "";
        };
        if (externalCommand != null && !externalCommand.isBlank())
        {
            String duration = punishment == PunishmentType.PERMANENT_BAN
                    ? "permanent" : formatCommandDuration(durationMillis);
            return dispatchExternalCommand(externalCommand, player.getName(), uuid, ip, safeReason, duration);
        }

        if (punishment != PunishmentType.PERMANENT_BAN
                && punishment != PunishmentType.TEMPORARY_BAN
                && punishment != PunishmentType.MUTE) return true;

        long expiresAt = punishment == PunishmentType.PERMANENT_BAN
                ? 0 : System.currentTimeMillis() + durationMillis;
        try
        {
            upsertPunishment(name, uuid, safeReason, punishment, expiresAt);
            try (PreparedStatement clearIps = connection.prepareStatement(
                    "DELETE FROM punishment_ips WHERE name = ?"))
            {
                clearIps.setString(1, name);
                clearIps.executeUpdate();
            }
            if (punishment != PunishmentType.MUTE) addIp(name, ip);
            refreshIndex();
            return true;
        }
        catch (SQLException e)
        {
            FLog.error("Failed to save {} punishment for {}", punishment, name, e);
            return false;
        }
    }

    private boolean dispatchExternalCommand(String template, String name, String uuid, String ip,
                                            String reason, String duration)
    {
        String command = template
                .replace("{player}", sanitizeCommandValue(name))
                .replace("{uuid}", sanitizeCommandValue(uuid))
                .replace("{ip}", sanitizeCommandValue(ip == null ? "" : ip))
                .replace("{reason}", sanitizeCommandValue(reason))
                .replace("{duration}", sanitizeCommandValue(duration));
        if (command.isBlank())
        {
            FLog.error("Configured external punishment command is empty after placeholder expansion");
            return false;
        }
        boolean dispatched;
        try
        {
            dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        catch (RuntimeException e)
        {
            FLog.error("Failed to execute external punishment command", e);
            return false;
        }
        if (!dispatched)
        {
            FLog.error("External punishment command was not handled: {}", command);
        }
        return dispatched;
    }

    private static String sanitizeCommandValue(String value)
    {
        return value.replace('\r', ' ').replace('\n', ' ').replace('\0', ' ');
    }

    public void banPlayer(Player player, String reason)
    {
        banPlayer(player, reason, PunishmentType.PERMANENT_BAN);
    }

    public void banPlayer(Player player, String reason, PunishmentType punishment)
    {
        if (!applyPunishment(player, reason, punishment, getConfiguredDurationMillis()))
        {
            FLog.error("Failed to apply {} punishment to {}", punishment, player == null ? "unknown player" : player.getName());
        }
    }

    public boolean unbanPlayer(String name)
    {
        String normalized = normalizeName(name);
        if (normalized.isEmpty()) return false;
        boolean removed = removeLocal(normalized);
        String externalCommand = ConfigEntry.FILTER_UNBAN_COMMAND.getString();
        if (externalCommand != null && !externalCommand.isBlank())
        {
            removed |= dispatchExternalCommand(externalCommand, name, "", "", "", "");
        }
        return removed;
    }

    public boolean unbanPlayerByUuid(String uuid)
    {
        Optional<String> name = findBannedNameByUuid(uuid);
        boolean removed = name.map(this::removeLocal).orElse(false);
        String externalCommand = ConfigEntry.FILTER_UNBAN_COMMAND.getString();
        if (externalCommand != null && !externalCommand.isBlank())
        {
            removed |= dispatchExternalCommand(externalCommand, name.orElse(""), uuid, "", "", "");
        }
        return removed;
    }

    public boolean unbanPlayerByIp(String ip)
    {
        Optional<String> name = findBannedNameByIp(ip);
        boolean removed = name.map(this::removeLocal).orElse(false);
        String externalCommand = ConfigEntry.FILTER_UNBAN_COMMAND.getString();
        if (externalCommand != null && !externalCommand.isBlank())
        {
            removed |= dispatchExternalCommand(externalCommand, name.orElse(""), "", ip, "", "");
        }
        return removed;
    }

    private synchronized boolean removeLocal(String name)
    {
        try
        {
            int changed;
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM punishments WHERE name = ?"))
            {
                delete.setString(1, normalizeName(name));
                changed = delete.executeUpdate();
            }
            refreshIndex();
            return changed > 0;
        }
        catch (SQLException e)
        {
            FLog.error("Failed to remove punishment for {}", name, e);
            return false;
        }
    }

    public Optional<String> findBannedNameByIp(String ip)
    {
        BanEntry entry = ip == null ? null : index.ips.get(ip.trim());
        return entry == null || !entry.isActive() ? Optional.empty() : Optional.of(entry.getName());
    }

    public Optional<String> findBannedNameByUuid(String uuid)
    {
        BanEntry entry = uuid == null ? null : index.uuids.get(uuid.toLowerCase(Locale.ROOT));
        return entry == null || !entry.isBan() || !entry.isActive()
                ? Optional.empty() : Optional.of(entry.getName());
    }

    public void closeDatabase()
    {
        close();
    }
}
