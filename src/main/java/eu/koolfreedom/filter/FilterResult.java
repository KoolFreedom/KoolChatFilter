package eu.koolfreedom.filter;

public record FilterResult(boolean matched, String expression)
{
    public static FilterResult noMatch()
    {
        return new FilterResult(false, null);
    }
}