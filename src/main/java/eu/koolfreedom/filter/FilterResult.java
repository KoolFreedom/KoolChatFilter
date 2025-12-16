package eu.koolfreedom.filter;

import org.jetbrains.annotations.Nullable;

public record FilterResult(boolean matched, @Nullable String matchedRule)
{
    public static FilterResult noMatch()
    {
        return new FilterResult(false, null);
    }
}
