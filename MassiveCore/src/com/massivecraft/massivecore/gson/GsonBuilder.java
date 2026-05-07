package com.massivecraft.massivecore.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.Type;

/**
 * Fluent configuration for {@link Gson}. Every setting is forwarded to Gson's
 * {@link com.google.gson.GsonBuilder}; {@link #create()} returns a MassiveCore {@link Gson} wrapper.
 * <p>
 * {@link #registerTypeAdapter(Type, Object)} accepts either native Gson adapters
 * ({@link com.google.gson.JsonSerializer}, {@link com.google.gson.TypeAdapter}, …) or MassiveCore's
 * {@link JsonSerializer} / {@link JsonDeserializer} (including a single object implementing both).
 * </p>
 */
public final class GsonBuilder
{
	private final com.google.gson.GsonBuilder delegate = new com.google.gson.GsonBuilder();

	/**
	 * Creates a builder with Gson defaults.
	 */
	public GsonBuilder()
	{
	}

	/**
	 * Builds an immutable {@link Gson} using the current configuration.
	 *
	 * @return new Gson instance
	 * @see com.google.gson.GsonBuilder#create()
	 */
	public Gson create()
	{
		return new Gson(delegate.create());
	}

	/**
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#disableHtmlEscaping()
	 */
	public GsonBuilder disableHtmlEscaping()
	{
		delegate.disableHtmlEscaping();
		return this;
	}

	/**
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setPrettyPrinting()
	 */
	public GsonBuilder setPrettyPrinting()
	{
		delegate.setPrettyPrinting();
		return this;
	}

	/**
	 * @param modifiers field modifiers to exclude (e.g. {@link java.lang.reflect.Modifier#TRANSIENT})
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#excludeFieldsWithModifiers(int...)
	 */
	public GsonBuilder excludeFieldsWithModifiers(int... modifiers)
	{
		delegate.excludeFieldsWithModifiers(modifiers);
		return this;
	}

	/**
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#serializeNulls()
	 */
	public GsonBuilder serializeNulls()
	{
		delegate.serializeNulls();
		return this;
	}

	/**
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#enableComplexMapKeySerialization()
	 */
	public GsonBuilder enableComplexMapKeySerialization()
	{
		delegate.enableComplexMapKeySerialization();
		return this;
	}

	/**
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#disableJdkUnsafe()
	 */
	public GsonBuilder disableJdkUnsafe()
	{
		delegate.disableJdkUnsafe();
		return this;
	}

	/**
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setLenient()
	 */
	public GsonBuilder setLenient()
	{
		delegate.setLenient();
		return this;
	}

	/**
	 * @param ignoreVersionsAfter Gson @Since threshold
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setVersion(double)
	 */
	public GsonBuilder setVersion(double ignoreVersionsAfter)
	{
		delegate.setVersion(ignoreVersionsAfter);
		return this;
	}

	/**
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#excludeFieldsWithoutExposeAnnotation()
	 */
	public GsonBuilder excludeFieldsWithoutExposeAnnotation()
	{
		delegate.excludeFieldsWithoutExposeAnnotation();
		return this;
	}

	/**
	 * @param namingConvention policy constant
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setFieldNamingPolicy(FieldNamingPolicy)
	 */
	public GsonBuilder setFieldNamingPolicy(FieldNamingPolicy namingConvention)
	{
		delegate.setFieldNamingPolicy(namingConvention);
		return this;
	}

