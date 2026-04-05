package com.massivecraft.massivecore.engine;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.chestgui.ChestAction;
import com.massivecraft.massivecore.chestgui.ChestGui;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class EngineMassiveCoreChestGui extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static EngineMassiveCoreChestGui i = new EngineMassiveCoreChestGui();
	public static EngineMassiveCoreChestGui get() { return i; }
	
	// -------------------------------------------- //
	// LISTENER
	// -------------------------------------------- //

	@EventHandler(priority = EventPriority.LOW)
	public void onClick(InventoryClickEvent event)
	{
		// If this inventory is a gui ...
		ChestGui gui = ChestGui.get(event);
		if (gui == null) return;
		
		// ... then cancel the event ...
		event.setCancelled(true);
		event.setResult(Result.DENY);
		
		// ... warn on bottom inventory ...
		if (InventoryUtil.isBottomInventory(event))
		{
			// ... only if its not allowed.
			if (gui.isBottomInventoryAllowed())
			{
				event.setCancelled(false);
				event.setResult(Result.DEFAULT);
			}
			else
			{
				MixinMessage.get().msgOne(event.getWhoClicked(), "<b>Exit the GUI to edit your items.");
			}
			
			return;
		}

		// ... and if this slot index has an action ...
		ChestAction action = gui.getAction(event);
		if (action == null) return;
		
		// ... set last action ...
		gui.setLastAction(action);
		
		// ... close the GUI ...
		if (gui.isAutoclosing()) event.getView().close();
		
		// ... and use that action.
		action.onClick(event);		
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onOpen(InventoryOpenEvent event)
	{
		// Get
		final ChestGui gui = ChestGui.get(event);
		if (gui == null) return;
		
		// Later
		Bukkit.getScheduler().runTask(getPlugin(), () -> {
			// Runnables
			gui.getRunnablesOpen().forEach(Runnable::run);
		});
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onClose(InventoryCloseEvent event)
	{
		// Get
		final ChestGui gui = ChestGui.get(event);
		if (gui == null) return;
		
		// Later
		Bukkit.getScheduler().runTask(getPlugin(), () -> {
			// Runnables
			gui.getRunnablesClose().forEach(Runnable::run);
		});
		
		if (gui.isAutoremoving())
		{
			// We save the inventory in the map for a little while.
			// A plugin may want to do something upon the chest gui closing.
			Bukkit.getScheduler().runTaskLater(this.getPlugin(), gui::remove, 20);
		}
	}

}
