package com.massivecraft.creativegates.cmd.type;

import com.massivecraft.creativegates.entity.GateTeamRelation;
import com.massivecraft.creativegates.integration.team.TeamBridge;
import com.massivecraft.creativegates.integration.team.TeamBridgeRegistry;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.type.TypeAbstract;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Tab suggestions for use-access tokens (relations, online players, faction/team names).
 * Free-form input is still accepted ({@code t:} / {@code p:} prefixes).
 */
public class TypeAccessRelation extends TypeAbstract<String>
{
	private static final TypeAccessRelation i = new TypeAccessRelation();
	public static TypeAccessRelation get() { return i; }
	
	private TypeAccessRelation()
	{
		super(String.class);
	}
	
	@Override
	public String read(String arg, CommandSender sender) throws MassiveException
	{
		if (arg == null || arg.isEmpty()) throw new MassiveException().addMsg("<b>Relation required.");
		return arg;
	}
	
	@Override
	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		MassiveList<String> ret = new MassiveList<>();
		for (GateTeamRelation rel : GateTeamRelation.values())
		{
			ret.add(rel.name());
		}
		for (Player player : Bukkit.getOnlinePlayers())
		{
			if (player != null) ret.add(player.getName());
		}
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		if (bridge != null && bridge.isAvailable())
		{
			for (String name : bridge.getTeamDisplayNames())
			{
				if (name != null && !name.isEmpty()) ret.add(name);
			}
		}
		return ret;
	}
}
