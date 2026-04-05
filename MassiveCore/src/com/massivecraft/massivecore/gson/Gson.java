package com.massivecraft.massivecore.gson;

import java.io.Reader;
import java.lang.reflect.Type;

/**
 * JSON serializer/deserializer for MassiveCore and dependent plugins.
 * <p>
 * Instances delegate to Gson bundled with MassiveCore (relocated in the packaged JAR). Public
 * methods throw MassiveCore {@link JsonSyntaxException} / {@link JsonParseException} where Gson
 * would throw shaded {@code com.google.gson} types, so callers never catch implementation types.
 * </p>
 * <p>
 * When deserializing to {@link JsonObject}, {@link JsonArray}, {@link JsonPrimitive}, {@link JsonNull},
 * or {@link JsonElement}, this class unwraps Gson's native tree into MassiveCore wrappers
 * automatically (see private {@code fromJson*Typed} helpers).
 * </p>
 */
public final class Gson
{
	private final com.google.gson.Gson delegate;

	/**
	 * Creates a Gson instance with default configuration (same as {@code new com.google.gson.Gson()}).
	 */
	public Gson()
	{
		this(new com.google.gson.Gson());
	}

	/**
	 * Package-private: wraps an already-built native Gson (used by {@link GsonBuilder#create()}).
	 *
	 * @param delegate configured Gson instance (must not be {@code null})
	 */
	Gson(com.google.gson.Gson delegate)
	{
		this.delegate = delegate;
	}

	/**
	 * For MassiveCore internal use only (e.g. advanced integration with native TypeAdapters).
	 *
	 * @return the underlying Gson instance
	 */
	com.google.gson.Gson unwrapDelegate()
	{
		return delegate;
	}

	// -------------------------------------------- //
	// toJson
	// -------------------------------------------- //

