package com.massivecraft.creativegates.cmd.type;

import com.massivecraft.creativegates.entity.GateTeamRank;
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
 * Tab suggestions for {@code /cg perm}: ranks and online faction/team members.
 */
public class TypeFactionPermTarget extends TypeAbstract<String>
{
	private static final TypeFactionPermTarget i = new TypeFactionPermTarget();
	public static TypeFactionPermTarget get() { return i; }
	
	private TypeFactionPermTarget()
	{
		super(String.class);
	}
	
	@Override
	public String read(String arg, CommandSender sender) throws MassiveException
	{
		if (arg == null || arg.isEmpty()) throw new MassiveException().addMsg("<b>Rank or player required.");
		return arg;
	}
	
	@Override
	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		MassiveList<String> ret = new MassiveList<>();
		for (GateTeamRank rank : GateTeamRank.values())
		{
			ret.add(rank.name());
		}
		
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		String teamId = null;
		if (bridge != null && bridge.isAvailable() && sender instanceof Player)
		{
			teamId = bridge.getTeamId((Player) sender);
		}
		
		for (Player player : Bukkit.getOnlinePlayers())
		{
			if (player == null) continue;
			if (teamId != null && bridge != null)
			{
				String otherTeam = bridge.getTeamId(player);
				if (!teamId.equals(otherTeam)) continue;
			}
			ret.add(player.getName());
		}
		return ret;
	}
}
