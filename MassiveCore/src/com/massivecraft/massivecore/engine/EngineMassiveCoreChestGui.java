package com.massivecraft.massivecore.engine;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.chestgui.ChestAction;
import com.massivecraft.massivecore.chestgui.ChestGui;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.InventoryUtil.InventoryAlter;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

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

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onClick(InventoryClickEvent event)
	{
		ChestGui gui = ChestGui.get(event);
		if (gui == null) return;
		
		this.deny(event);
		
		if (InventoryUtil.isBottomInventory(event))
		{
			if (this.isBottomClickAllowed(gui, event))
			{
				event.setCancelled(false);
				event.setResult(Result.DEFAULT);
			}
			else
			{
				this.warnBottomDenied(gui, event.getWhoClicked());
			}
			return;
		}

		ChestAction action = gui.getAction(event);
		if (action == null) return;
		
		boolean consumed = action.onClick(event);
		if (!consumed) return;
		
		gui.setLastAction(action);
		if (gui.isAutoclosing()) this.closeLater(event.getWhoClicked(), event.getView().getTopInventory());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onClickProtect(InventoryClickEvent event)
	{
		this.protectClick(event);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void onClickMonitor(InventoryClickEvent event)
	{
		this.protectClick(event);
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onDrag(InventoryDragEvent event)
	{
		ChestGui gui = ChestGui.get(event);
		if (gui == null) return;
		
		if (this.isBottomDragAllowed(gui, event)) return;
		
		this.deny(event);
		if (!this.touchesTop(event))
		{
			this.warnBottomDenied(gui, event.getWhoClicked());
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onDragProtect(InventoryDragEvent event)
	{
		this.protectDrag(event);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void onDragMonitor(InventoryDragEvent event)
	{
		this.protectDrag(event);
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onMove(InventoryMoveItemEvent event)
	{
		this.protectMove(event);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onMoveProtect(InventoryMoveItemEvent event)
	{
		this.protectMove(event);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onOpen(InventoryOpenEvent event)
	{
		final ChestGui gui = ChestGui.get(event);
		if (gui == null) return;
		
		Bukkit.getScheduler().runTask(getPlugin(), () -> gui.getRunnablesOpen().forEach(Runnable::run));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onClose(InventoryCloseEvent event)
	{
		final ChestGui gui = ChestGui.get(event);
		if (gui == null) return;
		
		Bukkit.getScheduler().runTask(getPlugin(), () -> gui.getRunnablesClose().forEach(Runnable::run));
		
		if (gui.isAutoremoving())
		{
			// We save the inventory in the map for a little while.
			// A plugin may want to do something upon the chest gui closing.
			Bukkit.getScheduler().runTaskLater(this.getPlugin(), gui::remove, 20);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event)
	{
		InventoryView view = event.getPlayer().getOpenInventory();
		if (view == null) return;
		ChestGui gui = ChestGui.get(view.getTopInventory());
		if (gui == null) return;
		if (gui.isAutoremoving()) gui.remove();
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	private void protectClick(InventoryClickEvent event)
	{
		ChestGui gui = ChestGui.get(event);
		if (gui == null) return;
		if (this.isBottomClickAllowed(gui, event)) return;
		this.deny(event);
	}
	
	private void protectDrag(InventoryDragEvent event)
	{
		ChestGui gui = ChestGui.get(event);
		if (gui == null) return;
		if (this.isBottomDragAllowed(gui, event)) return;
		this.deny(event);
	}
	
	private boolean isBottomClickAllowed(ChestGui gui, InventoryClickEvent event)
	{
		if (!gui.isBottomInventoryAllowed()) return false;
		if (!InventoryUtil.isBottomInventory(event)) return false;
		try
		{
			InventoryAlter alter = InventoryUtil.getAlter(event);
			return !alter.isGiving() && !alter.isTaking();
		}
		catch (Throwable ignored)
		{
			return false;
		}
	}
	
	private boolean isBottomDragAllowed(ChestGui gui, InventoryDragEvent event)
	{
		if (!gui.isBottomInventoryAllowed()) return false;
		if (this.touchesTop(event)) return false;
		return true;
	}
	
	private boolean touchesTop(InventoryDragEvent event)
	{
		Inventory top = event.getInventory();
		for (Integer rawSlot : event.getRawSlots())
		{
			if (rawSlot == null) continue;
			if (InventoryUtil.isTopInventory(rawSlot, top)) return true;
		}
		return false;
	}
	
	private void warnBottomDenied(ChestGui gui, HumanEntity human)
	{
		if (gui.isBottomInventoryAllowed())
		{
			MixinMessage.get().msgOne(human, "<b>You can't move items into this menu.");
		}
		else
		{
			MixinMessage.get().msgOne(human, "<b>Exit the GUI to edit your items.");
		}
	}
	
	private void protectMove(InventoryMoveItemEvent event)
	{
		if (ChestGui.get(event.getSource()) == null && ChestGui.get(event.getDestination()) == null) return;
		event.setCancelled(true);
	}
	
	private void deny(InventoryClickEvent event)
	{
		event.setCancelled(true);
		event.setResult(Result.DENY);
	}
	
	private void deny(InventoryDragEvent event)
	{
		event.setCancelled(true);
		event.setResult(Result.DENY);
	}
	
	private void closeLater(HumanEntity human, Inventory top)
	{
		Bukkit.getScheduler().runTask(getPlugin(), () -> {
			InventoryView open = human.getOpenInventory();
			if (open == null) return;
			if (open.getTopInventory() != top) return;
			open.close();
		});
	}

}
