package com.massivecraft.massivecore.command.type;

import com.massivecraft.massivecore.command.type.enumeration.TypeMaterial;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

public class TypeMaterialKey extends TypeTransformer<Material, String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final TypeMaterialKey i = new TypeMaterialKey();
	public static TypeMaterialKey get() { return i; }
	
	public TypeMaterialKey()
	{
		super(TypeMaterial.get(), TypeString.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public String innerToOuter(Material inner, CommandSender sender)
	{
		if (inner == null) return null;
		return inner.name();
	}

	@Override
	public Material outerToInner(String outer)
	{
		if (outer == null) return null;
		return Material.getMaterial(outer.toUpperCase());
	}
	
}
