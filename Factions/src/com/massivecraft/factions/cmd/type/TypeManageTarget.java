package com.massivecraft.factions.cmd.type;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.type.TypeAbstract;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Type for the first argument of /f perm manage: "ranks", "relationships" (and aliases),
 * or any specific rank/relation/faction/player (same as TypeMPermable tab list + general options).
 */
public class TypeManageTarget extends TypeAbstract<String>
{
	private static final List<String> GENERAL_OPTIONS = Arrays.asList("ranks", "relationships", "relations", "relation", "rels", "rel");

	private static TypeManageTarget i = new TypeManageTarget();
	public static TypeManageTarget get() { return i; }

	private TypeManageTarget()
	{
		super(String.class);
	}

	@Override
	public String getName()
	{
		return "rank/relation/faction/player";
	}

	@Override
	public String read(String arg, CommandSender sender) throws MassiveException
	{
		return arg;
	}

	@Override
	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		List<String> ret = new MassiveList<>();
		Faction faction = MPlayer.get(sender).getFaction();

		// General options (multi-column views)
		ret.addAll(GENERAL_OPTIONS);

		// Same tab list as /f perm set entity (ranks, relations, factions, players, prefixes)
		if (faction != null && !faction.isNone())
		{
			ret.addAll(TypeMPermable.get(faction).getTabList(sender, arg));
		}

		return ret;
	}
}
