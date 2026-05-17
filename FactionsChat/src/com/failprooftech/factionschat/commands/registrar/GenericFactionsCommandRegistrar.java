package com.failprooftech.factionschat.commands.registrar;

import com.failprooftech.factionschat.commands.FactionsChatDispatcher;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Registers the {@code /f c} subcommand by intercepting the existing {@code /f}
 * (or {@code /factions}) {@link PluginCommand} that the installed Factions plugin
 * already registered with Bukkit.
 *
 * <p>When {@code args[0]} is {@code "c"} or {@code "chat"} the executor and tab
 * completer delegate to {@link FactionsChatCommandLogic}; all other arguments are
 * passed unchanged to the original Factions executor so that no existing Factions
 * functionality is broken.</p>
 *
 * <p>This registrar works with any Factions plugin that registers a standard Bukkit
 * {@link PluginCommand} named {@code "factions"} or {@code "f"} - e.g. FactionsUUID,
 * Kingdoms, or any other fork.</p>
 */
public class GenericFactionsCommandRegistrar implements FactionsCommandRegistrar
{
    private PluginCommand   interceptedCmd;
    private CommandExecutor originalExecutor;
    private TabCompleter    originalTabCompleter;

    @Override
    public void register(JavaPlugin plugin)
    {
        // Try the canonical name first, then the short alias
        PluginCommand fCmd = Bukkit.getPluginCommand("factions");
        if (fCmd == null) fCmd = Bukkit.getPluginCommand("f");

        if (fCmd == null)
        {
            plugin.getLogger().warning(
                "Could not find a /factions or /f PluginCommand to hook into. " +
                "The /f c subcommand will not be available. " +
                "If your Factions plugin uses a non-standard command name, please report this.");
            return;
        }

        interceptedCmd       = fCmd;
        originalExecutor     = fCmd.getExecutor();
        originalTabCompleter = fCmd.getTabCompleter();

        // Wrap the executor: intercept /f c ... and pass everything else through
        fCmd.setExecutor((CommandSender sender, Command command, String label, String[] args) ->
        {
            if (args.length > 0 && isChatToken(args[0]))
            {
                String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                FactionsChatDispatcher.dispatch(sender, subArgs);
                return true;
            }
            // Not a chat subcommand - delegate to the original Factions executor
            if (originalExecutor != null)
                return originalExecutor.onCommand(sender, command, label, args);
            return false;
        });

        // Wrap the tab completer the same way
        fCmd.setTabCompleter((CommandSender sender, Command command, String alias, String[] args) ->
        {
            if (args.length > 0 && isChatToken(args[0]))
            {
                // args[1..] are the sub-args (may be empty while typing "c ")
                String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                List<String> completions = FactionsChatDispatcher.tabComplete(sender, subArgs);
                return completions.isEmpty() ? null : completions;
            }
            if (originalTabCompleter != null)
                return originalTabCompleter.onTabComplete(sender, command, alias, args);
            return null;
        });

        plugin.getLogger().info("Hooked into /" + fCmd.getName() + " for /f c subcommand.");
    }

    @Override
    public void unregister(JavaPlugin plugin)
    {
        // Restore the original executor and tab completer so the Factions plugin
        // continues to work normally if FactionsChat is disabled at runtime
        if (interceptedCmd != null)
        {
            if (originalExecutor     != null) interceptedCmd.setExecutor(originalExecutor);
            if (originalTabCompleter != null) interceptedCmd.setTabCompleter(originalTabCompleter);
            interceptedCmd = null;
        }
    }

    // --------------------------------------------------------------------- //

    /** Returns true if the given token is one of the "chat" subcommand tokens. */
    private static boolean isChatToken(String token)
    {
        return token.equalsIgnoreCase("c") || token.equalsIgnoreCase("chat");
    }
}
