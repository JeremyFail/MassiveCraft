package com.massivecraft.massivecore.command.type.enumeration;

import com.massivecraft.massivecore.command.type.TypeAbstractChoice;
import com.massivecraft.massivecore.util.Txt;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList; 
import java.util.List;
import org.bukkit.util.OldEnum;

public class TypeOldEnum<T extends OldEnum<T>> extends TypeAbstractChoice<T>
{
	// -------------------------------------------- //
	// FIELD
	// -------------------------------------------- //
	
	protected final Class<T> clazz;
	public Class<T> getClazz() { return this.clazz; }

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public TypeOldEnum(Class<T> clazz)
	{
		super(clazz);
		if (!OldEnum.class.isAssignableFrom(clazz)) 
		{
			throw new IllegalArgumentException("clazz must be enum");
		}
		this.clazz = clazz;
		
		this.setAll(getEnumValues(this.getClazz()));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String getName()
	{
		return Txt.getNicedEnumString(this.getClazz().getSimpleName());
	}
	
	@Override
	public String getNameInner(T value)
	{
		return Txt.getNicedOldEnum(value);
	}

	@Override
	public String getIdInner(T value)
	{
		return value.name();
	}
	
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //
	
	public static <T extends OldEnum<T>> T[] getEnumValues(Class<T> clazz)
	{
		if (clazz == null) throw new IllegalArgumentException("clazz is null");
		if (!OldEnum.class.isAssignableFrom(clazz))
		{
			throw new IllegalArgumentException("clazz must be enum");
		}
		
		T[] ret = getEnumConstants(clazz);
		
		if (ret == null) throw new RuntimeException("failed to retrieve enum constants");
		
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] getEnumConstants(Class<T> clazz)
	{
		List<T> constants = new ArrayList<>();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields)
		{
			if (field.isSynthetic())
			{
				continue;
			}
			if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())
					&& field.getType() == clazz)
			{
				try
				{
					constants.add((T) field.get(null));
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}
		if (constants.isEmpty())
		{
			return null;
		}
		
		T[] array = (T[]) Array.newInstance(clazz, constants.size());
		return constants.toArray(array);
	}
	
}
