package com.massivecraft.massivecore.dialog.input;

/**
 * Number-range / slider dialog input.
 * <p>
 * Values are stored as {@link Float} in {@link com.massivecraft.massivecore.dialog.MDialogResponse}.
 * </p>
 */
public final class MDialogInputNumber implements MDialogInput
{
	/** Response key. */
	private final String key;
	
	/** Field label. */
	private final String label;
	
	/** Minimum slider value. */
	private final float start;
	
	/** Maximum slider value. */
	private final float end;
	
	/** Optional width hint. */
	private final Integer width;
	
	/** Optional format string for the live value label. */
	private final String labelFormat;
	
	/** Optional starting value; otherwise {@link #start} is used when seeding session. */
	private final Float initial;
	
	/** Optional step size for the slider. */
	private final Float step;
	
	/**
	 * @param key Input key.
	 * @param label Label text.
	 * @param start Range minimum.
	 * @param end Range maximum.
	 * @param width Width hint or null.
	 * @param labelFormat Value label format or null.
	 * @param initial Initial value or null.
	 * @param step Step or null.
	 */
	private MDialogInputNumber(String key, String label, float start, float end, Integer width, String labelFormat, Float initial, Float step)
	{
		this.key = key;
		this.label = label;
		this.start = start;
		this.end = end;
		this.width = width;
		this.labelFormat = labelFormat;
		this.initial = initial;
		this.step = step;
	}
	
	/**
	 * Creates a number input over {@code [start, end]}.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @param start Minimum value.
	 * @param end Maximum value.
	 * @return New input.
	 */
	public static MDialogInputNumber of(String key, String label, float start, float end)
	{
		return new MDialogInputNumber(key, label, start, end, null, null, null, null);
	}
	
	/**
	 * Returns a copy with an updated width hint.
	 *
	 * @param width Layout width.
	 * @return New input instance.
	 */
	public MDialogInputNumber width(int width)
	{
		return new MDialogInputNumber(this.key, this.label, this.start, this.end, width, this.labelFormat, this.initial, this.step);
	}
	
	/**
	 * Returns a copy with a value label format.
	 *
	 * @param labelFormat Format string or null.
	 * @return New input instance.
	 */
	public MDialogInputNumber labelFormat(String labelFormat)
	{
		return new MDialogInputNumber(this.key, this.label, this.start, this.end, this.width, labelFormat, this.initial, this.step);
	}
	
	/**
	 * Returns a copy with an initial value.
	 *
	 * @param initial Starting slider value.
	 * @return New input instance.
	 */
	public MDialogInputNumber initial(float initial)
	{
		return new MDialogInputNumber(this.key, this.label, this.start, this.end, this.width, this.labelFormat, initial, this.step);
	}
	
	/**
	 * Returns a copy with a step size.
	 *
	 * @param step Slider increment.
	 * @return New input instance.
	 */
	public MDialogInputNumber step(float step)
	{
		return new MDialogInputNumber(this.key, this.label, this.start, this.end, this.width, this.labelFormat, this.initial, step);
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
	 * @return Range minimum.
	 */
	public float getStart() { return this.start; }
	
	/**
	 * @return Range maximum.
	 */
	public float getEnd() { return this.end; }
	
	/**
	 * @return Width hint or null.
	 */
	public Integer getWidth() { return this.width; }
	
	/**
	 * @return Value label format or null.
	 */
	public String getLabelFormat() { return this.labelFormat; }
	
	/**
	 * @return Configured initial value or null.
	 */
	public Float getInitial() { return this.initial; }
	
	/**
	 * @return Step size or null.
	 */
	public Float getStep() { return this.step; }
}
