package com.massivecraft.factions.integration.placeholderapi;

import com.massivecraft.massivecore.Integration;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class IntegrationPlaceholderAPI extends Integration
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static IntegrationPlaceholderAPI i = new IntegrationPlaceholderAPI();
	public static IntegrationPlaceholderAPI get() { return i; }
	private IntegrationPlaceholderAPI()
	{
		this.setPluginName("PlaceholderAPI");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void setIntegrationActiveInner(boolean active)
	{
		if (active) PlaceholderFactions.get().register();
	}

	public static void ensureRegistered()
	{
		if (PlaceholderFactions.get().isRegistered()) return;
		PlaceholderFactions.get().register();
	}

	// If the PlaceholderAPI command is run to reload the config
	// then we should reregister.
	@EventHandler(priority = EventPriority.MONITOR)
	public void lookForCommand(PlayerCommandPreprocessEvent event)
	{
		String str = event.getMessage();
		if (str.startsWith("/")) str = str.substring(1);

		if (!str.startsWith("papi")) return;
		Bukkit.getScheduler().runTaskLater(this.getPlugin(), IntegrationPlaceholderAPI::ensureRegistered, 10L);
	}

	
}
