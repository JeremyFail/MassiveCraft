package com.massivecraft.factions.cmd.type;

import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.WildTpPredefinedLocation;
import com.massivecraft.massivecore.command.type.TypeAbstractChoice;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Command type for configured {@link WildTpPredefinedLocation} entries by name.
 * Provides tab-completion of existing location names.
 */
public class TypeWildTpPredefinedLocation extends TypeAbstractChoice<WildTpPredefinedLocation>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static final TypeWildTpPredefinedLocation i = new TypeWildTpPredefinedLocation();
	public static TypeWildTpPredefinedLocation get() { return i; }

	public TypeWildTpPredefinedLocation()
	{
		super(WildTpPredefinedLocation.class);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public String getName()
	{
		return "wildtp location";
	}

	@Override
	public String getNameInner(WildTpPredefinedLocation value)
	{
		return value.name;
	}

	@Override
	public String getIdInner(WildTpPredefinedLocation value)
	{
		return value.name;
	}

	@Override
	public Collection<WildTpPredefinedLocation> getAll()
	{
		List<WildTpPredefinedLocation> ret = new ArrayList<>();
		List<WildTpPredefinedLocation> configured = MConf.get().wildTpPredefinedLocations;
		if (configured == null) return ret;
		for (WildTpPredefinedLocation loc : configured)
		{
			if (loc == null || loc.name == null || loc.name.isEmpty()) continue;
			ret.add(loc);
		}
		return ret;
	}

	@Override
	public String getVisualInner(WildTpPredefinedLocation value, CommandSender sender)
	{
		return value.name;
	}

}
