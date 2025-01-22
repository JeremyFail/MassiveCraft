package com.massivecraft.factions.cmd.type;

import com.massivecraft.factions.entity.Faction;
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

// TODO: This whole thing is a copy/paste of TypeMPermable. Duplicate code.
public class TypeTaxable extends TypeAbstract<String>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static TypeTaxable i = new TypeTaxable();
	public static TypeTaxable get() { return i; }
	private TypeTaxable()
	{
		super(String.class);

		this.faction = null;
	}

	public static TypeTaxable get(Faction faction) { return new TypeTaxable(faction); }
	public TypeTaxable(Faction faction)
	{
		super(String.class);
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
	public String read(String arg, CommandSender sender) throws MassiveException
	{
		if (arg.toLowerCase().startsWith("rank-"))
		{
			String subArg = arg.substring("rank-".length());
			return new TypeRank(this.getFaction()).read(subArg, sender).getId();
		}

		if (arg.toLowerCase().startsWith("player-"))
		{
			String subArg = arg.substring("player-".length());
			return TypeMPlayer.get().read(subArg, sender).getId();
		}


		TypeRank typeRank = new TypeRank(this.getFaction());
		try
		{
			return typeRank.read(arg, sender).getId();
		}
		catch (MassiveException ex)
		{
			// Do nothing
		}

		try
		{
			return TypeMPlayer.get().read(arg, sender).getId();
		}
		catch (MassiveException ex)
		{
			// Do nothing
		}

		throw new MassiveException().addMsg("<b>No rank or player matches: <h>%s<b>.", arg);
	}

	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		List<String> ret = new MassiveList<>();
		Faction faction = this.getFaction();
		if (faction == null) faction = MPlayer.get(sender).getFaction();

		// Always add ranks, relations, other factions and other players
		ret.addAll(faction.getRanks().getAll().stream().map(Rank::getName).collect(Collectors.toList()));
		ret.addAll(TypeMPlayer.get().getTabList(sender, arg));

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
			ret.addAll(MUtil.list("rank-", "player-"));
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
