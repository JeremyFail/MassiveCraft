package com.massivecraft.factions.cmd.type;

import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.command.type.TypeAbstractChoice;

import java.util.Arrays;
import java.util.Set;

/**
 * Type for primary vs secondary faction color.
 * Accepts: primary, secondary, p, s (case-insensitive).
 * Tab-completes those options.
 */
public class TypeFactionColor extends TypeAbstractChoice<String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static final TypeFactionColor i = new TypeFactionColor();
	public static TypeFactionColor get() { return i; }

	public TypeFactionColor()
	{
		super(String.class);
		this.setAll(Arrays.asList("primary", "secondary"));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public String getName()
	{
		return "primary|secondary";
	}

	@Override
	public String getNameInner(String value)
	{
		return value;
	}

	@Override
	public String getIdInner(String value)
	{
		return value;
	}

	@Override
	public Set<String> getNamesInner(String value)
	{
		if ("primary".equals(value)) return new MassiveSet<>(Arrays.asList("primary", "p"));
		if ("secondary".equals(value)) return new MassiveSet<>(Arrays.asList("secondary", "s"));
		return super.getNamesInner(value);
	}
}