	/**
	 * @param fieldNamingStrategy custom strategy
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setFieldNamingStrategy(FieldNamingStrategy)
	 */
	public GsonBuilder setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy)
	{
		delegate.setFieldNamingStrategy(fieldNamingStrategy);
		return this;
	}

	/**
	 * @param serializationPolicy how longs are written
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setLongSerializationPolicy(LongSerializationPolicy)
	 */
	public GsonBuilder setLongSerializationPolicy(LongSerializationPolicy serializationPolicy)
	{
		delegate.setLongSerializationPolicy(serializationPolicy);
		return this;
	}

	/**
	 * @param pattern date format pattern
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setDateFormat(String)
	 */
	public GsonBuilder setDateFormat(String pattern)
	{
		delegate.setDateFormat(pattern);
		return this;
	}

	/**
	 * @param style {@link java.text.DateFormat} style
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setDateFormat(int)
	 */
	public GsonBuilder setDateFormat(int style)
	{
		delegate.setDateFormat(style);
		return this;
	}

	/**
	 * @param dateStyle date portion style
	 * @param timeStyle time portion style
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setDateFormat(int, int)
	 */
	public GsonBuilder setDateFormat(int dateStyle, int timeStyle)
	{
		delegate.setDateFormat(dateStyle, timeStyle);
		return this;
	}

	/**
	 * @param strategies exclusion strategies to apply
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#setExclusionStrategies(ExclusionStrategy...)
	 */
	public GsonBuilder setExclusionStrategies(ExclusionStrategy... strategies)
	{
		delegate.setExclusionStrategies(strategies);
		return this;
	}

	/**
	 * @param strategy additional serialization exclusion
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#addSerializationExclusionStrategy(ExclusionStrategy)
	 */
	public GsonBuilder addSerializationExclusionStrategy(ExclusionStrategy strategy)
	{
		delegate.addSerializationExclusionStrategy(strategy);
		return this;
	}

	/**
	 * @param strategy additional deserialization exclusion
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#addDeserializationExclusionStrategy(ExclusionStrategy)
	 */
	public GsonBuilder addDeserializationExclusionStrategy(ExclusionStrategy strategy)
	{
		delegate.addDeserializationExclusionStrategy(strategy);
		return this;
	}

	/**
	 * @param factory type adapter factory (native Gson type)
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#registerTypeAdapterFactory(TypeAdapterFactory)
	 */
	public GsonBuilder registerTypeAdapterFactory(TypeAdapterFactory factory)
	{
		delegate.registerTypeAdapterFactory(factory);
		return this;
	}

	/**
	 * @param baseType   base class or interface
	 * @param typeAdapter native Gson hierarchy adapter
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#registerTypeHierarchyAdapter(Class, Object)
	 */
	public GsonBuilder registerTypeHierarchyAdapter(Class<?> baseType, Object typeAdapter)
	{
		delegate.registerTypeHierarchyAdapter(baseType, typeAdapter);
		return this;
	}

	/**
	 * Registers a type adapter for {@code type}. Accepts:
	 * <ul>
	 *   <li>Gson types: {@link com.google.gson.JsonSerializer}, {@link com.google.gson.JsonDeserializer},
	 *       {@link com.google.gson.TypeAdapter}, {@link com.google.gson.InstanceCreator}</li>
	 *   <li>MassiveCore types: {@link JsonSerializer}, {@link JsonDeserializer}, or one object implementing both</li>
	 *   <li>any other object forwarded as-is to Gson (if supported)</li>
	 * </ul>
	 *
	 * @param type        the type the adapter handles
	 * @param typeAdapter the adapter instance
	 * @return {@code this}
	 * @see com.google.gson.GsonBuilder#registerTypeAdapter(Type, Object)
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public GsonBuilder registerTypeAdapter(Type type, Object typeAdapter)
	{
		if (typeAdapter instanceof com.google.gson.JsonSerializer
			|| typeAdapter instanceof com.google.gson.JsonDeserializer
			|| typeAdapter instanceof com.google.gson.TypeAdapter
			|| typeAdapter instanceof com.google.gson.InstanceCreator)
		{
			delegate.registerTypeAdapter(type, typeAdapter);
			return this;
		}

		boolean isOurSer = typeAdapter instanceof JsonSerializer;
		boolean isOurDe = typeAdapter instanceof JsonDeserializer;

		if (isOurSer && isOurDe)
		{
			delegate.registerTypeAdapter(type, new GsonNativeBridge.DuplexAdapter((JsonSerializer) typeAdapter, (JsonDeserializer) typeAdapter));
		}
		else if (isOurDe)
		{
			delegate.registerTypeAdapter(type, GsonNativeBridge.toNativeDeserializer((JsonDeserializer) typeAdapter));
		}
		else if (isOurSer)
		{
			delegate.registerTypeAdapter(type, GsonNativeBridge.toNativeSerializer((JsonSerializer) typeAdapter));
		}
		else
		{
			// Last resort: let Gson validate (may throw if unsupported).
			delegate.registerTypeAdapter(type, typeAdapter);
		}
		return this;
	}
}
