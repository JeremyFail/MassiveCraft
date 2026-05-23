package com.failprooftech.factionschat.metrics;

import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.factions.FactionsBridge;
import com.failprooftech.factionschat.factions.MassiveFactionsBridge;
import com.failprooftech.factionschat.factions.PluginFactionsBridge;
import com.failprooftech.factionschat.factions.PvPIndexFactionsBridge;
import com.failprooftech.factionschat.factions.TeamsApiFactionsBridge;
import com.failprooftech.factionschat.integrations.teamsapi.TeamsApiProviderProbe;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.HashMap;
import java.util.Map;

/**
 * bStats integration for FactionsChat (plugin id {@value #PLUGIN_ID}).
 */
public final class FactionsChatBStats
{
    static final int PLUGIN_ID = 31430;

    private static final String CHART_INTEGRATION_TYPE = "integrationType";

    private static final String CATEGORY_MASSIVECRAFT = "MassiveCraft Factions";
    private static final String CATEGORY_PVPINDEX = "PvPIndex-Factions";
    private static final String CATEGORY_FACTIONS_BRIDGE = "FactionsBridge";
    private static final String CATEGORY_TEAMS_API = "Teams API";
    private static final String CATEGORY_STANDALONE = "None/Standalone";

    private final FactionsChat plugin;
    private Metrics metrics;

    /**
     * Constructs a new FactionsChatBStats instance.
     * @param plugin The FactionsChat plugin.
     */
    public FactionsChatBStats(final FactionsChat plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Starts bStats and registers custom charts. Failures are logged and do not affect the plugin.
     */
    public void enable()
    {
        try
        {
            this.metrics = new Metrics(this.plugin, PLUGIN_ID);
            this.metrics.addCustomChart(new DrilldownPie(CHART_INTEGRATION_TYPE, this::integrationTypeChart));
        }
        catch (final RuntimeException ex)
        {
            this.plugin.getLogger().warning("bStats initialization failed: " + ex.getMessage());
        }
    }

    /**
     * Creates a drilldown pie chart for the integration type.
     * @return A map of the integration type, with the category and subcategory.
     * @throws NullPointerException If the factions bridge is null.
     */
    private Map<String, Map<String, Integer>> integrationTypeChart()
    {
        final Map<String, Map<String, Integer>> data = new HashMap<>();
        final FactionsBridge bridge = this.plugin.getFactionsBridge();

        final String category;
        final String subcategory;
        if (bridge instanceof TeamsApiFactionsBridge)
        {
            category = CATEGORY_TEAMS_API;
            subcategory = TeamsApiProviderProbe.formatIntegrationSubcategory();
        }
        else if (bridge instanceof PluginFactionsBridge pluginBridge)
        {
            category = CATEGORY_FACTIONS_BRIDGE;
            subcategory = pluginBridge.getProviderName();
        }
        else if (bridge instanceof MassiveFactionsBridge)
        {
            category = CATEGORY_MASSIVECRAFT;
            subcategory = pluginVersion(this.plugin.getServer().getPluginManager(), "Factions");
        }
        else if (bridge instanceof PvPIndexFactionsBridge)
        {
            category = CATEGORY_PVPINDEX;
            subcategory = pluginVersion(this.plugin.getServer().getPluginManager(), "PvPIndexFactions");
        }
        else
        {
            category = CATEGORY_STANDALONE;
            subcategory = "N/A";
        }

        final Map<String, Integer> counts = new HashMap<>();
        counts.put(subcategory, 1);
        data.put(category, counts);
        return data;
    }

    /**
     * Gets the version of a plugin.
     * @param pm The plugin manager.
     * @param pluginName The name of the plugin.
     * @return The version of the plugin, or "unknown" if the plugin is not found or has no version.
     */
    private static String pluginVersion(final PluginManager pm, final String pluginName)
    {
        final Plugin target = pm.getPlugin(pluginName);
        if (target == null || target.getDescription() == null)
        {
            return "unknown";
        }
        return nullToUnknown(target.getDescription().getVersion());
    }

    /**
     * Converts a null or empty version string to "unknown".
     * @param version The version string.
     * @return The version string, or "unknown" if the version is null or empty.
     */
    private static String nullToUnknown(final String version)
    {
        return version == null || version.isEmpty() ? "unknown" : version;
    }
}
