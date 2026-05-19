package com.failprooftech.factionschat.integrations.teamsapi;

import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsService;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

/**
 * Resolves which Bukkit plugin registered the active {@link TeamsService} with Teams API.
 */
public final class TeamsApiProviderProbe
{
    private TeamsApiProviderProbe()
    {
    }

    /**
     * @return the plugin that registered the highest-priority {@link TeamsService}, if any
     */
    public static Optional<Plugin> resolveProviderPlugin()
    {
        if (!TeamsAPI.isAvailable())
        {
            return Optional.empty();
        }

        final RegisteredServiceProvider<TeamsService> registration =
                Bukkit.getServer().getServicesManager().getRegistration(TeamsService.class);
        if (registration == null)
        {
            return Optional.empty();
        }

        return Optional.ofNullable(registration.getPlugin());
    }

    /**
     * Builds a bStats drilldown subcategory for Teams API chat data: library API version plus provider plugin id and version.
     *
     * @return e.g. {@code 1.7.0 · Factions 3.4.0-alpha-14}, or {@code 1.7.0 · unknown provider}
     */
    public static String formatIntegrationSubcategory()
    {
        final String apiVersion = TeamsAPI.API_VERSION;
        return resolveProviderPlugin()
                .map(plugin -> apiVersion + " · " + plugin.getName() + " " + pluginVersion(plugin))
                .orElse(apiVersion + " · unknown provider");
    }

    private static String pluginVersion(final Plugin plugin)
    {
        if (plugin.getDescription() == null)
        {
            return "unknown";
        }
        final String version = plugin.getDescription().getVersion();
        return version == null || version.isEmpty() ? "unknown" : version;
    }
}
