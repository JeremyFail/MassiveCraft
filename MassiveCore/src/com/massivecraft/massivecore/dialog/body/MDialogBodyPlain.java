package com.massivecraft.massivecore.dialog.body;

/**
 * Plain message body line.
 * <p>
 * Immutable; use {@link #width(int)} for layout hints on supporting backends.
 * </p>
 */
public final class MDialogBodyPlain implements MDialogBody
{
	/** Message text (MassiveCore formatting via {@link com.massivecraft.massivecore.dialog.MDialogTexts}). */
	private final String message;
	
	/** Optional width hint. */
	private final Integer width;
	
	/**
	 * @param message Body text.
	 * @param width Width hint or null.
	 */
	private MDialogBodyPlain(String message, Integer width)
	{
		this.message = message;
		this.width = width;
	}
	
	/**
	 * Creates a plain body line.
	 *
	 * @param message Text to display.
	 * @return New body element.
	 */
	public static MDialogBodyPlain of(String message)
	{
		return new MDialogBodyPlain(message, null);
	}
	
	/**
	 * Returns a copy with an updated width hint.
	 *
	 * @param width Layout width.
	 * @return New body instance.
	 */
	public MDialogBodyPlain width(int width)
	{
		return new MDialogBodyPlain(this.message, width);
	}
	
	/**
	 * @return Message text.
	 */
	public String getMessage() { return this.message; }
	
	/**
	 * @return Width hint or null.
	 */
	public Integer getWidth() { return this.width; }
}
