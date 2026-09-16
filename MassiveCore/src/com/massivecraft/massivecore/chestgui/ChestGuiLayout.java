package com.massivecraft.massivecore.chestgui;

import org.bukkit.event.inventory.InventoryType;

/**
 * Virtual menu shape. Generic chests are 9-wide (1–6 rows). Hopper and dropper are icon menus, not block inventories.
 */
public enum ChestGuiLayout
{
	CHEST_1_ROW(9, null),
	CHEST_2_ROW(18, null),
	CHEST_3_ROW(27, null),
	CHEST_4_ROW(36, null),
	CHEST_5_ROW(45, null),
	CHEST_6_ROW(54, null),
	HOPPER(5, InventoryType.HOPPER),
	DROPPER(9, InventoryType.DROPPER),
	;
	
	private final int size;
	private final InventoryType specialType;
	
	ChestGuiLayout(int size, InventoryType specialType)
	{
		this.size = size;
		this.specialType = specialType;
	}
	
	public int getSize() { return this.size; }
	public boolean isGenericChest() { return this.specialType == null; }
	public InventoryType getSpecialType() { return this.specialType; }
	
	public static ChestGuiLayout chest(int slots)
	{
		int size = slots;
		if (size < 9) size = 9;
		if (size > 54) size = 54;
		int rem = size % 9;
		if (rem != 0) size = Math.min(54, size + 9 - rem);
		
		switch (size)
		{
			case 9: return CHEST_1_ROW;
			case 18: return CHEST_2_ROW;
			case 27: return CHEST_3_ROW;
			case 36: return CHEST_4_ROW;
			case 45: return CHEST_5_ROW;
			default: return CHEST_6_ROW;
		}
	}
	
	public static int slot(int row, int column)
	{
		return row * 9 + column;
	}
	
}
