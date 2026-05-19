package com.failprooftech.factionschat.commands.registrar;

import com.failprooftech.factionschat.commands.FactionsChatDispatcher;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers {@code /chat} (and aliases such as {@code /c}) when no Massive/PvPIndex integration applies and no {@code /f}
 * command exists to hook - typically standalone servers without a Factions plugin.
 */
public final class StandaloneChatCommandRegistrar implements FactionsCommandRegistrar
{
	@Override
	public void register(final JavaPlugin plugin)
	{
		final PluginCommand cmd = plugin.getCommand("chat");
		if (cmd == null)
		{
			plugin.getLogger().warning("plugin.yml is missing the \"chat\" command - standalone chat commands will not work.");
			return;
		}
		cmd.setExecutor((sender, command, label, args) ->
		{
			FactionsChatDispatcher.dispatch(sender, args);
			return true;
		});
		cmd.setTabCompleter((sender, command, alias, args) -> FactionsChatDispatcher.tabComplete(sender, args));
		plugin.getLogger().info("Registered standalone /" + cmd.getLabel() + " for FactionsChat (aliases: " + String.join(", ", cmd.getAliases()) + ").");
	}

	@Override
	public void unregister(final JavaPlugin plugin)
	{
		final PluginCommand cmd = plugin.getCommand("chat");
		if (cmd != null)
		{
			cmd.setExecutor(null);
			cmd.setTabCompleter(null);
		}
	}
}
