package com.massivecraft.massivecore.gson;

import java.lang.reflect.Type;

/**
 * Internal helpers that convert between MassiveCore's public Gson-facing types and Gson's native
 * {@link com.google.gson} interfaces used inside the shaded delegate.
 * <p>
 * Not part of the supported API for plugins; safe to change between MassiveCore releases.
 * </p>
 */
final class GsonNativeBridge
{
	private GsonNativeBridge()
	{
	}

	/**
	 * Adapts Gson's serialization context so {@link JsonSerializer} implementations see
	 * {@link JsonElement} return types.
	 */
	static final class SerializationContextWrapper implements JsonSerializationContext
	{
		private final com.google.gson.JsonSerializationContext nativeContext;

		/**
		 * @param nativeContext Gson context from the active deserialize/serialize call
		 */
		SerializationContextWrapper(com.google.gson.JsonSerializationContext nativeContext)
		{
			this.nativeContext = nativeContext;
		}

		@Override
		public JsonElement serialize(Object src)
		{
			return JsonElement.wrap(nativeContext.serialize(src));
		}

		@Override
		public JsonElement serialize(Object src, Class<?> classOfSrc)
		{
			return serialize(src, (Type) classOfSrc);
		}

		@Override
		public JsonElement serialize(Object src, Type typeOfSrc)
		{
			return JsonElement.wrap(nativeContext.serialize(src, typeOfSrc));
		}
	}

	/**
	 * Adapts Gson's deserialization context so {@link JsonDeserializer} implementations pass and
	 * receive MassiveCore {@link JsonElement} values.
	 */
	static final class DeserializationContextWrapper implements JsonDeserializationContext
	{
		private final com.google.gson.JsonDeserializationContext nativeContext;

		/**
		 * @param nativeContext Gson context from the active deserialize call
		 */
		DeserializationContextWrapper(com.google.gson.JsonDeserializationContext nativeContext)
		{
			this.nativeContext = nativeContext;
		}

		@Override
		public <T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException
		{
			try
			{
				return nativeContext.deserialize(json == null ? null : json.unwrapNative(), typeOfT);
			}
			catch (com.google.gson.JsonParseException e)
			{
				// Our adapters throw JsonParseException; unwrap would recurse — map to public type.
				throw new JsonParseException(e.getMessage(), e);
			}
		}

		@Override
		public <T> T deserialize(JsonElement json, Class<T> classOfT) throws JsonParseException
		{
			return deserialize(json, (Type) classOfT);
		}
	}

	/**
	 * Wraps a MassiveCore {@link JsonDeserializer} as Gson's native deserializer.
	 *
	 * @param ours plugin-facing deserializer
	 * @return Gson-compatible deserializer (same behavior, wrapped trees)
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static com.google.gson.JsonDeserializer<Object> toNativeDeserializer(JsonDeserializer<?> ours)
	{
		return (json, typeOfT, context) ->
		{
			try
			{
				return ((JsonDeserializer) ours).deserialize(
					JsonElement.wrap(json),
					typeOfT,
					new DeserializationContextWrapper(context));
			}
			catch (JsonParseException e)
			{
				// Gson expects com.google.gson.JsonParseException from native adapters.
				throw new com.google.gson.JsonParseException(e.getMessage(), e);
			}
		};
	}

	/**
	 * Wraps a MassiveCore {@link JsonSerializer} as Gson's native serializer.
	 *
	 * @param ours plugin-facing serializer
	 * @return Gson-compatible serializer
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static com.google.gson.JsonSerializer<Object> toNativeSerializer(JsonSerializer<?> ours)
	{
		return (src, typeOfSrc, context) ->
		{
			JsonElement el = ((JsonSerializer) ours).serialize(src, typeOfSrc, new SerializationContextWrapper(context));
			return el == null ? null : el.unwrapNative();
		};
	}

	/**
	 * Single registration object when the same class implements both {@link JsonSerializer} and
	 * {@link JsonDeserializer} (MassiveCore API).
	 */
	@SuppressWarnings("rawtypes")
	static final class DuplexAdapter implements com.google.gson.JsonSerializer<Object>, com.google.gson.JsonDeserializer<Object>
	{
		private final com.google.gson.JsonDeserializer<Object> nativeDeserializer;
		private final com.google.gson.JsonSerializer<Object> nativeSerializer;

		/**
		 * @param serializer   MassiveCore serializer side
		 * @param deserializer MassiveCore deserializer side (often the same object)
		 */
		DuplexAdapter(JsonSerializer<?> serializer, JsonDeserializer<?> deserializer)
		{
			this.nativeDeserializer = toNativeDeserializer(deserializer);
			this.nativeSerializer = toNativeSerializer(serializer);
		}

		@Override
		public Object deserialize(com.google.gson.JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context)
		{
			return nativeDeserializer.deserialize(json, typeOfT, context);
		}

		@Override
		public com.google.gson.JsonElement serialize(Object src, Type typeOfSrc, com.google.gson.JsonSerializationContext context)
		{
			return nativeSerializer.serialize(src, typeOfSrc, context);
		}
	}

	/**
	 * Maps Gson runtime exceptions to MassiveCore public types where a parallel exists.
	 *
	 * @param e typically {@link com.google.gson.JsonSyntaxException} or related
	 * @return {@link JsonSyntaxException}, {@link JsonIOException}, {@link JsonParseException}, or {@code e} unchanged
	 */
	static RuntimeException mapGsonRuntime(RuntimeException e)
	{
		if (e instanceof com.google.gson.JsonSyntaxException) return new JsonSyntaxException(e.getMessage(), e);
		if (e instanceof com.google.gson.JsonIOException) return new JsonIOException(e.getMessage(), e);
		if (e instanceof com.google.gson.JsonParseException) return new JsonParseException(e.getMessage(), e);
		return e;
	}
}
