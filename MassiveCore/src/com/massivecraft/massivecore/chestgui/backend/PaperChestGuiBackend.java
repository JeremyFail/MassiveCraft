package com.massivecraft.massivecore.chestgui.backend;

import com.massivecraft.massivecore.chestgui.ChestGui;
import com.massivecraft.massivecore.chestgui.ChestGuiLayout;
import com.massivecraft.massivecore.util.Txt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;

/**
 * Paper inventory backend. Loaded only via {@link com.massivecraft.massivecore.chestgui.ChestGui}.
 * Uses Adventure titles and {@link MenuType} player-bound views when the inventory is created at open time.
 */
public final class PaperChestGuiBackend implements ChestGuiBackend, ChestGuiBackend.CapabilityProbe
{
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
	
	@Override
	public boolean isAvailable()
	{
		try
		{
			Class.forName("net.kyori.adventure.text.Component");
			Class.forName("org.bukkit.inventory.MenuType");
			Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class, Component.class);
			return true;
		}
		catch (ReflectiveOperationException | NoClassDefFoundError ex)
		{
			return false;
		}
	}
	
	@Override
	public void ensureInventory(ChestGui gui)
	{
		if (gui.getInventory() != null) return;
		ChestGuiLayout layout = gui.getLayout();
		if (layout == null) layout = ChestGuiLayout.CHEST_3_ROW;
		
		Component title = title(gui.getTitle());
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
		if (gui.getInventory() == null && this.tryOpenMenuType(player, gui)) return;
		this.ensureInventory(gui);
		player.openInventory(gui.getInventory());
	}
	
	private boolean tryOpenMenuType(Player player, ChestGui gui)
	{
		MenuType menuType = menuType(gui.getLayout());
		if (menuType == null) return false;
		
		try
		{
			InventoryView view = menuType.create(player, title(gui.getTitle()));
			gui.attachInventory(view.getTopInventory());
			player.openInventory(view);
			return true;
		}
		catch (Throwable ignored)
		{
			return false;
		}
	}
	
	private static MenuType menuType(ChestGuiLayout layout)
	{
		if (layout == null) layout = ChestGuiLayout.CHEST_3_ROW;
		if (layout.isGenericChest())
		{
			switch (layout.getSize())
			{
				case 9: return MenuType.GENERIC_9X1;
				case 18: return MenuType.GENERIC_9X2;
				case 27: return MenuType.GENERIC_9X3;
				case 36: return MenuType.GENERIC_9X4;
				case 45: return MenuType.GENERIC_9X5;
				case 54: return MenuType.GENERIC_9X6;
				default: return null;
			}
		}
		if (layout == ChestGuiLayout.HOPPER) return MenuType.HOPPER;
		if (layout == ChestGuiLayout.DROPPER) return MenuType.GENERIC_3X3;
		return null;
	}
	
	private static Component title(String raw)
	{
		return LEGACY.deserialize(Txt.parse(raw == null ? "" : raw));
	}
}
