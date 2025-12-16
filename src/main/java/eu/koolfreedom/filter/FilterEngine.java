package eu.koolfreedom.filter;

import eu.koolfreedom.config.ConfigEntry;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class FilterEngine
{
    private static final Map<String, Pattern> RULES = new HashMap<>();

    private FilterEngine()
    {
    }

    public static void reload()
    {
        RULES.clear();

        for (String rule : ConfigEntry.FILTER.getStringList())
        {
            String clean = sanitize(rule);
            String regex = "\\b" + Pattern.quote(clean) + "\\b";
            RULES.put(rule, Pattern.compile(regex));
        }
    }

    public static FilterResult check(String input)
    {
        String message = sanitize(input);

        for (Map.Entry<String, Pattern> entry : RULES.entrySet())
        {
            if (entry.getValue().matcher(message).find())
            {
                return new FilterResult(true, entry.getKey());
            }
        }
        return FilterResult.noMatch();
    }

    private static String sanitize(String input)
    {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        normalized = normalized.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");

        normalized = normalized.toLowerCase();
        normalized = normalized.replaceAll("[^a-z ]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }
}
