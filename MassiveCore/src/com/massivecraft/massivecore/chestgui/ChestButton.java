package com.massivecraft.massivecore.chestgui;

import org.bukkit.inventory.ItemStack;

// This class is just used to construct ChestGui's
public interface ChestButton
{
	ChestAction getAction();
	ItemStack getItem();
}
