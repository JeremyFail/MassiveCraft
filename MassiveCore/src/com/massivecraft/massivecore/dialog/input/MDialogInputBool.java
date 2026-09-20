package com.massivecraft.massivecore.dialog.input;

import com.massivecraft.massivecore.dialog.MDialogText;
import com.massivecraft.massivecore.mson.Mson;

/**
 * Boolean / checkbox dialog input.
 * <p>
 * Optional {@link #onTrue(String)} / {@link #onFalse(String)} customize displayed state labels on some backends.
 * Labels may be {@link MDialogText} (Txt, {@link Mson}, or Adventure) for color and hover tooltips.
 * </p>
 */
public final class MDialogInputBool implements MDialogInput
{
	/** Response key. */
	private final String key;
	
	/** Field label. */
	private final MDialogText label;
	
	/** Starting checked state. */
	private final boolean initial;
	
	/** Label when true, if customized. */
	private final String onTrue;
	
	/** Label when false, if customized. */
	private final String onFalse;
	
	private MDialogInputBool(String key, MDialogText label, boolean initial, String onTrue, String onFalse)
	{
		this.key = key;
		this.label = label == null ? MDialogText.txt("") : label;
		this.initial = initial;
		this.onTrue = onTrue;
		this.onFalse = onFalse;
	}
	
	/**
	 * Creates a checkbox with a Txt-markup label, defaulting to unchecked.
	 *
	 * @param key Response key.
	 * @param label Field label (Txt tags allowed).
	 * @return New input.
	 */
	public static MDialogInputBool of(String key, String label)
	{
		return new MDialogInputBool(key, MDialogText.txt(label), false, null, null);
	}
	
	/**
	 * Creates a checkbox with rich {@link MDialogText} label, defaulting to unchecked.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return New input.
	 */
	public static MDialogInputBool of(String key, MDialogText label)
	{
		return new MDialogInputBool(key, label, false, null, null);
	}
	
	/**
	 * Creates a checkbox with an {@link Mson} label, defaulting to unchecked.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return New input.
	 */
	public static MDialogInputBool of(String key, Mson label)
	{
		return new MDialogInputBool(key, MDialogText.mson(label), false, null, null);
	}
	
	/**
	 * Returns a copy with an initial checked state.
	 *
	 * @param initial Starting value.
	 * @return New input instance.
	 */
	public MDialogInputBool initial(boolean initial)
	{
		return new MDialogInputBool(this.key, this.label, initial, this.onTrue, this.onFalse);
	}
	
	/**
	 * Returns a copy with a custom label when checked.
	 *
	 * @param onTrue True-state text or null.
	 * @return New input instance.
	 */
	public MDialogInputBool onTrue(String onTrue)
	{
		return new MDialogInputBool(this.key, this.label, this.initial, onTrue, this.onFalse);
	}
	
	/**
	 * Returns a copy with a custom label when unchecked.
	 *
	 * @param onFalse False-state text or null.
	 * @return New input instance.
	 */
	public MDialogInputBool onFalse(String onFalse)
	{
		return new MDialogInputBool(this.key, this.label, this.initial, this.onTrue, onFalse);
	}
	
	@Override
	public String getKey() { return this.key; }
	
	/**
	 * @return Rich label text.
	 */
	public MDialogText getLabelText() { return this.label; }
	
	/**
	 * Plain / legacy label for ChestGui and callers that need a string.
	 *
	 * @return Styled plain label.
	 */
	public String getLabel() { return this.label.toPlain(); }
	
	/**
	 * @return Initial checked state.
	 */
	public boolean getInitial() { return this.initial; }
	
	/**
	 * @return Custom true label or null.
	 */
	public String getOnTrue() { return this.onTrue; }
	
	/**
	 * @return Custom false label or null.
	 */
	public String getOnFalse() { return this.onFalse; }
}
