package com.massivecraft.massivecore.dialog.backend;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.dialog.body.DialogBody;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Spigot-side {@code minecraft:item} dialog body.
 * <p>
 * BungeeCord's dialog module still has no {@code ItemBody} type, but the client accepts
 * vanilla item bodies. This class is shaped for reflective Gson serialization used by
 * Spigot's dialog serializer (field names match the dialog JSON schema).
 * </p>
 */
final class SpigotItemDialogBody extends DialogBody
{
	/** Vanilla item stack payload ({@code id} / {@code count}). */
	private final ItemRef item;
	
	/** Optional description shown beside the icon. */
	private final Description description;
	
	/** Whether count / damage overlays render on the icon. */
	private final boolean show_decoration;
	
	/** Whether the item tooltip shows on hover. */
	private final boolean show_tooltip;
	
	/** Icon slot width (1–256), or null for default. */
	private final Integer width;
	
	/** Icon slot height (1–256), or null for default. */
	private final Integer height;
	
	private SpigotItemDialogBody(ItemRef item, Description description, boolean showDecoration, boolean showTooltip, Integer width, Integer height)
	{
		super("minecraft:item");
		this.item = item;
		this.description = description;
		this.show_decoration = showDecoration;
		this.show_tooltip = showTooltip;
		this.width = width;
		this.height = height;
	}
	
	/**
	 * Builds an item body from a Bukkit stack and optional description text.
	 *
	 * @param stack Bukkit item; null yields null.
	 * @param description Clickable / styled description, or null.
	 * @param showDecorations Count / damage overlays.
	 * @param showTooltip Item tooltip on hover.
	 * @param width Icon width or null.
	 * @param height Icon height or null.
	 * @return Body element, or null when the stack cannot be represented.
	 */
	static SpigotItemDialogBody of(ItemStack stack, BaseComponent description, boolean showDecorations, boolean showTooltip, Integer width, Integer height)
	{
		ItemRef item = ItemRef.of(stack);
		if (item == null) return null;
		Description desc = description == null ? null : new Description(description, null);
		return new SpigotItemDialogBody(item, desc, showDecorations, showTooltip, width, height);
	}
	
	/**
	 * Minimal item stack reference for dialog JSON.
	 */
	static final class ItemRef
	{
		/** Namespaced item id (e.g. {@code minecraft:stone}). */
		private final String id;
		
		/** Stack count. */
		private final int count;
		
		private ItemRef(String id, int count)
		{
			this.id = id;
			this.count = count;
		}
		
		/**
		 * @param stack Bukkit stack.
		 * @return Item ref, or null if material has no key.
		 */
		static ItemRef of(ItemStack stack)
		{
			if (stack == null) return null;
			Material material = stack.getType();
			if (material == null || material.isAir()) return null;
			String id = material.getKey() == null ? null : material.getKey().toString();
			if (id == null || id.isEmpty()) return null;
			int count = Math.max(1, stack.getAmount());
			return new ItemRef(id, count);
		}
	}
	
	/**
	 * Description compound nested under an item body ({@code contents} + optional {@code width}).
	 */
	static final class Description
	{
		/** Description text component. */
		private final BaseComponent contents;
		
		/** Optional max width. */
		private final Integer width;
		
		private Description(BaseComponent contents, Integer width)
		{
			this.contents = contents;
			this.width = width;
		}
	}
}
