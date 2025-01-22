package com.massivecraft.factions.cmd.type;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveSet;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;

import java.util.Set;

public class TypeRank extends TypeEntityInternalFaction<Rank>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static TypeRank i = new TypeRank();
	public static TypeRank get() { return i; }
	private TypeRank()
	{
		super(Rank.class);
		this.currentRank = null;
	}

	@Deprecated public static TypeRank get(Faction faction) { return new TypeRank(faction, null); }
	@Deprecated public TypeRank(Faction faction)
	{
		this(faction, null);
	}

	public static TypeRank get(Faction faction, Rank rank) { return new TypeRank(faction, rank); }
	public TypeRank(Faction faction, Rank rank)
	{
		super(Rank.class, faction);
		this.currentRank = rank;

		// When setAll is done in the super constructor some optimisations are done
		// which don't take the promote/demote thing into account.
		this.setAll(this.getAll(faction));
	}

	private final Rank currentRank;


	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Collection<Rank> getAll(Faction faction)
	{
		return faction.getRanks().getAll();
	}

	@Override
	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		List<String> ret = new MassiveList<>(super.getTabList(sender, arg));
		ret.add("promote");
		ret.add("demote");
		return ret;
	}

	@Override
	public Set<String> getNamesInner(Rank value)
	{
		Set<String> names = new MassiveSet<>();
		names.add(value.getName());

		if (this.currentRank != null)
		{
			// You can't use "promote" to make someone leader.
			Rank promote = getPromote(currentRank);
			if (value == promote && !promote.isLeader()) names.add("promote");

			if (value == getDemote(currentRank)) names.add("demote");
		}

		return names;
	}

	private static Rank getPromote(Rank rank)
	{
		Rank ret = null;
		for (Rank r : rank.getFaction().getRanks().getAll())
		{
			if (rank == r) continue;
			if (rank.isMoreThan(r)) continue;
			if (ret != null && ret.isLessThan(r)) continue;

			ret = r;
		}
		return ret;
	}

	private static Rank getDemote(Rank rank)
	{
		Rank ret = null;
		for (Rank r : rank.getFaction().getRanks().getAll())
		{
			if (rank == r) continue;
			if (rank.isLessThan(r)) continue;
			if (ret != null && ret.isMoreThan(r)) continue;

			ret = r;
		}
		return ret;
	}

}
