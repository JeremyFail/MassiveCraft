package com.massivecraft.massivecore.gson;

import java.lang.reflect.Type;

/**
 * Custom deserializer hook for a generic type T, using MassiveCore JsonElement trees.
 * Register via GsonBuilder.registerTypeAdapter; MassiveCore maps this to Gson's native JsonDeserializer.
 */
public interface JsonDeserializer<T>
{
	/**
	 * Gson calls this when reading JSON into T.
	 *
	 * @param json     JSON subtree for this value
	 * @param typeOfT  concrete Type being deserialized (generics via TypeToken)
	 * @param context  context for delegating nested deserialization
	 * @return the deserialized object
	 * @throws JsonParseException if JSON cannot be read as T
	 */
	T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException;
}
