package com.massivecraft.massivecore.gson;

import java.lang.reflect.Type;

/**
 * Custom serializer hook for a generic type T, producing MassiveCore JsonElement trees.
 * Register via GsonBuilder.registerTypeAdapter; MassiveCore maps this to Gson's native JsonSerializer.
 */
public interface JsonSerializer<T>
{
	/**
	 * Gson calls this when writing a T instance to JSON.
	 *
	 * @param src        the object to serialize; may be null per Gson rules
	 * @param typeOfSrc  declared type of src (includes generics when applicable)
	 * @param context    context for delegating nested serialization
	 * @return the JSON representation, or null for JSON null
	 */
	JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context);
}
