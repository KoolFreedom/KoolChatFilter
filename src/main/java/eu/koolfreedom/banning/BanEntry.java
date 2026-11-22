package eu.koolfreedom.banning;

import java.util.Set;

public record BanEntry(String name, String uuid, Set<String> ips, String reason, PunishmentType punishment,
                long expiresAtMillis)
{
    public BanEntry(String name, String uuid, Set<String> ips, String reason, PunishmentType punishment, long expiresAtMillis)
    {
        this.name = name;
        this.uuid = uuid;
        this.ips = Set.copyOf(ips);
        this.reason = reason;
        this.punishment = punishment;
        this.expiresAtMillis = expiresAtMillis;
    }

    public boolean isActive()
    {
        return expiresAtMillis == 0 || expiresAtMillis > System.currentTimeMillis();
    }

    public boolean isBan()
    {
        return punishment == PunishmentType.PERMANENT_BAN
                || punishment == PunishmentType.TEMPORARY_BAN;
    }

    public PunishmentType getPunishment()
    {
        return punishment;
    }

    public long getExpiresAtMillis()
    {
        return expiresAtMillis;
    }
}
