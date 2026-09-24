package com.massivecraft.massivecore.dialog;

import org.bukkit.inventory.ItemStack;

/**
 * A clickable dialog action button.
 * <p>
 * Immutable value type with fluent copy methods ({@link #tooltip(String)}, etc.).
 * Handlers are invoked via {@link com.massivecraft.massivecore.engine.EngineMassiveCoreDialog#completeClick}.
 * </p>
 */
public final class MDialogButton
{
	/** Stable id referenced in {@link MDialogResponse#getButtonId()}. */
	private final String id;
	
	/** Visible label (MassiveCore color codes allowed). */
	private final String label;
	
	/** Optional hover text. */
	private final String tooltip;
	
	/** Optional icon; stored defensively cloned. */
	private final ItemStack icon;
	
	/** Optional layout width hint for backends that support it. */
	private final Integer width;
	
	/** Plugin callback when this button is activated. */
	private final MDialogClickHandler clickHandler;
	
	/**
	 * @param id Button id.
	 * @param label Label text.
	 * @param tooltip Tooltip or null.
	 * @param icon Icon stack or null.
	 * @param width Width hint or null.
	 * @param clickHandler Click handler or null.
	 */
	private MDialogButton(String id, String label, String tooltip, ItemStack icon, Integer width, MDialogClickHandler clickHandler)
	{
		this.id = id;
		this.label = label;
		this.tooltip = tooltip;
		// Never expose mutable ItemStack references.
		this.icon = icon == null ? null : icon.clone();
		this.width = width;
		this.clickHandler = clickHandler;
	}
	
	/**
	 * Creates a minimal button with id and label only.
	 *
	 * @param id Non-null button id.
	 * @param label Visible label.
	 * @return New button.
	 */
	public static MDialogButton of(String id, String label)
	{
		return new MDialogButton(id, label, null, null, null, null);
	}
	
	/**
	 * Returns a copy with an updated tooltip.
	 *
	 * @param tooltip Tooltip text or null to clear.
	 * @return New button instance.
	 */
	public MDialogButton tooltip(String tooltip)
	{
		return new MDialogButton(this.id, this.label, tooltip, this.icon, this.width, this.clickHandler);
	}
	
	/**
	 * Returns a copy with an updated icon.
	 *
	 * @param icon Item stack or null to clear.
	 * @return New button instance.
	 */
	public MDialogButton icon(ItemStack icon)
	{
		return new MDialogButton(this.id, this.label, this.tooltip, icon, this.width, this.clickHandler);
	}
	
	/**
	 * Returns a copy with an updated width hint.
	 *
	 * @param width Layout width.
	 * @return New button instance.
	 */
	public MDialogButton width(int width)
	{
		return new MDialogButton(this.id, this.label, this.tooltip, this.icon, width, this.clickHandler);
	}
	
	/**
	 * Returns a copy with a click handler.
	 *
	 * @param clickHandler Handler or null.
	 * @return New button instance.
	 */
	public MDialogButton onClick(MDialogClickHandler clickHandler)
	{
		return new MDialogButton(this.id, this.label, this.tooltip, this.icon, this.width, clickHandler);
	}
	
	/**
	 * @return Button id.
	 */
	public String getId() { return this.id; }
	
	/**
	 * @return Display label.
	 */
	public String getLabel() { return this.label; }
	
	/**
	 * @return Tooltip or null.
	 */
	public String getTooltip() { return this.tooltip; }
	
	/**
	 * @return Cloned icon or null.
	 */
	public ItemStack getIcon() { return this.icon == null ? null : this.icon.clone(); }
	
	/**
	 * @return Width hint or null.
	 */
	public Integer getWidth() { return this.width; }
	
	/**
	 * @return Click handler or null.
	 */
	public MDialogClickHandler getClickHandler() { return this.clickHandler; }
}
