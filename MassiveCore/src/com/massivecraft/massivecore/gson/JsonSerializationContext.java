package com.massivecraft.massivecore.gson;

import java.lang.reflect.Type;

/**
 * Callback passed to JsonSerializer implementations so they can serialize nested values without
 * constructing a Gson instance. Wraps Gson's native serialization context; results are MassiveCore
 * JsonElement wrappers.
 */
public interface JsonSerializationContext
{
	/**
	 * Serializes src using Gson's default handling for its runtime class.
	 *
	 * @param src the object to serialize
	 * @return the JSON tree for src, or null if Gson writes null
	 */
	JsonElement serialize(Object src);

	/**
	 * Serializes src using a Class token (e.g. Foo.class). Delegates to the Type overload in bridges.
	 *
	 * @param src          the object to serialize
	 * @param classOfSrc   the type Gson should assume for src
	 * @return the JSON tree for src, or null if Gson writes null
	 */
	JsonElement serialize(Object src, Class<?> classOfSrc);

	/**
	 * Serializes src as the given type (generics and abstract declared types).
	 *
	 * @param src         the object to serialize
	 * @param typeOfSrc   the type Gson should assume for src
	 * @return the JSON tree for src, or null if Gson writes null
	 */
	JsonElement serialize(Object src, Type typeOfSrc);
}
