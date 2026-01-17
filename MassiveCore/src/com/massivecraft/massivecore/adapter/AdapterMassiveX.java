package com.massivecraft.massivecore.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.massivecraft.massivecore.collections.Def;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * This is the abstract adapter for all "Massive structures".
 * It makes sure Def instances "handle empty as null".
 * It makes sure we avoid infinite GSON recurse loops by recursing with supertype.
 */
public abstract class AdapterMassiveX<T> implements JsonDeserializer<T>, JsonSerializer<T>
{
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public JsonElement serialize(T src, Type type, JsonSerializationContext context)
	{
		ParameterizedType ptype = (ParameterizedType) type;
		
		// Calculate def
		Class<?> clazz = getClazz(ptype);
		boolean def = Def.class.isAssignableFrom(clazz);
		
		// If this is a Def ...
		if (def)
		{
			// ... and the instance is null or contains no elements ...
			if (isEmpty(src))
			{
				// ... then serialize as a JsonNull!
				return JsonNull.INSTANCE;
			}
			// ... and it's non null and contains something ...
			else
			{
				// ... then serialize it as if it were the regular Java collection!
				// SUPER TYPE x2 EXAMPLE: MassiveListDef --> MassiveList --> ArrayList
				return context.serialize(src, getSuperType(getSuperType(ptype)));
			}
		}
		// If this a regular Massive structure and not a Def ...
		else
		{
			// ... then serialize it as if it were the regular java collection!
			// SUPER TYPE x1 EXAMPLE: MassiveList --> ArrayList
			return context.serialize(src, getSuperType(ptype));
		}
	}
	
	@Override
	public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException
	{
		ParameterizedType ptype = (ParameterizedType) type;
		
		/*// TODO: Temporary Debug
		if (MUtil.getStackTraceString().contains("com.massivecraft.factions.entity.FactionColl.init"))
		{
			typeDebug(ptype);
			typeDebug(getSuperType(ptype));
			typeDebug(getSuperType(getSuperType(ptype)));
		}*/
		
		// Calculate def
		Class<?> clazz = getClazz(ptype);
		boolean def = Def.class.isAssignableFrom(clazz);
		
		// If this is a Def ...
		if (def)
		{
			// ... then deserialize it as if it were the regular Java collection!
			// SUPER TYPE x2 EXAMPLE: MassiveListDef --> MassiveList --> ArrayList
			Object parent = context.deserialize(json, getSuperType(getSuperType(ptype)));
			return create(parent, def, json, type, context);
		}
		// If this a regular Massive structure and not a Def ...
		else
		{
			// ... and the json is null or a JsonNull ...
			if (json == null || json instanceof JsonNull)
			{
				// ... then deserialize as a null!
				return null;
			}
			// ... and it's non null and contains something ...
			else
			{
				// ... then deserialize it as if it were the regular java collection!
				// SUPER TYPE x1 EXAMPLE: MassiveList --> ArrayList
				Object parent = context.deserialize(json, getSuperType(ptype));
				return create(parent, def, json, type, context);
			}
		}
	}
	
	/*
	public static void typeDebug(ParameterizedType ptype)
	{
		System.out.println("=== Type Debug Start ===");
		
		System.out.println(ptype.toString());
		
		ParameterizedType parameterizedType = (ParameterizedType) ptype;
		System.out.println("Actual Type Arguments: " + Txt.implode(parameterizedType.getActualTypeArguments(), ", "));
		
		System.out.println("=== Type Debug End ===");
	}*/
	
	// -------------------------------------------- //
	// ABSTRACT
	// -------------------------------------------- //
	
	public abstract T create(Object parent, boolean def, JsonElement json, Type typeOfT, JsonDeserializationContext context);
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	/**
	 * Custom implementation of ParameterizedType to replace Gson's internal $Gson$Types.
	 * 
	 * <p>This class provides a concrete implementation of Java's {@link ParameterizedType} interface,
	 * which represents generic types with actual type arguments (e.g., {@code List<String>}, {@code Map<String, Integer>}).
	 * 
	 * <p><b>Why this exists:</b><br>
	 * Previously, plugins could access Gson's internal {@code $Gson$Types} utility class to construct
	 * parameterized types at runtime. However, updates to Gson and Paper's newer classloader isolation prevents access to Gson's internal
	 * classes, requiring us to implement our own type construction mechanism.
	 * 
	 * <p><b>What it does:</b><br>
	 * This class allows {@link #getSuperType(ParameterizedType)} to dynamically construct new parameterized types
	 * during serialization/deserialization. For example, when processing {@code MassiveList<String>}, it can
	 * construct the supertype {@code ArrayList<String>} to pass to Gson for actual processing.
	 * 
	 * <p><b>Implementation notes:</b><br>
	 * The implementation follows the standard contract for {@link ParameterizedType}, including proper
	 * {@link #equals(Object)}, {@link #hashCode()}, and {@link #toString()} methods to ensure type
	 * comparison and debugging work correctly.
	 * 
	 * @see ParameterizedType
	 * @see #getSuperType(ParameterizedType)
	 */
	private static class ParameterizedTypeImpl implements ParameterizedType
	{
		// The owning type (for nested types like Outer.Inner<T>), typically null for our use cases
		private final Type ownerType;
		
