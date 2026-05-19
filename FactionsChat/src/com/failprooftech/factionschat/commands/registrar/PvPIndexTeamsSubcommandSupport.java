package com.failprooftech.factionschat.commands.registrar;

import com.skyblockexp.teamsapi.api.TeamsAPI;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * Detects whether PvPIndex-Factions will delegate unknown {@code /f} top-level tokens to {@link TeamsAPI} consumer
 * subcommands ({@code teamsApiEnabled} on {@link com.pvpindex.factions.command.FactionCommandExecutor}).
 */
public final class PvPIndexTeamsSubcommandSupport
{
    private PvPIndexTeamsSubcommandSupport()
    {
    }

    /**
     * @param logger optional; may log at {@link java.util.logging.Level#FINE} why the probe failed
     * @return {@code true} when TeamsAPI runtime is up and PvPIndex's faction command executor has team-API subcommands enabled
     */
    public static boolean isTeamsSubcommandDispatchAvailable(final Logger logger)
    {
        final Plugin teamsApiPlugin = Bukkit.getPluginManager().getPlugin("TeamsAPI");
        if (teamsApiPlugin == null || !teamsApiPlugin.isEnabled())
        {
            fine(logger, "TeamsAPI plugin not loaded or not enabled; using direct /f hook for FactionsChat.");
            return false;
        }
        if (!TeamsAPI.isAvailable())
        {
            fine(logger, "TeamsAPI.isAvailable() is false; using direct /f hook for FactionsChat.");
            return false;
        }

        final PluginCommand fCmd = resolveFactionRootCommand();
        if (fCmd == null)
        {
            fine(logger, "No /f or /factions PluginCommand; cannot probe PvPIndex executor.");
            return false;
        }

        final CommandExecutor executor = fCmd.getExecutor();
        if (executor == null)
        {
            fine(logger, "Faction root command has no executor; using direct /f hook for FactionsChat.");
            return false;
        }

        return readTeamsApiEnabledFlag(executor, logger);
    }

    private static PluginCommand resolveFactionRootCommand()
    {
        PluginCommand cmd = Bukkit.getPluginCommand("factions");
        if (cmd == null)
        {
            cmd = Bukkit.getPluginCommand("f");
        }
        return cmd;
    }

    private static boolean readTeamsApiEnabledFlag(final CommandExecutor executor, final Logger logger)
    {
        try
        {
            final Field field = executor.getClass().getDeclaredField("teamsApiEnabled");
            field.setAccessible(true);
            return field.getBoolean(executor);
        }
        catch (final ReflectiveOperationException ex)
        {
            fine(logger, "Could not read teamsApiEnabled from /f executor (" + executor.getClass().getName()
                    + "); using direct /f hook for FactionsChat.");
            return false;
        }
    }

    private static void fine(final Logger logger, final String msg)
    {
        if (logger != null)
        {
            logger.fine(msg);
        }
    }
}
