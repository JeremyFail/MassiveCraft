package com.massivecraft.massivehat.engine;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivehat.MassiveHat;
import com.massivecraft.massivehat.Perm;
import com.massivecraft.massivehat.entity.MConf;
import com.massivecraft.massivehat.predicate.PredicateIsHat;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class EngineHatSwitch extends Engine
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	public static final int RAW_HAT_SLOT_ID = 5;
	
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static final EngineHatSwitch i = new EngineHatSwitch();
	
	public static EngineHatSwitch get()
	{
		return i;
	}
	
	// -------------------------------------------- //
	// LISTENER
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void hatSwitch(final InventoryClickEvent event)
	{
		final HumanEntity clicker = event.getWhoClicked();
		
		// If a player ...
		if (MUtil.isntPlayer(clicker)) return;
		final Player me = (Player) clicker;
		
		final InventoryView view = event.getView();
		
		// ... is clicking around in their own/armor/crafting view ...
		if (view.getType() != InventoryType.CRAFTING) return;
		
		// ... and they are clicking their hat slot ...
		if (event.getRawSlot() != RAW_HAT_SLOT_ID) return;
		
		final ItemStack cursor = event.getCursor();
		
		// ... and the cursor is a hat ...
		if (!PredicateIsHat.getHat().apply(cursor)) return;
		
		// ... and hatting is allowed ...
		if (!Perm.USE.has(me, MConf.get().hatPlacePermDenyVerbose)) return;

		// Get
		final ItemStack current = event.getCurrentItem();

		// ... and make sure the current head item doesn't have curse of binding
		if (current != null && current.containsEnchantment(Enchantment.BINDING_CURSE)){
			return;
		}
		
		// ... then perform the switch.
		// We deny the normal result
		// NOTE: There is no need to cancel the event since that is just a proxy method for the line below.
		event.setResult(Event.Result.DENY);
		

		
		// Schedule swap
		Bukkit.getScheduler().scheduleSyncDelayedTask(MassiveHat.get(), () -> {
			// Set
			event.setCurrentItem(cursor);
			view.setCursor(current);
			
			// Update
			InventoryUtil.update(clicker);
		});
	}
	
}
