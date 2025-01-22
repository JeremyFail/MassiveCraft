package com.massivecraft.factions.cmd.type;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.type.TypeAbstract;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TypeMPermable extends TypeAbstract<MPerm.MPermable>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static TypeMPermable i = new TypeMPermable();
	public static TypeMPermable get() { return i; }
	private TypeMPermable()
	{
		super(Rank.class);

		this.faction = null;
	}

	public static TypeMPermable get(Faction faction) { return new TypeMPermable(faction); }
	public TypeMPermable(Faction faction)
	{
		super(MPerm.MPermable.class);
		if (faction == null) throw new NullPointerException("faction");

		this.faction = faction;
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private final Faction faction;
	public Faction getFaction() { return this.faction; }

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public MPerm.MPermable read(String arg, CommandSender sender) throws MassiveException
	{
		if (arg.toLowerCase().startsWith("rank-"))
		{
			String subArg = arg.substring("rank-".length());
			return new TypeRank(this.getFaction()).read(subArg, sender);
		}

		if (arg.toLowerCase().startsWith("relation-"))
		{
			String subArg = arg.substring("relation-".length());
			return TypeRelation.get().read(subArg, sender);
		}

		if (arg.toLowerCase().startsWith("player-"))
		{
			String subArg = arg.substring("player-".length());
			return TypeMPlayer.get().read(subArg, sender);
		}

		if (arg.toLowerCase().startsWith("faction-"))
		{
			String subArg = arg.substring("faction-".length());
			return TypeFaction.get().read(subArg, sender);
		}

		TypeRank typeRank = new TypeRank(this.getFaction());
		try
		{
			return typeRank.read(arg, sender);
		}
		catch (MassiveException ex)
		{
			// Do nothing
		}

		try
		{
			return TypeRelation.get().read(arg, sender);
		}
		catch (MassiveException ex)
		{
			// Do nothing
		}

		try
		{
			return TypeMPlayer.get().read(arg, sender);
		}
		catch (MassiveException ex)
		{
			// Do nothing
		}

		try
		{
			return TypeFaction.get().read(arg, sender);
		}
		catch (MassiveException ex)
		{
			// Do nothing
		}

		if (arg.contains("-"))
		{
			int idx = arg.indexOf('-');
			String factionName = arg.substring(0, idx);
			String rankName = arg.substring(idx+1);

			Faction faction = TypeFaction.get().read(factionName, sender);
			return TypeRank.get(faction).read(rankName, sender);
		}

		throw new MassiveException().addMsg("<b>No rank, relation, player or faction matches: <h>%s<b>.", arg);
	}

	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		List<String> ret = new MassiveList<>();
		Faction faction = this.getFaction();
		if (faction == null) faction = MPlayer.get(sender).getFaction();

		// Always add ranks, relations, other factions and other players
		ret.addAll(faction.getRanks().getAll().stream().map(Rank::getName).collect(Collectors.toList()));
		ret.addAll(TypeRelation.get().getTabList(sender, arg));
		ret.addAll(TypeFaction.get().getTabList(sender, arg));
		ret.addAll(TypeMPlayer.get().getTabList(sender, arg));

		// If something has been specified, then suggest factin specific ranks
		if (arg.length() >= 2)
		{
			for (Faction f : FactionColl.get().getAll())
			{
				String name = f.getName();
				if (arg.length() <= name.length() && !name.toLowerCase().startsWith(arg.toLowerCase())) continue;
				if (arg.length() > name.length() && !arg.toLowerCase().startsWith(name.toLowerCase())) continue;

				ret.addAll(f.getRanks().getAll().stream().map(r -> name + "-" + r.getName()).collect(Collectors.toList()));
			}
		}

		// Also add the cases for when type is specified
		if (arg.length() >= 2)
		{
			String compArg = arg.toLowerCase();
			if (compArg.startsWith("rank-") || "rank-".startsWith(compArg))
			{
				ret.addAll(faction.getRanks().getAll().stream()
							   .map(Rank::getName)
							   .map(n -> "rank-" + n)
							   .collect(Collectors.toList()));
			}
			if (compArg.startsWith("relation-") || "relation-".startsWith(compArg))
			{
				ret.addAll(TypeRelation.get().getTabList(sender, arg).stream()
							   .map(s -> "relation-" + s)
							   .collect(Collectors.toList()));
			}
			if (compArg.startsWith("faction-") || "faction-".startsWith(compArg))
			{
				ret.addAll(TypeFaction.get().getTabList(sender, arg).stream()
							   .map(s -> "faction-" + s)
							   .collect(Collectors.toList()));
			}
			if (compArg.startsWith("player-") || "player-".startsWith(compArg))
			{
				ret.addAll(TypeMPlayer.get().getTabList(sender, arg).stream()
							   .map(s -> "player-" + s)
							   .collect(Collectors.toList()));
			}
		}
		else
		{
			// Or at least add the beginning
			ret.addAll(MUtil.list("rank-", "relation-", "faction-", "player-"));
		}

		return ret;
	}


	@Override
	public boolean isValid(String arg, CommandSender sender)
	{
		// In the generic case accept all
		if (this.getFaction() == null) return true;
		else return super.isValid(arg, sender);
	}

}
