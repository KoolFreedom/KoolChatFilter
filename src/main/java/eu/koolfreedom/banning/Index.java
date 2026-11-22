package eu.koolfreedom.banning;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record Index(Map<String, BanEntry> names, Map<String, BanEntry> uuids, Map<String, BanEntry> ips)
{
    public Index(Map<String, BanEntry> names, Map<String, BanEntry> uuids, Map<String, BanEntry> ips)
    {
        this.names = Collections.unmodifiableMap(names);
        this.uuids = Collections.unmodifiableMap(uuids);
        this.ips = Collections.unmodifiableMap(ips);
    }

    public static Index empty()
    {
        return new Index(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }
}