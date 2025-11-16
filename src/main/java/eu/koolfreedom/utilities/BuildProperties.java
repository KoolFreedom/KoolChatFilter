package eu.koolfreedom.utilities;

import eu.koolfreedom.KoolChatFilter;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class BuildProperties extends Properties
{
    private final String author;
    private final String version;
    private final String number;
    private final String date;

    public BuildProperties()
    {
        super();

        try (InputStream in = KoolChatFilter.class.getClassLoader().getResourceAsStream("build.properties"))
        {
            load(in);
        }
        catch (IOException ignored)
        {
        }

        author = getProperty("buildAuthor", "KoolFreedom");
        version = getProperty("buildVersion", "1.0");
        number = getProperty("buildNumber", "1");
        date = getProperty("buildDate", "unknown");
    }
}
