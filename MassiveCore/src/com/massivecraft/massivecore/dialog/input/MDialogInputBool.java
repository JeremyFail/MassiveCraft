package com.massivecraft.massivecore.dialog.input;

/**
 * Boolean / checkbox dialog input.
 * <p>
 * Optional {@link #onTrue(String)} / {@link #onFalse(String)} customize displayed state labels on some backends.
 * </p>
 */
public final class MDialogInputBool implements MDialogInput
{
	/** Response key. */
	private final String key;
	
	/** Field label. */
	private final String label;
	
	/** Starting checked state. */
	private final boolean initial;
	
	/** Label when true, if customized. */
	private final String onTrue;
	
	/** Label when false, if customized. */
	private final String onFalse;
	
	/**
	 * @param key Input key.
	 * @param label Label text.
	 * @param initial Initial boolean.
	 * @param onTrue True-state label or null.
	 * @param onFalse False-state label or null.
	 */
	private MDialogInputBool(String key, String label, boolean initial, String onTrue, String onFalse)
	{
		this.key = key;
		this.label = label;
		this.initial = initial;
		this.onTrue = onTrue;
		this.onFalse = onFalse;
	}
	
	/**
	 * Creates a checkbox defaulting to unchecked.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return New input.
	 */
	public static MDialogInputBool of(String key, String label)
	{
		return new MDialogInputBool(key, label, false, null, null);
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
