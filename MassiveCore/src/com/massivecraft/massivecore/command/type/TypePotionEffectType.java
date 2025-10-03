package com.massivecraft.massivecore.command.type;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

public class TypePotionEffectType extends TypeAbstractChoice<PotionEffectType>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TypePotionEffectType i = new TypePotionEffectType();
	public static TypePotionEffectType get() { return i; }
	public TypePotionEffectType()
	{
		super(PotionEffectType.class);

		List<PotionEffectType> potionEffects = new ArrayList<>();
		Registry.EFFECT.iterator().forEachRemaining(potionEffects::add);
		this.setAll(potionEffects);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public String getNameInner(PotionEffectType value)
	{
		NamespacedKey key = value.getKey();
		return key != null ? key.getKey() : null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public String getIdInner(PotionEffectType value)
	{
		return String.valueOf(value.getId());
	}
	
}
