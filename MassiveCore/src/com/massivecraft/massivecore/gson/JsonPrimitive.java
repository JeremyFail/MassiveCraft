package com.massivecraft.massivecore.gson;

/**
 * A JSON string, number, or boolean leaf value (not an object or array).
 * <p>
 * Delegates to {@link com.google.gson.JsonPrimitive}. Construct with the typed overloads or obtain
 * from {@link JsonElement#getAsJsonPrimitive()} / {@link JsonObject#getAsJsonPrimitive(String)}.
 * </p>
 */
public final class JsonPrimitive extends JsonElement
{
	/**
	 * Wraps a boolean as a JSON boolean primitive.
	 *
	 * @param bool the value (must not be {@code null}; use JSON null via {@link JsonNull} if needed)
	 */
	public JsonPrimitive(Boolean bool)
	{
		super(new com.google.gson.JsonPrimitive(bool));
	}

	/**
	 * Wraps a number as a JSON number primitive.
	 *
	 * @param number the value (must not be {@code null})
	 */
	public JsonPrimitive(Number number)
	{
		super(new com.google.gson.JsonPrimitive(number));
	}

	/**
	 * Wraps a string as a JSON string primitive.
	 *
	 * @param string the value (may be {@code null} per Gson rules for {@link com.google.gson.JsonPrimitive})
	 */
	public JsonPrimitive(String string)
	{
		super(new com.google.gson.JsonPrimitive(string));
	}

	/**
	 * Wraps a character as a JSON string primitive of length one.
	 *
	 * @param c the character (must not be {@code null})
	 */
	public JsonPrimitive(Character c)
	{
		super(new com.google.gson.JsonPrimitive(c));
	}

	/**
	 * Package-private: wraps an existing Gson primitive (used by {@link JsonElement#wrap}).
	 *
	 * @param nativePrimitive the Gson primitive to delegate to (must not be {@code null})
	 */
	JsonPrimitive(com.google.gson.JsonPrimitive nativePrimitive)
	{
		super(nativePrimitive);
	}

	@Override
	public JsonPrimitive getAsJsonPrimitive()
	{
		return this;
	}

	// Underlying Gson node (same instance as JsonElement.nativeElement, narrowed).
	private com.google.gson.JsonPrimitive nativePrimitive()
	{
		return (com.google.gson.JsonPrimitive) nativeElement;
	}

	/**
	 * @return {@code true} if this primitive holds a boolean
	 * @see com.google.gson.JsonPrimitive#isBoolean()
	 */
	public boolean isBoolean()
	{
		return nativePrimitive().isBoolean();
	}

	/**
	 * @return {@code true} if this primitive holds a number
	 * @see com.google.gson.JsonPrimitive#isNumber()
	 */
	public boolean isNumber()
	{
		return nativePrimitive().isNumber();
	}

	/**
	 * @return {@code true} if this primitive holds a string
	 * @see com.google.gson.JsonPrimitive#isString()
	 */
	public boolean isString()
	{
		return nativePrimitive().isString();
	}
}
