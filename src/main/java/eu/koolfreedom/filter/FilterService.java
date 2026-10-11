package eu.koolfreedom.filter;

import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.utilities.FLog;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Compiles configured regular expressions and checks normalized text against them.
 * The immutable rule list can be safely read by asynchronous chat event handlers.
 */
public final class FilterService
{
    private static final int REGEX_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static volatile List<FilterRule> rules = List.of();

    public FilterService() {}

    /** Recompiles the configured rules; invalid expressions are logged and skipped. */
    public static void reload()
    {
        List<FilterRule> loadedRules = new ArrayList<>();

        for (String expression : ConfigEntry.FILTER.getStringList())
        {
            if (expression == null || expression.isBlank()) continue;

            try
            {
                loadedRules.add(new FilterRule(expression,
                        Pattern.compile(normalize(expression), REGEX_FLAGS)));
            }
            catch (PatternSyntaxException exception)
            {
                FLog.warning("Ignoring invalid filter regex '{}': {}",
                        expression, exception.getDescription());
            }
        }

        rules = List.copyOf(loadedRules);
        FLog.info("Loaded {} text filter rules", rules.size());
    }

    /** Returns the first matching rule, or a no-match result for blank/unmatched text. */
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
}