	/**
	 * Serializes {@code src} to JSON text using the runtime class of {@code src}.
	 *
	 * @param src the object to serialize (may be {@code null} per Gson rules)
	 * @return JSON string
	 * @throws JsonIOException if Gson fails while writing
	 */
	public String toJson(Object src)
	{
		try
		{
			return delegate.toJson(src);
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	/**
	 * Serializes {@code src} using the given type (needed for generics and polymorphic fields).
	 *
	 * @param src        the object to serialize
	 * @param typeOfSrc  declared type to use for adapter lookup
	 * @return JSON string
	 * @throws JsonIOException if Gson fails while writing
	 */
	public String toJson(Object src, Type typeOfSrc)
	{
		try
		{
			return delegate.toJson(src, typeOfSrc);
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	/**
	 * Serializes an already-built JSON tree to text.
	 *
	 * @param jsonElement root of the tree, or {@code null}
	 * @return JSON string
	 * @throws JsonIOException if Gson fails while writing
	 */
	public String toJson(JsonElement jsonElement)
	{
		try
		{
			return delegate.toJson(jsonElement == null ? null : jsonElement.unwrapNative());
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	// -------------------------------------------- //
	// toJsonTree
	// -------------------------------------------- //

	/**
	 * Converts a Java object to a {@link JsonElement} tree.
	 *
	 * @param src the object to convert
	 * @return wrapped JSON tree root
	 * @throws JsonIOException if Gson fails while building the tree
	 */
	public JsonElement toJsonTree(Object src)
	{
		try
		{
			return JsonElement.wrap(delegate.toJsonTree(src));
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	/**
	 * Converts {@code src} to a tree using {@code typeOfSrc} for adapter resolution.
	 *
	 * @param src        the object to convert
	 * @param typeOfSrc  type to assume for {@code src}
	 * @return wrapped JSON tree root
	 * @throws JsonIOException if Gson fails while building the tree
	 */
	public JsonElement toJsonTree(Object src, Type typeOfSrc)
	{
		try
		{
			return JsonElement.wrap(delegate.toJsonTree(src, typeOfSrc));
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	// -------------------------------------------- //
	// fromJson String
	// -------------------------------------------- //

	/**
	 * Parses JSON and deserializes to {@code classOfT}. When {@code classOfT} is a MassiveCore
	 * JSON tree type, returns the corresponding wrapper.
	 *
	 * @param json      JSON text (may be {@code null} per Gson null-handling)
	 * @param classOfT  target class
	 * @param <T>       result type
	 * @return deserialized object, or {@code null} for JSON null / Gson rules
	 * @throws JsonSyntaxException if JSON is invalid or incompatible with {@code classOfT}
	 */
	public <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException
	{
		try
		{
			return fromJsonTyped(json, classOfT);
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	/**
	 * Parses JSON into an instance of {@code typeOfT} (use {@link com.massivecraft.massivecore.gson.reflect.TypeToken} for generics).
	 * Separate from {@link #fromJson(String, Class)} so tooling that applies stricter generic-erasure checks does not treat the overloads as duplicates.
	 *
	 * @param json      JSON text
	 * @param typeOfT   full type token
	 * @param <T>       result type (caller responsibility to match {@code typeOfT})
	 * @return deserialized value
	 * @throws JsonSyntaxException if parsing fails
	 */
	public <T> T fromJsonType(String json, Type typeOfT) throws JsonSyntaxException
	{
		try
		{
			return delegate.fromJson(json, typeOfT);
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	/**
	 * Reads JSON from a character stream into {@code classOfT}.
	 *
	 * @param json      reader over JSON text (must not be {@code null})
	 * @param classOfT  target class
	 * @param <T>       result type
	 * @return deserialized object
	 * @throws JsonSyntaxException if JSON is invalid
	 */
	public <T> T fromJson(Reader json, Class<T> classOfT) throws JsonSyntaxException
	{
		try
		{
			return fromJsonReaderTyped(json, classOfT);
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	/**
	 * Reads JSON from a character stream into {@code typeOfT}.
	 *
	 * @param json     reader over JSON text
	 * @param typeOfT  target type
	 * @param <T>      result type
	 * @return deserialized value
	 * @throws JsonSyntaxException if JSON is invalid
	 * @see #fromJsonType(String, Type)
	 */
	public <T> T fromJsonType(Reader json, Type typeOfT) throws JsonSyntaxException
	{
		try
		{
			return delegate.fromJson(json, typeOfT);
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	// -------------------------------------------- //
	// fromJson JsonElement
	// -------------------------------------------- //

	/**
	 * Deserializes a JSON subtree into {@code classOfT}, with the same tree-type wrapping rules as
	 * {@link #fromJson(String, Class)}.
	 *
	 * @param json      root of subtree, or {@code null}
	 * @param classOfT  target class
	 * @param <T>       result type
	 * @return deserialized object
	 * @throws JsonSyntaxException if data is incompatible with {@code classOfT}
	 */
	public <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException
	{
		try
		{
			return fromJsonElementTyped(json, classOfT);
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	/**
	 * Deserializes a JSON subtree using a full {@link Type}.
	 *
	 * @param json     root of subtree, or {@code null}
	 * @param typeOfT  target type
	 * @param <T>      result type
	 * @return deserialized value
	 * @throws JsonSyntaxException if data is incompatible
	 * @see #fromJsonType(String, Type)
	 */
	public <T> T fromJsonType(JsonElement json, Type typeOfT) throws JsonSyntaxException
	{
		try
		{
			return delegate.fromJson(json == null ? null : json.unwrapNative(), typeOfT);
		}
		catch (RuntimeException e)
		{
			throw GsonNativeBridge.mapGsonRuntime(e);
		}
	}

	// -------------------------------------------- //
	// Typed helpers (Json* classes vs Gson's after relocation)
	// -------------------------------------------- //

	/**
	 * When {@code classOfT} is {@link JsonObject}, {@link JsonArray}, etc., Gson must deserialize using
	 * its own class object, then we wrap; otherwise delegate normally.
	 */
	@SuppressWarnings("unchecked")
	private <T> T fromJsonTyped(String json, Class<T> classOfT)
	{
		if (classOfT == JsonObject.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonObject.class));
		if (classOfT == JsonArray.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonArray.class));
		if (classOfT == JsonPrimitive.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonPrimitive.class));
		if (classOfT == JsonNull.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonNull.class));
		if (classOfT == JsonElement.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonElement.class));
		return delegate.fromJson(json, classOfT);
	}

	/** Same as {@link #fromJsonTyped(String, Class)} but reading from a {@link Reader}. */
	@SuppressWarnings("unchecked")
	private <T> T fromJsonReaderTyped(Reader json, Class<T> classOfT)
	{
		if (classOfT == JsonObject.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonObject.class));
		if (classOfT == JsonArray.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonArray.class));
		if (classOfT == JsonPrimitive.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonPrimitive.class));
		if (classOfT == JsonNull.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonNull.class));
		if (classOfT == JsonElement.class)
			return (T) JsonElement.wrap(delegate.fromJson(json, com.google.gson.JsonElement.class));
		return delegate.fromJson(json, classOfT);
	}

	/** Same as {@link #fromJsonTyped(String, Class)} but starting from an existing tree. */
	@SuppressWarnings("unchecked")
	private <T> T fromJsonElementTyped(JsonElement json, Class<T> classOfT)
	{
		com.google.gson.JsonElement nativeEl = json == null ? null : json.unwrapNative();
		if (classOfT == JsonObject.class) return (T) JsonElement.wrap(delegate.fromJson(nativeEl, com.google.gson.JsonObject.class));
		if (classOfT == JsonArray.class) return (T) JsonElement.wrap(delegate.fromJson(nativeEl, com.google.gson.JsonArray.class));
		if (classOfT == JsonPrimitive.class) return (T) JsonElement.wrap(delegate.fromJson(nativeEl, com.google.gson.JsonPrimitive.class));
		if (classOfT == JsonNull.class) return (T) JsonElement.wrap(delegate.fromJson(nativeEl, com.google.gson.JsonNull.class));
		if (classOfT == JsonElement.class) return (T) JsonElement.wrap(delegate.fromJson(nativeEl, com.google.gson.JsonElement.class));
		return delegate.fromJson(nativeEl, classOfT);
	}
}
