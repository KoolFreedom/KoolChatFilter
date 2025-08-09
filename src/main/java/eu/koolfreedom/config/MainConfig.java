package eu.koolfreedom.config;

import eu.koolfreedom.KoolFreedomCore;
import eu.koolfreedom.util.FLog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.List;

public class MainConfig
{
    public static final File CONFIG_FILE = new File(KoolFreedomCore.getInstance().getDataFolder(), "config.yml");
    private static final EnumMap<ConfigEntry, Object> ENTRY_MAP;

    static
    {
        ENTRY_MAP = new EnumMap<>(ConfigEntry.class);

        Defaults tempDefaults;
        try
        {
            try (InputStream defaultConfig = getDefaultConfig())
            {
                tempDefaults = new Defaults(defaultConfig);
                for (ConfigEntry entry : ConfigEntry.values())
                {
                    ENTRY_MAP.put(entry, tempDefaults.get(entry.getConfigName()));
                }
            }
            catch (IOException ex)
            {
                FLog.error("Failed to load default configuration", ex);
            }

            copyDefaultConfig();

            load();
        }
        catch (Exception ex)
        {
            FLog.error("Failed to load configuration", ex);
        }
    }

    public static void load()
    {
        try
        {
            YamlConfiguration config = new YamlConfiguration();

            if (!CONFIG_FILE.exists())
            {
                copyDefaultConfig();
            }

            config.load(CONFIG_FILE);

            for (ConfigEntry entry : ConfigEntry.values())
            {
                String path = entry.getConfigName();
                if (config.contains(path))
                {
                    Object value = config.get(path);
                    if (value == null || entry.getType().isAssignableFrom(value.getClass()))
                    {
                        ENTRY_MAP.put(entry, value);
                    }
                    else
                    {
                        FLog.warning("Value for " + entry.getConfigName() + " is of type " + value.getClass().getSimpleName() + ". Needs to be " + entry.getType().getSimpleName() + ". Using default value.");
                    }
                }
                else
                {
                    FLog.warning("Missing configuration entry " + entry.getConfigName() + ". Using default value.");
                }
            }
        }
        catch (IOException | InvalidConfigurationException ex)
        {
            FLog.error("Failed to load configuration", ex);
        }
    }

    public static String getString(ConfigEntry entry)
    {
        try
        {
            return get(entry, String.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to read configuration entry as a string (incorrect type?)", ex);
        }

        return null;
    }

    public static void setString(ConfigEntry entry, String value)
    {
        try
        {
            set(entry, value, String.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to set configuration entry as a string (incorrect type?)", ex);
        }
    }

    public static Double getDouble(ConfigEntry entry)
    {
        try
        {
            return get(entry, Double.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to read configuration entry as a double (incorrect type?)", ex);
        }

        return null;
    }

    public static void setDouble(ConfigEntry entry, Double value)
    {
        try
        {
            set(entry, value, Double.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to set configuration entry as a double (incorrect type?)", ex);
        }
    }

    public static Boolean getBoolean(ConfigEntry entry)
    {
        try
        {
            return get(entry, Boolean.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to read configuration entry as a boolean (incorrect type?)", ex);
        }

        return null;
    }

    public static void setBoolean(ConfigEntry entry, Boolean value)
    {
        try
        {
            set(entry, value, Boolean.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to set configuration entry as a boolean (incorrect type?)", ex);
        }
    }

    public static Integer getInteger(ConfigEntry entry)
    {
        try
        {
            return get(entry, Integer.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to read configuration entry as an integer (incorrect type?)", ex);
        }

        return null;
    }

    public static void setInteger(ConfigEntry entry, Integer value)
    {
        try
        {
            set(entry, value, Integer.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to set configuration entry as an integer (incorrect type?)", ex);
        }
    }

    public static Long getLong(ConfigEntry entry)
    {
        try
        {
            return get(entry, Long.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to read configuration entry as a long (incorrect type?)", ex);
        }

        return null;
    }

    public static void setLong(ConfigEntry entry, Long value)
    {
        try
        {
            set(entry, value, Long.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to set configuration entry as an integer (incorrect type?)", ex);
        }
    }

    public static List<?> getList(ConfigEntry entry)
    {
        try
        {
            return get(entry, List.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to read configuration entry as a list (incorrect type?)", ex);
        }

        return null;
    }

    public static ConfigurationSection getConfigurationSection(ConfigEntry entry)
    {
        try
        {
            return get(entry, ConfigurationSection.class);
        }
        catch (IllegalArgumentException ex)
        {
            FLog.error("Failed to read configuration entry as a configuration section (incorrect type?)", ex);
        }

        return null;
    }

    public static <T> T get(ConfigEntry entry, Class<T> type) throws IllegalArgumentException
    {
        Object value = ENTRY_MAP.get(entry);
        try
        {
            return type.cast(value);
        }
        catch (ClassCastException ex)
        {
            throw new IllegalArgumentException(entry.name() + " is not of type " + type.getSimpleName());
        }
    }

    public static <T> void set(ConfigEntry entry, T value, Class<T> type) throws IllegalArgumentException
    {
        if (!type.isAssignableFrom(entry.getType()))
        {
            throw new IllegalArgumentException(entry.name() + " is not of type " + type.getSimpleName());
        }
        if (value != null && !type.isAssignableFrom(value.getClass()))
        {
            throw new IllegalArgumentException("Value is not of type " + type.getSimpleName());
        }
        ENTRY_MAP.put(entry, value);
    }

    private static void copyDefaultConfig()
    {
        if (MainConfig.CONFIG_FILE.exists())
        {
            return;
        }

        FLog.info("Installing default configuration file template: {}", MainConfig.CONFIG_FILE.getPath());

        try
        {
            InputStream defaultConfig = getDefaultConfig();
            Files.copy(defaultConfig, MainConfig.CONFIG_FILE.toPath());
            defaultConfig.close();
        }
        catch (IOException ex)
        {
            FLog.error("Failed to install default configuration file", ex);
        }
    }

    private static InputStream getDefaultConfig()
    {
        return KoolSMPCore.getInstance().getResource("config.yml");
    }

    public static class Defaults
    {
        private final YamlConfiguration defaults;

        private Defaults(InputStream defaultConfig)
        {
            defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfig));
        }

        public Object get(String path)
        {
            return defaults.get(path);
        }
    }
}