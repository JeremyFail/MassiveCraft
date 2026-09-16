package com.massivecraft.massivecore.chestgui;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Click handler for a ChestGui slot.
 * <p>
 * The engine always cancels the Bukkit click first so virtual items cannot be taken.
 * Returning {@code false} does <em>not</em> un-cancel that. It only means “this click
 * was not consumed”: {@code lastAction} is left unchanged and autoclose does not run.
 * Return {@code true} when the click did its job (command ran, option picked, …).
 * </p>
 */
public interface ChestAction
{
	/**
	 * @param event Already-cancelled click on this action's slot.
	 * @return {@code true} if consumed; {@code false} to ignore for lastAction/autoclose.
	 */
	boolean onClick(InventoryClickEvent event);
}
