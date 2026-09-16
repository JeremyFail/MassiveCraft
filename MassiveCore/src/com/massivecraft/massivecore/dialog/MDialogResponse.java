package com.massivecraft.massivecore.dialog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Values submitted with a dialog button click (inputs + which button).
 * <p>
 * Maps are unmodifiable snapshots; keys match {@link com.massivecraft.massivecore.dialog.input.MDialogInput#getKey()}.
 * Single-option inputs store the selected option id in {@link #getText(String)}.
 * </p>
 */
public final class MDialogResponse
{
	/** Id of the button the player clicked. */
	private final String buttonId;
	
	/** Text and single-option values at click time. */
	private final Map<String, String> texts;
	
	/** Boolean input values at click time. */
	private final Map<String, Boolean> booleans;
	
	/** Number input values at click time. */
	private final Map<String, Float> numbers;
	
	/**
	 * @param buttonId Activated button id.
	 * @param texts Text map; null becomes empty.
	 * @param booleans Boolean map; null becomes empty.
	 * @param numbers Number map; null becomes empty.
	 */
	public MDialogResponse(String buttonId, Map<String, String> texts, Map<String, Boolean> booleans, Map<String, Float> numbers)
	{
		this.buttonId = buttonId;
		// Preserve iteration order and prevent caller mutation.
		this.texts = texts == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(texts));
		this.booleans = booleans == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(booleans));
		this.numbers = numbers == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(numbers));
	}
	
	/**
	 * Builds a response with only a button id (no input values).
	 *
	 * @param buttonId Button id.
	 * @return Empty-valued response.
	 */
	public static MDialogResponse ofButton(String buttonId)
	{
		return new MDialogResponse(buttonId, null, null, null);
	}
	
	/**
	 * @return Clicked button id.
	 */
	public String getButtonId() { return this.buttonId; }
	
	/**
	 * @param key Input key.
	 * @return Text value or null if absent.
	 */
	public String getText(String key)
	{
		return this.texts.get(key);
	}
	
	/**
	 * @param key Input key.
	 * @return Boolean value or null if absent.
	 */
	public Boolean getBoolean(String key)
	{
		return this.booleans.get(key);
	}
	
	/**
	 * @param key Input key.
	 * @return Float value or null if absent.
	 */
	public Float getFloat(String key)
	{
		return this.numbers.get(key);
	}
	
	/**
	 * @return Unmodifiable text / option map.
	 */
	public Map<String, String> getTexts() { return this.texts; }
	
	/**
	 * @return Unmodifiable boolean map.
	 */
	public Map<String, Boolean> getBooleans() { return this.booleans; }
	
	/**
	 * @return Unmodifiable number map.
	 */
	public Map<String, Float> getNumbers() { return this.numbers; }
}
