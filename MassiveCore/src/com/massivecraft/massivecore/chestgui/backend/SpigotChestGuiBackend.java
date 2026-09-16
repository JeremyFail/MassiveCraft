package com.massivecraft.massivecore.chestgui.backend;

import com.massivecraft.massivecore.chestgui.ChestGui;
import com.massivecraft.massivecore.chestgui.ChestGuiLayout;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Spigot inventory backend. Loaded as the ChestGui fallback when Paper Adventure/MenuType APIs are absent.
 */
public final class SpigotChestGuiBackend implements ChestGuiBackend
{
	@Override
	public void ensureInventory(ChestGui gui)
	{
		if (gui.getInventory() != null) return;
		ChestGuiLayout layout = gui.getLayout();
		if (layout == null) layout = ChestGuiLayout.CHEST_3_ROW;
		
		String title = Txt.parse(gui.getTitle() == null ? "" : gui.getTitle());
		Inventory inventory;
		if (layout.isGenericChest())
		{
			inventory = Bukkit.createInventory(gui, layout.getSize(), title);
		}
		else
		{
			inventory = Bukkit.createInventory(gui, layout.getSpecialType(), title);
		}
		gui.attachInventory(inventory);
	}
	
	@Override
	public void open(Player player, ChestGui gui)
	{
		this.ensureInventory(gui);
		player.openInventory(gui.getInventory());
	}
}
