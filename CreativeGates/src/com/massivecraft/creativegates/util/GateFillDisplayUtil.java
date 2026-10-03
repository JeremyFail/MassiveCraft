package com.massivecraft.creativegates.util;

import com.massivecraft.massivecore.collections.BackstringSet;
import org.bukkit.Material;

/**
 * Helpers for choosing how creative-gate fill looks are rendered client-side.
 * <p>
 * Face-attached / flat “clump” blocks often look better as {@link org.bukkit.entity.ItemDisplay}
 * (inventory item model) than as a squashed {@link org.bukkit.entity.BlockDisplay}. Materials are
 * stored as strings via {@link BackstringSet} so names from newer Minecraft versions can be listed
 * without requiring the enum constant on older runtimes.
 * </p>
 */
public final class GateFillDisplayUtil
{
	/**
	 * Materials that use {@link org.bukkit.entity.ItemDisplay} instead of BlockDisplay for gate fill.
	 * Unknown names on the current server are ignored when resolving to {@link Material}.
	 */
	public static final BackstringSet<Material> MATERIALS_ITEM_DISPLAY_FILL = new BackstringSet<>(Material.class,
		"GLOW_LICHEN",			// Minecraft 1.17
		"LEAF_LITTER",			// Minecraft 26.3
		"PALE_HANGING_MOSS", 	// Minecraft 1.21.4
		"SCULK_VEIN",			// Minecraft 1.19
		"TWISTING_VINES",		// Minecraft 1.16
		"VINE",					// Minecraft 1.8
		"WEEPING_VINES" 		// Minecraft 1.16
	);
	
	private GateFillDisplayUtil()
	{
	}
	
	/**
	 * Whether this fill material should be shown with an ItemDisplay rather than a BlockDisplay.
	 *
	 * @param material Client display material; null returns false.
	 * @return True when {@code material} is in {@link #MATERIALS_ITEM_DISPLAY_FILL}.
	 */
	public static boolean usesItemDisplay(Material material)
	{
		return material != null && MATERIALS_ITEM_DISPLAY_FILL.contains(material);
	}
}
