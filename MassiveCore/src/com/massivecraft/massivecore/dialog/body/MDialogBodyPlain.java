package com.massivecraft.massivecore.dialog.body;

import com.massivecraft.massivecore.dialog.MDialogText;
import com.massivecraft.massivecore.mson.Mson;

/**
 * Plain message body line.
 * <p>
 * Immutable; use {@link #width(int)} for layout hints on supporting backends.
 * Text may be Txt markup, {@link Mson}, or Adventure via {@link MDialogText}.
 * </p>
 */
public final class MDialogBodyPlain implements MDialogBody
{
	/** Message text. */
	private final MDialogText message;
	
	/** Optional width hint. */
	private final Integer width;
	
	private MDialogBodyPlain(MDialogText message, Integer width)
	{
		this.message = message == null ? MDialogText.txt("") : message;
		this.width = width;
	}
	
	/**
	 * Creates a plain body line from Txt markup.
	 *
	 * @param message Text to display.
	 * @return New body element.
	 */
	public static MDialogBodyPlain of(String message)
	{
		return new MDialogBodyPlain(MDialogText.txt(message), null);
	}
	
	/**
	 * Creates a plain body line from rich dialog text.
	 *
	 * @param message Text to display.
	 * @return New body element.
	 */
	public static MDialogBodyPlain of(MDialogText message)
	{
		return new MDialogBodyPlain(message, null);
	}
	
	/**
	 * Creates a plain body line from {@link Mson}.
	 *
	 * @param message Text to display.
	 * @return New body element.
	 */
	public static MDialogBodyPlain of(Mson message)
	{
		return new MDialogBodyPlain(MDialogText.mson(message), null);
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
	 * @return Rich message text.
	 */
	public MDialogText getMessageText() { return this.message; }
	
	/**
	 * Plain / legacy message for callers that need a string.
	 *
	 * @return Styled plain message.
	 */
	public String getMessage() { return this.message.toPlain(); }
	
	/**
	 * @return Width hint or null.
	 */
	public Integer getWidth() { return this.width; }
}
