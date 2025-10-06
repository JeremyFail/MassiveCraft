package com.massivecraft.factions;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Manages warnings for plugins using the legacy Factions API compatibility layer.
 * Ensures each plugin only gets warned once per server restart cycle.
 */
public class LegacyApiWarningManager
{
    // -------------------------------------------- //
    // FIELDS
    // -------------------------------------------- //
    
    private static final Set<String> warnedPlugins = new HashSet<>();
    private static final Logger logger = Factions.get().getLogger();
    
    // Package names to exclude when looking for the calling plugin
    private static final String[] EXCLUDED_PACKAGES = {
        "com.massivecraft.factions",
        "com.massivecraft.massivecore",
        "java.",
        "javax.",
        "sun.",
        "com.sun.",
        "org.bukkit.",
        "net.minecraft.",
        "org.spigotmc.",
        "io.papermc."
    };
    
    // -------------------------------------------- //
    // METHODS
    // -------------------------------------------- //

    public static void checkAndWarnLegacyUsage()
    {
        checkAndWarnLegacyUsage(false);
    }
    
    /**
     * Checks the current call stack and warns about legacy API usage if this is the first time
     * the calling plugin has used the legacy API.
     */
    public static void checkAndWarnLegacyUsage(boolean isFactionsUUID)
    {
        try
        {
            // Get the current stack trace
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            
            // Find the first external plugin in the call chain
            Plugin callingPlugin = findCallingPlugin(stackTrace);
            
            if (callingPlugin == null)
            {
                // Could not determine calling plugin, skip warning
                return;
            }
            
            String pluginName = callingPlugin.getName();
            
            // Check if we've already warned this plugin
            if (warnedPlugins.contains(pluginName))
            {
                return;
            }
            
            // Add to warned plugins set
            warnedPlugins.add(pluginName);
            
            // Log the warning
            logLegacyApiWarning(callingPlugin, isFactionsUUID);
        }
        catch (Exception e)
        {
            // If anything goes wrong with the detection, fail silently
            // We don't want to break functionality just for a warning
        }
    }
    
    /**
     * Analyzes the stack trace to find the first plugin that is not part of Factions or core Bukkit/Spigot.
     */
    private static Plugin findCallingPlugin(StackTraceElement[] stackTrace)
    {
        for (StackTraceElement element : stackTrace)
        {
            String className = element.getClassName();
            
            // Skip our own packages and core packages
            if (isExcludedPackage(className))
            {
                continue;
            }
            
            try
            {
                // Try to load the class and find its providing plugin
                Class<?> clazz = Class.forName(className);
                Plugin plugin = JavaPlugin.getProvidingPlugin(clazz);
                
                if (plugin != null)
                {
                    return plugin;
                }
            }
            catch (ClassNotFoundException | IllegalArgumentException e)
            {
                // Class not found or not from a plugin, continue searching
                continue;
            }
        }
        
        return null;
    }
    
    /**
     * Checks if a class name belongs to an excluded package.
     */
    private static boolean isExcludedPackage(String className)
    {
        for (String excludedPackage : EXCLUDED_PACKAGES)
        {
            if (className.startsWith(excludedPackage))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Logs a warning message about legacy API usage.
     */
    private static void logLegacyApiWarning(Plugin plugin, boolean isFactionsUUID)
    {
        logger.warning("######################################################################");
        if (isFactionsUUID)
        {
            logger.warning("WARNING: FactionsUUID Compatibility Layer in use by the plugin:");
        } 
        else 
        {
            logger.warning("WARNING: Legacy Factions Compatibility Layer in use by the plugin:");
        }

        // Build plugin info string
        String pluginInfo = "'" + plugin.getName() + "'";
        String version = plugin.getDescription().getVersion();
        String authors = String.join(", ", plugin.getDescription().getAuthors());
        if (version != null && !version.isEmpty())
        {
            pluginInfo += " v" + version;
        }
        if (!authors.isEmpty())
        {
            pluginInfo += " by " + authors;
        }
        logger.warning(pluginInfo);

        logger.warning("This integration is UNSUPPORTED by Factions and may or may not work.");
        logger.warning("Please contact the plugin author(s) for assistance with any issues.");
        logger.warning("######################################################################");
    }
    
    /**
     * Clears the warned plugins set. This method is primarily for testing purposes
     * or if you want to reset warnings without restarting the server.
     */
    public static void clearWarnedPlugins()
    {
        warnedPlugins.clear();
    }
    
    /**
     * Returns a copy of the set of plugins that have been warned.
     * This is primarily for debugging or administrative purposes.
     */
    public static Set<String> getWarnedPlugins()
    {
        return new HashSet<>(warnedPlugins);
    }
}
