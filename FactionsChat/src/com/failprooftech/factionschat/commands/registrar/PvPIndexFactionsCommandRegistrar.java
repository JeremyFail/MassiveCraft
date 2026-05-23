package com.failprooftech.factionschat.commands.registrar;

import com.failprooftech.factionschat.commands.FactionsChatDispatcher;
import com.pvpindex.factions.command.FactionCommandExecutor;
import com.pvpindex.factions.command.FactionTabCompleter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Registers the {@code /f c} subcommand by wrapping PvPIndex Factions'
 * {@link FactionCommandExecutor} and {@link FactionTabCompleter}.
 *
 * <p>Unlike the generic registrar, this class references PvPIndex's executor
 * types directly, giving us a compile-time guarantee that our interception is
 * compatible with PvPIndex's command setup. Any future structural change in
 * PvPIndex that breaks the hook will surface as a build error rather than a
 * silent runtime failure.</p>
 *
 * <p>Chat subcommand tokens: {@code c}, {@code chat}.</p>
 */
public class PvPIndexFactionsCommandRegistrar implements FactionsCommandRegistrar
{
    private PluginCommand           interceptedCmd;
    private FactionCommandExecutor  originalExecutor;
    private FactionTabCompleter     originalTabCompleter;

    /** Non-null only when we fell back to generic interception. */
    private GenericFactionsCommandRegistrar fallback;

    @Override
    public void register(JavaPlugin plugin)
    {
        PluginCommand fCmd = Bukkit.getPluginCommand("factions");
        if (fCmd == null) fCmd = Bukkit.getPluginCommand("f");

        if (fCmd == null)
        {
            plugin.getLogger().warning(
                "Could not find PvPIndex Factions' /f command to hook into. " +
                "The /f c subcommand will not be available.");
            return;
        }

        // Verify the executor is PvPIndex's - if not, fall back gracefully
        if (!(fCmd.getExecutor() instanceof FactionCommandExecutor pvpExecutor))
        {
            plugin.getLogger().warning(
                "/f executor is not a PvPIndex FactionCommandExecutor - " +
                "another plugin may have already claimed it. Falling back to generic interception.");
            fallback = new GenericFactionsCommandRegistrar();
            fallback.register(plugin);
            return;
        }

        interceptedCmd       = fCmd;
        originalExecutor     = pvpExecutor;
        originalTabCompleter = fCmd.getTabCompleter() instanceof FactionTabCompleter tc ? tc : null;

        fCmd.setExecutor((CommandSender sender, Command command, String label, String[] args) ->
        {
            if (args.length > 0 && isChatToken(args[0]))
            {
                String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                FactionsChatDispatcher.dispatch(sender, subArgs);
                return true;
            }
            return originalExecutor.onCommand(sender, command, label, args);
        });

        fCmd.setTabCompleter((CommandSender sender, Command command, String alias, String[] args) ->
        {
            if (args.length > 0 && isChatToken(args[0]))
            {
                String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                List<String> completions = FactionsChatDispatcher.tabComplete(sender, subArgs);
                return completions.isEmpty() ? null : completions;
            }
            if (originalTabCompleter != null)
                return originalTabCompleter.onTabComplete(sender, command, alias, args);
            return null;
        });

        plugin.getLogger().info("Registered via PvPIndex Factions command hook.");
    }

    @Override
    public void unregister(JavaPlugin plugin)
    {
        if (fallback != null)
        {
            fallback.unregister(plugin);
            fallback = null;
            return;
        }

        if (interceptedCmd == null) return;

        // Restore PvPIndex's original executor and tab completer
        interceptedCmd.setExecutor(originalExecutor);
        if (originalTabCompleter != null)
            interceptedCmd.setTabCompleter(originalTabCompleter);

        interceptedCmd       = null;
        originalExecutor     = null;
        originalTabCompleter = null;
    }

    private static boolean isChatToken(String token)
    {
        return token.equalsIgnoreCase("c") || token.equalsIgnoreCase("chat");
    }
}
