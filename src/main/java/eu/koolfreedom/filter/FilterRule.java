package eu.koolfreedom.filter;

import java.util.regex.Pattern;

public record FilterRule(String expression, Pattern pattern)
{
    public boolean matches(String input)
    {
        return pattern.matcher(input).find();
    }
}