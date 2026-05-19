package com.failprooftech.factionschat.commands.registrar;

import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.util.ChatTxt;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Wires {@code /chat} and {@code /c} (from {@code plugin.yml}) to show integrated-mode usage pointing at
 * {@link FactionsChat#getChatCommandPrefix()} instead of handling chat logic directly.
 */
public final class IntegratedModeChatCommandRegistrar
{
	/**
	 * Private constructor to prevent instantiation.
	 */
	private IntegratedModeChatCommandRegistrar()
	{

	}

	/**
	 * Registers the integrated chat command.
	 * @param plugin The plugin instance.
	 */
	public static void register(final JavaPlugin plugin)
	{
		final PluginCommand cmd = plugin.getCommand("chat");
		if (cmd == null)
		{
			plugin.getLogger().warning("plugin.yml is missing the \"chat\" command - /chat redirect in integrated mode will not work.");
			return;
		}
		cmd.setExecutor((sender, command, label, args) ->
		{
			sendIntegratedUsage(sender, args);
			return true;
		});
		cmd.setTabCompleter(null);
	}

	/**
	 * Unregisters the integrated chat command.
	 * @param plugin The plugin instance.
	 */
	public static void unregister(final JavaPlugin plugin)
	{
		final PluginCommand cmd = plugin.getCommand("chat");
		if (cmd != null)
		{
			cmd.setExecutor(null);
			cmd.setTabCompleter(null);
		}
	}

	/**
	 * Sends the integrated usage message to the sender.
	 * @param sender The command sender.
	 * @param args The arguments passed to the command.
	 */
	private static void sendIntegratedUsage(final CommandSender sender, final String[] args)
	{
		final String prefix = FactionsChat.instance.getChatCommandPrefix();
		if (args.length == 0)
		{
			sender.sendMessage(ChatTxt.parse("<n>FactionsChat commands are available at <k>" + prefix + "<n>."));
			sender.sendMessage(ChatTxt.parse("<n>Usage: <k>" + prefix + " [subcommand|mode] [args]"));
			sender.sendMessage(ChatTxt.parse("<n>Use <k>" + prefix + " help<n> to see available commands and modes."));
			return;
		}
		final String suggested = prefix + " " + String.join(" ", args);
		sender.sendMessage(ChatTxt.parse("<n>FactionsChat is integrated with your factions command, not <k>/chat<n>."));
		sender.sendMessage(ChatTxt.parse("<n>Use <k>" + suggested + "<n> instead."));
	}
}
