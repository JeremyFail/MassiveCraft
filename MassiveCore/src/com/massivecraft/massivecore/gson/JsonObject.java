package com.massivecraft.massivecore.gson;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A JSON object: string keys mapped to {@link JsonElement} values.
 * <p>
 * Key order follows Gson's {@link com.google.gson.JsonObject}. Member {@code getAsJsonObject(String)}
 * and related methods return {@code null} when the named member is missing (same as Gson 2.13+).
 * </p>
 */
public final class JsonObject extends JsonElement
{
	/**
	 * Creates an empty JSON object.
	 */
	public JsonObject()
	{
		super(new com.google.gson.JsonObject());
	}

	/**
	 * Wraps an existing Gson object (used by {@link JsonElement#wrap} and internal bridges).
	 *
	 * @param nativeObject the Gson object to delegate to (must not be {@code null})
	 */
	JsonObject(com.google.gson.JsonObject nativeObject)
	{
		super(nativeObject);
	}

	// Underlying Gson object (same reference as JsonElement.nativeElement).
	private com.google.gson.JsonObject nativeObject()
	{
		return (com.google.gson.JsonObject) nativeElement;
	}

	@Override
	public JsonObject getAsJsonObject()
	{
		return this;
	}

	/**
	 * Adds or replaces a member. Passing {@code null} for {@code value} is forwarded to Gson
	 * (typically stored as JSON null).
	 *
	 * @param property name of the member
	 * @param value    JSON subtree for that member, or {@code null}
	 */
	public void add(String property, JsonElement value)
	{
		if (value == null) nativeObject().add(property, null);
		else nativeObject().add(property, value.unwrapNative());
	}

	/**
	 * Convenience: adds a string member (Gson creates a {@link JsonPrimitive}).
	 *
	 * @param property object key
	 * @param value    string value (Gson null rules apply)
	 */
	public void addProperty(String property, String value)
	{
		nativeObject().addProperty(property, value);
	}

	/**
	 * @param property object key
	 * @param value    numeric value
	 */
	public void addProperty(String property, Number value)
	{
		nativeObject().addProperty(property, value);
	}

	/**
	 * @param property object key
	 * @param value    boolean value
	 */
	public void addProperty(String property, Boolean value)
	{
		nativeObject().addProperty(property, value);
	}

	/**
	 * @param property object key
	 * @param value    character stored as JSON string of length one
	 */
	public void addProperty(String property, Character value)
	{
		nativeObject().addProperty(property, value);
	}

	/**
	 * Removes a member by name.
	 *
	 * @param property name of the member to remove
	 * @return the removed element, wrapped, or {@code null} if no such member existed
	 */
	public JsonElement remove(String property)
	{
		return JsonElement.wrap(nativeObject().remove(property));
	}

	/**
	 * Returns a member by name without coercing type.
	 *
	 * @param memberName key to look up
	 * @return the wrapped member, or {@code null} if absent
	 */
	public JsonElement get(String memberName)
	{
		return JsonElement.wrap(nativeObject().get(memberName));
	}

	/**
	 * Returns the member as a JSON object, or {@code null} if missing.
	 *
	 * @param memberName key to look up
	 * @return wrapped {@link JsonObject}, or {@code null} if there is no such member
	 * @throws ClassCastException if the member exists but is not a JSON object
	 * @see com.google.gson.JsonObject#getAsJsonObject(String)
	 */
	public JsonObject getAsJsonObject(String memberName)
	{
		com.google.gson.JsonObject o = nativeObject().getAsJsonObject(memberName);
		return o == null ? null : new JsonObject(o);
	}

	/**
	 * Returns the member as a JSON array, or {@code null} if missing.
	 *
	 * @param memberName key to look up
	 * @return wrapped {@link JsonArray}, or {@code null} if there is no such member
	 * @throws ClassCastException if the member exists but is not a JSON array
	 * @see com.google.gson.JsonObject#getAsJsonArray(String)
	 */
	public JsonArray getAsJsonArray(String memberName)
	{
		com.google.gson.JsonArray a = nativeObject().getAsJsonArray(memberName);
		return a == null ? null : new JsonArray(a);
	}

	/**
	 * Returns the member as a JSON primitive, or {@code null} if missing.
	 *
	 * @param memberName key to look up
	 * @return wrapped {@link JsonPrimitive}, or {@code null} if there is no such member
	 * @throws ClassCastException if the member exists but is not a primitive
	 * @see com.google.gson.JsonObject#getAsJsonPrimitive(String)
	 */
	public JsonPrimitive getAsJsonPrimitive(String memberName)
	{
		com.google.gson.JsonPrimitive p = nativeObject().getAsJsonPrimitive(memberName);
		return p == null ? null : new JsonPrimitive(p);
	}

	/**
	 * @param memberName key to test
	 * @return {@code true} if this object has a member with that name
	 */
	public boolean has(String memberName)
	{
		return nativeObject().has(memberName);
	}

	/**
	 * @return unmodifiable view of member names (iteration order matches insertion order in Gson)
	 */
	public Set<String> keySet()
	{
		return nativeObject().keySet();
	}

	/**
	 * @return number of name/value pairs
	 */
	public int size()
	{
		return nativeObject().size();
	}

	/**
	 * View of members as map entries. {@link Map.Entry#setValue(JsonElement)} is not supported
	 * (throws {@link UnsupportedOperationException}); use {@link #add(String, JsonElement)} to mutate.
	 *
	 * @return set of entries with wrapped values
	 */
	public Set<Map.Entry<String, JsonElement>> entrySet()
	{
		return new AbstractSet<Map.Entry<String, JsonElement>>()
		{
			@Override
			public Iterator<Map.Entry<String, JsonElement>> iterator()
			{
				final Iterator<Map.Entry<String, com.google.gson.JsonElement>> it = nativeObject().entrySet().iterator();
				return new Iterator<Map.Entry<String, JsonElement>>()
				{
					@Override
					public boolean hasNext()
					{
						return it.hasNext();
					}

					@Override
					public Map.Entry<String, JsonElement> next()
					{
						Map.Entry<String, com.google.gson.JsonElement> e = it.next();
						return new Map.Entry<String, JsonElement>()
						{
							@Override
							public String getKey()
							{
								return e.getKey();
							}

							@Override
							public JsonElement getValue()
							{
								return JsonElement.wrap(e.getValue());
							}

							@Override
							public JsonElement setValue(JsonElement value)
							{
								throw new UnsupportedOperationException();
							}
						};
					}
				};
			}

			@Override
			public int size()
			{
				return nativeObject().size();
			}
		};
	}
}
