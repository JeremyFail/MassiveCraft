package com.massivecraft.massivecore.command.type.convert;

import com.massivecraft.massivecore.command.type.TypeEnchantment;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.item.ConverterFromEnchant;
import com.massivecraft.massivecore.item.ConverterToEnchant;
import org.bukkit.enchantments.Enchantment;

public class TypeConverterEnchant extends TypeConverter<Enchantment, String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final TypeConverterEnchant i = new TypeConverterEnchant();
	public static TypeConverterEnchant get() { return i; }
	
	public TypeConverterEnchant()
	{
		super(TypeEnchantment.get(), TypeString.get(), ConverterFromEnchant.get(), ConverterToEnchant.get());
	}
	
}
