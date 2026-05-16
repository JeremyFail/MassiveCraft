package com.failprooftech.factionschat.integrations.placeholderapi;

import com.massivecraft.factions.integration.placeholderapi.PlaceholderExpander;
import com.massivecraft.factions.integration.placeholderapi.PlaceholderFactions;
import com.failprooftech.factionschat.ChatMode;
import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.util.PlaceholderProcessor;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI integration for FactionsChat when MassiveCraft Factions is installed.
 *
 * <p>Implements {@link PlaceholderExpander} to inject chat-related placeholders into
 * the existing {@code factions} PAPI expansion, so all placeholders share the
 * {@code %factions_*} namespace without re-registering the expansion.</p>
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>{@code %factions_chat_prefix%} – prefix string for the player's current chat mode</li>
 *   <li>{@code %factions_chat_color%}  – color string for the player's current chat mode</li>
 * </ul>
 */
public final class MassivePlaceholderBridge implements PlaceholderBridge, PlaceholderExpander
{
    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //

    private static final MassivePlaceholderBridge INSTANCE = new MassivePlaceholderBridge();
    public static MassivePlaceholderBridge get() { return INSTANCE; }

    private MassivePlaceholderBridge() {}

    // -------------------------------------------- //
    // PlaceholderBridge
    // -------------------------------------------- //

    @Override
    public void activate()
    {
        PlaceholderFactions.addExpander(this);
    }

    @Override
    public void deactivate()
    {
        PlaceholderFactions.removeExpander(this);
    }

    // -------------------------------------------- //
    // PlaceholderExpander
    // -------------------------------------------- //

    @Override
    @SuppressWarnings("deprecation")
    public String getExpanderVersion()
    {
        return FactionsChat.instance.getName() + " " + FactionsChat.instance.getDescription().getVersion();
    }

    @Override
    public List<String> getPlaceholders()
    {
        List<String> list = new ArrayList<>();
        list.add("%factions_chat_prefix%");
        list.add("%factions_chat_color%");
        return list;
    }

    @Override
    public String onPlaceholderRequest(Player player, String placeholder)
    {
        return parsePlaceholder(player, placeholder);
    }

    @Override
    public String onPlaceholderRequest(OfflinePlayer player, String placeholder)
    {
        if (player == null || !player.isOnline()) return "";
        return parsePlaceholder(player.getPlayer(), placeholder);
    }

    // -------------------------------------------- //
    // PRIVATE HELPERS
    // -------------------------------------------- //

    private static String parsePlaceholder(Player player, String placeholder)
    {
        if (placeholder == null) return null;
        boolean isNull = player == null;
        return PlaceholderProcessor.parsePlaceholderWithModifiers(placeholder, key ->
        {
            switch (key)
            {
                case "chat_prefix":
                    return !isNull ? Settings.ChatPrefixes.getPrefix(ChatMode.getChatModeForPlayer(player)) : "";
                case "chat_color":
                    return !isNull ? Settings.TextColors.getColor(ChatMode.getChatModeForPlayer(player)) : "";
                default:
                    return null; // Unknown - let Factions handle remaining placeholders
            }
        });
    }
}
