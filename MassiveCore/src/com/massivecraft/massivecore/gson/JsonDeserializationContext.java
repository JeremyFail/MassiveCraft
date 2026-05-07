package com.massivecraft.massivecore.gson;

import java.lang.reflect.Type;

/**
 * Callback passed to JsonDeserializer implementations so they can deserialize nested values
 * without constructing a Gson instance.
 * Wraps Gson's com.google.gson.JsonDeserializationContext; JsonElement arguments and results use this API.
 */
public interface JsonDeserializationContext
{
	/**
	 * Deserializes the given JSON element into an object of the requested type.
	 *
	 * @param json     the JSON subtree to deserialize; may be null for absent JSON null handling
	 * @param typeOfT  the full Type to deserialize as (use TypeToken for generics)
	 * @return the deserialized instance
	 * @throws JsonParseException if deserialization fails
	 */
	<T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException;

	/**
	 * Deserializes using a Class token; same behavior as deserialize(JsonElement, Type) with that class.
	 * Bridges delegate to the Type overload. Explicit overload helps IDEs resolve Foo.class call sites.
	 *
	 * @param json      the JSON subtree to deserialize; may be null
	 * @param classOfT  the target class
	 * @return the deserialized instance
	 * @throws JsonParseException if deserialization fails
	 */
	<T> T deserialize(JsonElement json, Class<T> classOfT) throws JsonParseException;
}
