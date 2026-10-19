package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.cmd.requirement.RequirementTeamBridge;
import com.massivecraft.creativegates.cmd.type.TypeFactionPermTarget;
import com.massivecraft.creativegates.entity.GateTeamRank;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UFactionPerms;
import com.massivecraft.creativegates.entity.UFactionPermsColl;
import com.massivecraft.creativegates.integration.team.TeamBridge;
import com.massivecraft.creativegates.integration.team.TeamBridgeRegistry;
import com.massivecraft.creativegates.util.FactionTerm;
import com.massivecraft.creativegates.util.GateFactionPermUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.Visibility;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.enumeration.TypeEnum;
import com.massivecraft.massivecore.util.IdUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Edits per-faction ranks and player allowlist for creating/managing FACTION gates.
 * Only visible/usable when a team bridge is active.
 */
public class CmdCgPerm extends MassiveCommand
{
	public enum Action
	{
		LIST,
		ADD,
		REMOVE,
		RESET,
		;
	}
	
	public CmdCgPerm()
	{
		this.setVisibility(Visibility.SECRET);
		this.addParameter(new TypeEnum<>(Action.class), "list|add|remove|reset");
		this.addParameter(TypeFactionPermTarget.get(), "rank|player", "none");
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG_PERM));
		this.addRequirements(RequirementTeamBridge.get());
	}
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgPerm());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		Action action = this.readArg();
		String target = this.readArg(null);
		
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		String teamId = bridge.getTeamId(this.me);
		if (teamId == null || teamId.isEmpty())
		{
			msg("<b>You must be in a %s.", FactionTerm.get());
			return;
		}
		
		UFactionPerms perms = UFactionPermsColl.get().getOrCreate(teamId);
		if (perms == null)
		{
			msg("<b>Could not load %s gate perms.", FactionTerm.get());
			return;
		}
		
		String teamName = bridge.getTeamDisplayName(teamId);
		if (teamName == null || teamName.isEmpty()) teamName = teamId;
		
		if (action == Action.LIST)
		{
			listPerms(teamName, perms);
			return;
		}
		
		if (!GateFactionPermUtil.canEditFactionPerms(this.me, teamId))
		{
			msg("<b>Only the %s leader can change gate perms.", FactionTerm.get());
			return;
		}
		
		if (action == Action.RESET)
		{
			perms.resetToDefaults();
			msg("<g>%s gate perms reset to defaults for <h>%s<g>.", FactionTerm.capital(), teamName);
			listPerms(teamName, perms);
			return;
		}
		
		if (target == null || target.isEmpty() || target.equalsIgnoreCase("none"))
		{
			msg("<b>Specify a rank (LEADER, OFFICER, MEMBER) or a player name.");
			return;
		}
		
		GateTeamRank rank = GateTeamRank.parse(target);
		if (rank != null)
		{
			if (action == Action.ADD)
			{
				if (perms.addRank(rank))
				{
					msg("<g>Added rank <h>%s<g>.", rank.name());
				}
				else
				{
					msg("<i>That rank is already allowed.");
				}
				return;
			}
			if (action == Action.REMOVE)
			{
				if (perms.removeRank(rank))
				{
					msg("<g>Removed rank <h>%s<g>.", rank.name());
				}
				else
				{
					msg("<i>That rank was not present.");
				}
				return;
			}
		}
		
		String playerId = resolvePlayerId(target);
		if (playerId == null)
		{
			msg("<b>Unknown rank or player <h>%s<b>.", target);
			return;
		}
		String playerName = IdUtil.getName(playerId);
		if (playerName == null || playerName.isEmpty()) playerName = target;
		
		if (action == Action.ADD)
		{
			if (!isMemberOfTeam(bridge, playerId, teamId))
			{
				msg("<b><h>%s <b>must be in your %s to be added.", playerName, FactionTerm.get());
				return;
			}
			if (perms.addPlayer(playerId))
			{
				msg("<g>Added player <h>%s<g> (only while they remain in the %s).", playerName, FactionTerm.get());
			}
			else
			{
				msg("<i>That player is already allowed.");
			}
			return;
		}
		
		if (action == Action.REMOVE)
		{
			if (perms.removePlayer(playerId))
			{
				msg("<g>Removed player <h>%s<g>.", playerName);
			}
			else
			{
				msg("<i>That player was not present.");
			}
			return;
		}
		
		msg("<b>Use list, add, remove, or reset.");
	}
	
	private void listPerms(String teamName, UFactionPerms perms)
	{
		msg("<i>%s gate create/manage for <h>%s<i>:", FactionTerm.capital(), teamName);
		
		Set<String> ranks = perms.getManageRanks();
		msg("<k>Ranks:");
		if (ranks.isEmpty())
		{
			msg("<i>  (empty — treated as LEADER only)");
		}
		else
		{
			List<String> sorted = new ArrayList<>(ranks);
			sorted.sort(String.CASE_INSENSITIVE_ORDER);
			for (String token : sorted)
			{
				msg("<k>- <v>%s", token);
			}
		}
		
		Set<String> players = perms.getManagePlayers();
		msg("<k>Players:");
		if (players.isEmpty())
		{
			msg("<i>  (none)");
		}
		else
		{
			List<String> sorted = new ArrayList<>(players);
			sorted.sort(String.CASE_INSENSITIVE_ORDER);
			for (String id : sorted)
			{
				String name = IdUtil.getName(id);
				msg("<k>- <v>%s", name != null ? name : id);
			}
		}
		msg("<i>Rank matching is at-least. Listed players must stay in the %s.", FactionTerm.get());
	}
	
	private static String resolvePlayerId(String raw)
	{
		if (raw == null || raw.isEmpty()) return null;
		String t = raw.trim();
		if (t.regionMatches(true, 0, UFactionPerms.PLAYER_PREFIX, 0, UFactionPerms.PLAYER_PREFIX.length()))
		{
			t = t.substring(UFactionPerms.PLAYER_PREFIX.length()).trim();
		}
		String id = IdUtil.getId(t);
		if (id == null) return null;
		if (IdUtil.getPlayer(id) != null || IdUtil.getName(id) != null) return id;
		return null;
	}
	
	private static boolean isMemberOfTeam(TeamBridge bridge, String playerId, String teamId)
	{
		if (bridge == null || playerId == null || teamId == null) return false;
		Player online = IdUtil.getPlayer(playerId);
		if (online != null)
		{
			return bridge.getRank(online, teamId) != null;
		}
		// Offline: cannot verify membership via bridge player APIs that need a Player.
		// Still allow remove; for add we require online membership check above when possible.
		// If offline, reject add by returning false (caller requires membership for add).
		return false;
	}
}
