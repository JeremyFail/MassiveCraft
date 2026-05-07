package com.massivecraft.massivecore.gson.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Captures a generic {@link Type} for use with {@link com.massivecraft.massivecore.gson.Gson}
 * (e.g. {@code Set<String>}, {@code Map<K,V>}) without passing raw {@link Class} objects.
 * <p>
 * Usage matches Gson's {@link com.google.gson.reflect.TypeToken}:
 * {@code Type t = new TypeToken<List<String>>(){}.getType();}
 * </p>
 *
 * @param <T> a phantom type parameter; the actual type comes from the anonymous subclass
 */
@SuppressWarnings("unused")
public class TypeToken<T>
{
	private final Type type;

	/**
	 * Subclasses must use the empty anonymous form {@code new TypeToken<...>() {}} so the generic
	 * supertype can be read via reflection.
	 *
	 * @throws IllegalArgumentException if the direct superclass is not a parameterized {@link TypeToken}
	 */
	protected TypeToken()
	{
		Type superclass = getClass().getGenericSuperclass();
		if (!(superclass instanceof ParameterizedType))
		{
			throw new IllegalArgumentException("TypeToken must be created with a type parameter: new TypeToken<...>() {}");
		}
		this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
	}

	/**
	 * Internal: builds a token for an already-resolved type (used by {@link #getParameterized}).
	 *
	 * @param type the resolved type (must not be {@code null})
	 */
	private TypeToken(Type type)
	{
		this.type = type;
	}

	/**
	 * @return the captured {@link Type} (includes generic arguments)
	 */
	public Type getType()
	{
		return type;
	}

	/**
	 * Builds a type representing {@code rawType} parameterized with {@code typeArguments}
	 * (e.g. {@code Map.class}, {@code String.class}, {@code Integer.class}).
	 *
	 * @param rawType       the raw class (e.g. {@link java.util.Map})
	 * @param typeArguments type arguments in order
	 * @return a token whose {@link #getType()} is the parameterized type
	 * @see com.google.gson.reflect.TypeToken#getParameterized(Type, Type...)
	 */
	public static TypeToken<?> getParameterized(Type rawType, Type... typeArguments)
	{
		Type t = com.google.gson.reflect.TypeToken.getParameterized(rawType, typeArguments).getType();
		return new TypeToken<Object>(t) {};
	}
}
