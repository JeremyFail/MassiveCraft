package com.massivecraft.massivecore.gson;

/**
 * Parses JSON text into a {@link JsonElement} tree.
 * <p>
 * Prefer the static {@link #parseString(String)} method (aligned with Gson 2.8.6+). The no-arg
 * constructor exists for legacy {@code new JsonParser().parse(...)} call sites.
 * </p>
 */
public final class JsonParser
{
	/**
	 * Constructs a parser instance (stateless; equivalent to using static {@link #parseString(String)}).
	 */
	public JsonParser()
	{
	}

	/**
	 * Parses JSON text into a tree.
	 *
	 * @param json the JSON string to parse
	 * @return the root element (object, array, primitive, or null)
	 * @throws JsonSyntaxException if {@code json} is not valid JSON
	 * @deprecated use {@link #parseString(String)} instead
	 */
	@Deprecated
	public JsonElement parse(String json) throws JsonSyntaxException
	{
		return parseString(json);
	}

	/**
	 * Parses JSON text into a MassiveCore {@link JsonElement} tree.
	 *
	 * @param json the JSON string to parse (must not be {@code null})
	 * @return the root element
	 * @throws JsonSyntaxException if {@code json} is not valid JSON
	 */
	public static JsonElement parseString(String json) throws JsonSyntaxException
	{
		try
		{
			return JsonElement.wrap(com.google.gson.JsonParser.parseString(json));
		}
		catch (com.google.gson.JsonSyntaxException e)
		{
			// Surface public API exception type (not shaded com.google.gson).
			throw new JsonSyntaxException(e.getMessage(), e);
		}
	}
}