		// The raw type (the class without type parameters, e.g., List.class for List<String>)
		private final Type rawType;
		
		// The actual type arguments (e.g., [String.class] for List<String>)
		private final Type[] actualTypeArguments;
		
		/**
		 * Constructs a new parameterized type.
		 * 
		 * @param ownerType The owning type for nested types, or null if not nested
		 * @param rawType The raw class type (e.g., ArrayList.class)
		 * @param actualTypeArguments The type arguments (e.g., String.class for ArrayList<String>)
		 */
		public ParameterizedTypeImpl(Type ownerType, Type rawType, Type... actualTypeArguments)
		{
			this.ownerType = ownerType;
			this.rawType = rawType;
			this.actualTypeArguments = actualTypeArguments;
		}
		
		/**
		 * Returns a copy of the actual type arguments for this parameterized type.
		 * For example, for {@code Map<String, Integer>}, returns [String.class, Integer.class].
		 */
		@Override
		public Type[] getActualTypeArguments()
		{
			return actualTypeArguments.clone();
		}
		
		/**
		 * Returns the raw type for this parameterized type.
		 * For example, for {@code List<String>}, returns List.class.
		 */
		@Override
		public Type getRawType()
		{
			return rawType;
		}
		
		/**
		 * Returns the owner type for nested types, or null if this type is not nested.
		 * For example, for {@code Outer.Inner<String>}, returns Outer.class.
		 */
		@Override
		public Type getOwnerType()
		{
			return ownerType;
		}
		
		/**
		 * Checks equality with another type. Two parameterized types are equal if they have
		 * the same raw type, the same type arguments, and the same owner type.
		 */
		@Override
		public boolean equals(Object other)
		{
			if (!(other instanceof ParameterizedType)) return false;
			ParameterizedType that = (ParameterizedType) other;
			return this.getRawType().equals(that.getRawType())
				&& Arrays.equals(this.getActualTypeArguments(), that.getActualTypeArguments())
				&& (this.getOwnerType() == null ? that.getOwnerType() == null : this.getOwnerType().equals(that.getOwnerType()));
		}
		
		/**
		 * Returns a hash code based on the raw type, type arguments, and owner type.
		 */
		@Override
		public int hashCode()
		{
			return Arrays.hashCode(actualTypeArguments) ^ rawType.hashCode() ^ (ownerType == null ? 0 : ownerType.hashCode());
		}
		
		/**
		 * Returns a string representation of this type in standard Java generic type syntax.
		 * For example: "java.util.ArrayList<java.lang.String>" or "Outer$Inner<T>".
		 */
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			if (ownerType != null)
			{
				sb.append(ownerType.getTypeName()).append("$");
			}
			sb.append(rawType.getTypeName());
			if (actualTypeArguments.length > 0)
			{
				sb.append("<");
				for (int i = 0; i < actualTypeArguments.length; i++)
				{
					if (i > 0) sb.append(", ");
					sb.append(actualTypeArguments[i].getTypeName());
				}
				sb.append(">");
			}
			return sb.toString();
		}
	}
	
	public static Class<?> getClazz(ParameterizedType ptype)
	{
		return (Class<?>)ptype.getRawType();
	}
	
	public static ParameterizedType getSuperType(ParameterizedType ptype)
	{
		// ------- SELF -------
		
		// Get args
		Type[] args = ptype.getActualTypeArguments();
		
		// Get clazz
		Class<?> clazz = (Class<?>)ptype.getRawType();
		
		// ------- SUPER -------
		
		// Get stype
		ParameterizedType sptype = (ParameterizedType) clazz.getGenericSuperclass();
		
		// Get sargs
		// NOTE: These will be broken! we can however look at the count!
		Type[] sargs = sptype.getActualTypeArguments();
		
		// Get sclazz
		Class<?> sclazz = (Class<?>)sptype.getRawType();
		
		// ------- CONSTRUCTED -------
		
		Type[] typeArguments = Arrays.copyOfRange(args, 0, sargs.length);
		
		return new ParameterizedTypeImpl(null, sclazz, typeArguments);
	}
	
	public static Object getNewArgumentInstance(Type type, int index)
	{
		ParameterizedType parameterizedType = (ParameterizedType) type;
		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
		Class<?> clazz = (Class<?>) actualTypeArguments[index];
		try
		{
			return clazz.getDeclaredConstructor().newInstance();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean isEmpty(Object object)
	{
		// A Map is not a Collection.
		// Thus we have to use isEmpty() declared in different interfaces. 
		if (object == null) return true;
		if (object instanceof Map) return ((Map)object).isEmpty();
		if (object instanceof Collection) return ((Collection)object).isEmpty();
		return false;
	}
	
}
