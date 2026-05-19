package com.failprooftech.factionschat.commands.registrar;

import com.failprooftech.factionschat.commands.teamsapi.FactionsChatTeamsSubcommand;

import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsSubcommand;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * Registers FactionsChat as TeamsAPI consumer subcommands ({@code chat} and {@code c}) so team plugins that call
 * {@link TeamsAPI#dispatchSubcommand} (e.g. PvPIndex-Factions when {@code teamsApiEnabled}) expose {@code /f chat …}.
 *
 * @see <a href="https://ez-plugins.github.io/teams-api/consumer-subcommands.html">TeamsAPI - Registering Subcommands</a>
 */
public final class TeamsApiChatCommandRegistrar implements FactionsCommandRegistrar
{
    private final FactionsChatTeamsSubcommand chatSubcommand =
            new FactionsChatTeamsSubcommand("chat", "FactionsChat channels and utilities.");
    private final FactionsChatTeamsSubcommand shortSubcommand =
            new FactionsChatTeamsSubcommand("c", "FactionsChat (short alias).");

    @Override
    public void register(final JavaPlugin plugin)
    {
        for (final TeamsSubcommand sub : Arrays.asList(chatSubcommand, shortSubcommand))
        {
            final boolean already = TeamsAPI.getSubcommands().stream()
                    .anyMatch(s -> s.getName().equalsIgnoreCase(sub.getName()));
            if (!already)
            {
                TeamsAPI.registerSubcommand(plugin, sub);
            }
        }
        plugin.getLogger().info("Registered FactionsChat via TeamsAPI subcommands (/f chat, /f c).");
    }

    @Override
    public void unregister(final JavaPlugin plugin)
    {
        TeamsAPI.unregisterSubcommand(chatSubcommand);
        TeamsAPI.unregisterSubcommand(shortSubcommand);
    }
}
