package eu.koolfreedom.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
public enum ConfigEntry
{
    FILTER(List.class, "filtered-stuff");
    //
    private final Class<?> type;
    private final String configName;

    ConfigEntry(Class<?> type, String configName)
    {
        this.type = type;
        this.configName = configName;
    }

    public String getString()
    {
        return MainConfig.getString(this);
    }

    public String setString(String value)
    {
        MainConfig.setString(this, value);
        return value;
    }

    public Double getDouble()
    {
        return MainConfig.getDouble(this);
    }

    public Double setDouble(Double value)
    {
        MainConfig.setDouble(this, value);
        return value;
    }

    public Boolean getBoolean()
    {
        return MainConfig.getBoolean(this);
    }

    public Boolean setBoolean(Boolean value)
    {
        MainConfig.setBoolean(this, value);
        return value;
    }

    public Integer getInteger()
    {
        return MainConfig.getInteger(this);
    }

    public Integer setInteger(Integer value)
    {
        MainConfig.setInteger(this, value);
        return value;
    }

    public Long getLong()
    {
        return MainConfig.getLong(this);
    }

    public Long setInteger(Long value)
    {
        MainConfig.setLong(this, value);
        return value;
    }

    public List<?> getList()
    {
        return MainConfig.getList(this);
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList()
    {
        return (List<String>) MainConfig.getList(this);
    }

    public ConfigurationSection getConfigurationSection()
    {
        return MainConfig.getConfigurationSection(this);
    }
}
