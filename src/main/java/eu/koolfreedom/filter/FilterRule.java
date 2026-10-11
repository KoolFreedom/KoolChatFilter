package eu.koolfreedom.filter;

import java.util.regex.Pattern;

/** A configured expression compiled once and reused to test submitted text. */
public record FilterRule(String expression, Pattern pattern)
{
    /** Returns true when the compiled expression occurs anywhere in the input. */
    public boolean matches(String input)
    {
        return pattern.matcher(input).find();
    }
}