package com.massivecraft.massivecore.chestgui;

import org.bukkit.inventory.ItemStack;

public class ChestButtonSimple implements ChestButton
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private final ChestAction action;
	@Override public ChestAction getAction() { return this.action; }

	private final ItemStack item;
	@Override public ItemStack getItem() { return this.item; }

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public ChestButtonSimple(ChestAction action, ItemStack item)
	{
		this.action = action;
		this.item = item;
	}

}
