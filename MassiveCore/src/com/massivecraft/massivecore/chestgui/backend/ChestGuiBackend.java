package com.massivecraft.massivecore.chestgui.backend;

import com.massivecraft.massivecore.chestgui.ChestGui;
import org.bukkit.entity.Player;

/**
 * Creates and opens virtual chest inventories. Paper uses Adventure titles and {@code MenuType}; Spigot uses Bukkit string titles.
 */
public interface ChestGuiBackend
{
	void ensureInventory(ChestGui gui);
	
	void open(Player player, ChestGui gui);
	
	interface CapabilityProbe
	{
		boolean isAvailable();
	}
}
