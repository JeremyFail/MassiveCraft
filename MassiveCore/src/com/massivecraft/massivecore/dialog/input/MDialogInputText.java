package com.massivecraft.massivecore.dialog.input;

/**
 * Text dialog input.
 * <p>
 * Supports single-line and multiline modes when {@link #multiline(Integer, Integer)} is used.
 * </p>
 */
public final class MDialogInputText implements MDialogInput
{
	/** Response key. */
	private final String key;
	
	/** Field label. */
	private final String label;
	
	/** Optional width hint. */
	private final Integer width;
	
	/** Whether the label is shown in the UI. */
	private final boolean labelVisible;
	
	/** Starting text value. */
	private final String initial;
	
	/** Max character length when set. */
	private final Integer maxLength;
	
	/** Multiline max lines when set. */
	private final Integer multilineMaxLines;
	
	/** Multiline height when set. */
	private final Integer multilineHeight;
	
	/**
	 * @param key Input key.
	 * @param label Label text.
	 * @param width Width hint or null.
	 * @param labelVisible Label visibility.
	 * @param initial Initial value or null.
	 * @param maxLength Max length or null.
	 * @param multilineMaxLines Multiline line cap or null.
	 * @param multilineHeight Multiline height or null.
	 */
	private MDialogInputText(String key, String label, Integer width, boolean labelVisible, String initial, Integer maxLength, Integer multilineMaxLines, Integer multilineHeight)
	{
		this.key = key;
		this.label = label;
		this.width = width;
		this.labelVisible = labelVisible;
		this.initial = initial;
		this.maxLength = maxLength;
		this.multilineMaxLines = multilineMaxLines;
		this.multilineHeight = multilineHeight;
	}
	
	/**
	 * Creates a text input with visible label.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return New input.
	 */
	public static MDialogInputText of(String key, String label)
	{
		return new MDialogInputText(key, label, null, true, null, null, null, null);
	}
	
	/**
	 * Returns a copy with an updated width hint.
	 *
	 * @param width Layout width.
	 * @return New input instance.
	 */
	public MDialogInputText width(int width)
	{
		return new MDialogInputText(this.key, this.label, width, this.labelVisible, this.initial, this.maxLength, this.multilineMaxLines, this.multilineHeight);
	}
	
	/**
	 * Returns a copy with updated label visibility.
	 *
	 * @param labelVisible True to show label.
	 * @return New input instance.
	 */
	public MDialogInputText labelVisible(boolean labelVisible)
	{
		return new MDialogInputText(this.key, this.label, this.width, labelVisible, this.initial, this.maxLength, this.multilineMaxLines, this.multilineHeight);
	}
	
	/**
	 * Returns a copy with an initial value.
	 *
	 * @param initial Starting text or null.
	 * @return New input instance.
	 */
	public MDialogInputText initial(String initial)
	{
		return new MDialogInputText(this.key, this.label, this.width, this.labelVisible, initial, this.maxLength, this.multilineMaxLines, this.multilineHeight);
	}
	
	/**
	 * Returns a copy with a max length constraint.
	 *
	 * @param maxLength Maximum characters.
	 * @return New input instance.
	 */
	public MDialogInputText maxLength(int maxLength)
	{
		return new MDialogInputText(this.key, this.label, this.width, this.labelVisible, this.initial, maxLength, this.multilineMaxLines, this.multilineHeight);
	}
	
	/**
	 * Returns a copy configured as multiline.
	 *
	 * @param maxLines Max lines or null.
	 * @param height Field height or null.
	 * @return New input instance.
	 */
	public MDialogInputText multiline(Integer maxLines, Integer height)
	{
		return new MDialogInputText(this.key, this.label, this.width, this.labelVisible, this.initial, this.maxLength, maxLines, height);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getKey() { return this.key; }
	
	/**
	 * @return Label text.
	 */
	public String getLabel() { return this.label; }
	
	/**
	 * @return Width hint or null.
	 */
	public Integer getWidth() { return this.width; }
	
	/**
	 * @return True if the label is shown.
	 */
	public boolean isLabelVisible() { return this.labelVisible; }
	
	/**
	 * @return Initial value or null.
	 */
	public String getInitial() { return this.initial; }
	
	/**
	 * @return Max length or null.
	 */
	public Integer getMaxLength() { return this.maxLength; }
	
	/**
	 * @return Multiline max lines or null.
	 */
	public Integer getMultilineMaxLines() { return this.multilineMaxLines; }
	
	/**
	 * @return Multiline height or null.
	 */
	public Integer getMultilineHeight() { return this.multilineHeight; }
	
	/**
	 * @return True when multiline limits or height are configured.
	 */
	public boolean isMultiline() { return this.multilineMaxLines != null || this.multilineHeight != null; }
}
