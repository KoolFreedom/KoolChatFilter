package eu.koolfreedom.utilities;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Random;

public class FUtil
{

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().tags(TagResolver.builder()
            .resolver(StandardTags.defaults())
            .resolver(TagResolver.resolver("randomize", RandomColorTag.randomColorTag))
            .build()).build();
    private static final Random RANDOM = new Random();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();


    public static void broadcast(Component component)
    {
        Bukkit.broadcast(component);
    }

    /**
     * Broadcasts a MiniMessage-formatted message to the server.
     * @param message       The message being broadcast as a String in MiniMessage format.
     * @param placeholders  An array of {@link TagResolver} instances. If you don't want to have any additional
     *                      placeholders, just leave out that argument.
     */
    public static void broadcast(String message, TagResolver... placeholders)
    {
        Bukkit.broadcast(miniMessage(message, placeholders));
    }

    /**
     * Broadcasts a text component to users on the server with a specific permission node.
     * @param component     The {@link Component} being broadcasted.
     * @param permission    The permission node required for a player to see the message.
     */
    public static void broadcast(Component component, String permission)
    {
        Bukkit.broadcast(component, permission);
    }

    /**
     * Broadcasts a MiniMessage-formatted message to users on the server with a specific permission node.
     * @param permission    The permission node required for a player to see the message.
     * @param message       The message being broadcast as a String in MiniMessage format.
     * @param placeholders  An array of {@link TagResolver} instances. If you don't want to have any additional
     *                      placeholders, just leave out that argument.
     */
    public static void broadcast(String permission, String message, TagResolver... placeholders)
    {
        Bukkit.broadcast(miniMessage(message, placeholders), permission);
    }

    /**
     * Processes MiniMessage messages into a regular Adventure text component.
     * @param message       The message as a String in MiniMessage format.
     * @param placeholders  An array of {@link TagResolver} instances. If you don't want to have any additional
     *                      placeholders, just leave out that argument.
     * @return              The resulting {@link Component}.
     */
    public static Component miniMessage(String message, TagResolver... placeholders)
    {
        return MINI_MESSAGE.deserialize(message, placeholders);
    }

    public static class RandomColorTag implements Modifying
    {
        @Getter
        private static final RandomColorTag randomColorTag = new RandomColorTag();

        @Override
        public Component apply(@NotNull Component current, int depth)
        {
            if (current instanceof TextComponent textComponent)
            {
                return Component.join(JoinConfiguration.spaces(), Arrays.stream(textComponent.content().split(" "))
                        .map(text -> Component.text(text).colorIfAbsent(TextColor.color(randomNumber(0, 255),
                                randomNumber(0, 255), randomNumber(0, 255)))).toList());
            }

            return current;
        }
    }

    /**
     * Get a random number between two numbers.
     * @param min   The lowest the number can be.
     * @param max   The highest the number can be.
     * @return      A randomly generated number.
     */
    public static int randomNumber(int min, int max)
    {
        if (min > max)
        {
            throw new IllegalArgumentException("Numbers are flipped. Max < min.");
        }

        return RANDOM.nextInt(min, max);
    }

    /**
     * Get the content of a component as a plain text String.
     * @param input     The {@link Component} to get as a component.
     * @return          The resulting String.
     */
    public static String plainText(Component input)
    {
        return PLAIN_TEXT.serialize(input);
    }
}
