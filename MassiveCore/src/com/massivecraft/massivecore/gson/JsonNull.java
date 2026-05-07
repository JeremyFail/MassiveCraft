package com.massivecraft.massivecore.gson;

/**
 * Represents the JSON {@code null} literal in the MassiveCore tree API.
 * <p>
 * Prefer {@link #INSTANCE} for the canonical singleton, matching {@link com.google.gson.JsonNull#INSTANCE}.
 * </p>
 */
public final class JsonNull extends JsonElement
{
	/**
	 * Shared singleton for JSON null (same role as {@link com.google.gson.JsonNull#INSTANCE}).
	 */
	public static final JsonNull INSTANCE = new JsonNull(com.google.gson.JsonNull.INSTANCE);

	/**
	 * Returns {@link #INSTANCE} when the native element is Gson's singleton; otherwise wraps the given null node.
	 *
	 * @param nativeNull the Gson {@link com.google.gson.JsonNull} (must not be {@code null})
	 * @return a {@link JsonNull} wrapper
	 */
	static JsonNull wrap(com.google.gson.JsonNull nativeNull)
	{
		return nativeNull == com.google.gson.JsonNull.INSTANCE ? INSTANCE : new JsonNull(nativeNull);
	}

	/**
	 * @param nativeNull the Gson null element to wrap (must not be {@code null})
	 */
	private JsonNull(com.google.gson.JsonNull nativeNull)
	{
		super(nativeNull);
	}
}
