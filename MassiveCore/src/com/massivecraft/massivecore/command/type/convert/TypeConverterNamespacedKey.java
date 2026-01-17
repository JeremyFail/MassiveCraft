package com.massivecraft.massivecore.command.type.convert;

import com.massivecraft.massivecore.command.type.TypeNamespacedKey;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.item.ConverterFromNamespacedKey;
import com.massivecraft.massivecore.item.ConverterToNamespacedKey;
import org.bukkit.NamespacedKey;

public class TypeConverterNamespacedKey extends TypeConverter<NamespacedKey, String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final TypeConverterNamespacedKey i = new TypeConverterNamespacedKey();
	public static TypeConverterNamespacedKey get() { return i; }
	
	public TypeConverterNamespacedKey()
	{
		super(TypeNamespacedKey.get(), TypeString.get(), ConverterFromNamespacedKey.get(), ConverterToNamespacedKey.get());
	}
	
}
