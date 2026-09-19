package com.massivecraft.massivecore.dialog.body;

import com.massivecraft.massivecore.dialog.MDialogClickHandler;
import org.bukkit.inventory.ItemStack;

/**
 * Item display body entry.
 * <p>
 * Shows an {@link ItemStack} with optional description and decoration/tooltip flags.
 * If {@link #clickId(String)} is set, clicking the description completes that id.
 * Pair it with {@link #onClick(MDialogClickHandler)} when there is no matching footer button.
 * </p>
 */
public final class MDialogBodyItem implements MDialogBody
{
	/** Item to render; cloned on access. */
	private final ItemStack item;
	
	/** Optional text beside or below the item. */
	private final String description;
	
	/** Whether enchant glint / count overlays show. */
	private final boolean showDecorations;
	
	/** Whether vanilla item tooltip shows on hover. */
	private final boolean showTooltip;
	
	/** Optional layout width. */
	private final Integer width;
	
	/** Optional layout height. */
	private final Integer height;
	
	/** Optional action id completed when the description is clicked. */
	private final String clickId;
	
	/** Optional click handler when this description is used as the action (no footer button). */
	private final MDialogClickHandler clickHandler;
	
	/**
	 * @param item Item stack or null.
	 * @param description Description text or null.
	 * @param showDecorations Decoration flag.
	 * @param showTooltip Tooltip flag.
	 * @param width Width hint or null.
	 * @param height Height hint or null.
	 * @param clickId Action id, or null.
	 * @param clickHandler Click handler or null.
	 */
	private MDialogBodyItem(ItemStack item, String description, boolean showDecorations, boolean showTooltip, Integer width, Integer height, String clickId, MDialogClickHandler clickHandler)
	{
		this.item = item == null ? null : item.clone();
		this.description = description;
		this.showDecorations = showDecorations;
		this.showTooltip = showTooltip;
		this.width = width;
		this.height = height;
		this.clickId = clickId;
		this.clickHandler = clickHandler;
	}
	
	/**
	 * Creates an item body with default decoration and tooltip visibility.
	 *
	 * @param item Item to show.
	 * @return New body element.
	 */
	public static MDialogBodyItem of(ItemStack item)
	{
		return new MDialogBodyItem(item, null, true, true, null, null, null, null);
	}
	
	/**
	 * Returns a copy with an updated description.
	 *
	 * @param description Description or null.
	 * @return New body instance.
	 */
	public MDialogBodyItem description(String description)
	{
		return new MDialogBodyItem(this.item, description, this.showDecorations, this.showTooltip, this.width, this.height, this.clickId, this.clickHandler);
	}
	
	/**
	 * Returns a copy with updated decoration visibility.
	 *
	 * @param showDecorations True to show count/enchant overlays.
	 * @return New body instance.
	 */
	public MDialogBodyItem showDecorations(boolean showDecorations)
	{
		return new MDialogBodyItem(this.item, this.description, showDecorations, this.showTooltip, this.width, this.height, this.clickId, this.clickHandler);
	}
	
	/**
	 * Returns a copy with updated tooltip visibility.
	 *
	 * @param showTooltip True to show item tooltip.
	 * @return New body instance.
	 */
	public MDialogBodyItem showTooltip(boolean showTooltip)
	{
		return new MDialogBodyItem(this.item, this.description, this.showDecorations, showTooltip, this.width, this.height, this.clickId, this.clickHandler);
	}
	
	/**
	 * Returns a copy with an updated width hint.
	 *
	 * @param width Layout width.
	 * @return New body instance.
	 */
	public MDialogBodyItem width(int width)
	{
		return new MDialogBodyItem(this.item, this.description, this.showDecorations, this.showTooltip, width, this.height, this.clickId, this.clickHandler);
	}
	
	/**
	 * Returns a copy with an updated height hint.
	 *
	 * @param height Layout height.
	 * @return New body instance.
	 */
	public MDialogBodyItem height(int height)
	{
		return new MDialogBodyItem(this.item, this.description, this.showDecorations, this.showTooltip, this.width, height, this.clickId, this.clickHandler);
	}
	
	/**
	 * Returns a copy that completes the given action id when the description is clicked.
	 *
	 * @param clickId Action id, or null to clear.
	 * @return New body instance.
	 */
	public MDialogBodyItem clickId(String clickId)
	{
		return new MDialogBodyItem(this.item, this.description, this.showDecorations, this.showTooltip, this.width, this.height, clickId, this.clickHandler);
	}
	
	/**
	 * Returns a copy with a click handler for this description.
	 *
	 * @param clickHandler Handler or null to clear.
	 * @return New body instance.
	 */
	public MDialogBodyItem onClick(MDialogClickHandler clickHandler)
	{
		return new MDialogBodyItem(this.item, this.description, this.showDecorations, this.showTooltip, this.width, this.height, this.clickId, clickHandler);
	}
	
	/**
	 * @return Cloned item or null.
	 */
	public ItemStack getItem() { return this.item == null ? null : this.item.clone(); }
	
	/**
	 * @return Description or null.
	 */
	public String getDescription() { return this.description; }
	
	/**
	 * @return True if decorations should show.
	 */
	public boolean isShowDecorations() { return this.showDecorations; }
	
	/**
	 * @return True if item tooltip should show.
	 */
	public boolean isShowTooltip() { return this.showTooltip; }
	
	/**
	 * @return Width hint or null.
	 */
	public Integer getWidth() { return this.width; }
	
	/**
	 * @return Height hint or null.
	 */
	public Integer getHeight() { return this.height; }
	
	/**
	 * @return Action id completed on description click, or null.
	 */
	public String getClickId() { return this.clickId; }
	
	/**
	 * @return Click handler or null.
	 */
	public MDialogClickHandler getClickHandler() { return this.clickHandler; }
}
