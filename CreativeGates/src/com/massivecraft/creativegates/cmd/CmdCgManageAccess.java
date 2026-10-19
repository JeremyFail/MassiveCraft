package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.cmd.type.TypeAccessAction;
import com.massivecraft.creativegates.cmd.type.TypeAccessRelation;
import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.GateTeamRelation;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.entity.UNetworkAccess;
import com.massivecraft.creativegates.integration.team.TeamBridge;
import com.massivecraft.creativegates.integration.team.TeamBridgeRegistry;
import com.massivecraft.creativegates.util.FactionTerm;
import com.massivecraft.creativegates.util.GateAccessUtil;
import com.massivecraft.creativegates.util.GateLookUtil;
import com.massivecraft.creativegates.util.GateScopeUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.store.TypeEntity;
import com.massivecraft.massivecore.util.IdUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lists and edits network-scoped use-access relations for PLAYER and FACTION gates.
 */
public class CmdCgManageAccess extends MassiveCommand
{
	public CmdCgManageAccess()
	{
		this.addParameter(TypeAccessAction.TYPE, "list|add|remove|reset");
		this.addParameter(TypeAccessRelation.get(), "relation", "none");
		this.addParameter(TypeEntity.get(UGateColl.get()), "gate", "look");
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG_MANAGE));
	}
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgManageAccess());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		TypeAccessAction mode = this.readArg();
		String relationArg = this.readArg(null);
		UGate gate = this.readArg(null);
		if (gate == null) gate = GateLookUtil.getLookedAtGate(this.me);
		if (gate == null)
		{
			msg("<b>You must be looking at a gate.");
			return;
		}
		if (!EngineGateOverride.canManage(this.me, gate))
		{
			msg("<b>You may not manage that gate.");
			return;
		}
		
		GateScopeType scope = gate.getScopeType();
		if (scope != GateScopeType.PLAYER && scope != GateScopeType.FACTION)
		{
			msg("<b>Use-access lists only apply to PLAYER and FACTION networks.");
			return;
		}
		
		UNetworkAccess access = GateAccessUtil.getAccess(gate);
		if (access == null)
		{
			msg("<b>Could not load network access.");
			return;
		}
		
		if (mode == TypeAccessAction.LIST)
		{
			listRelations(gate, access);
			return;
		}
		if (mode == TypeAccessAction.RESET)
		{
			access.resetToDefaults();
			msg("<g>Use-access reset to defaults for this network.");
			listRelations(gate, access);
			return;
		}
		if (mode == TypeAccessAction.ADD || mode == TypeAccessAction.REMOVE)
		{
			if (relationArg == null || relationArg.isEmpty() || relationArg.equalsIgnoreCase("none"))
			{
				msg("<b>Specify a relation, a player name, or %s name.", FactionTerm.get());
				return;
			}
			String token = resolveRelationToken(relationArg, scope);
			if (token == null)
			{
				msg("<b>Unknown relation <h>%s<b>.", relationArg);
				return;
			}
			if (mode == TypeAccessAction.ADD)
			{
				if (access.addRelation(token))
				{
					msg("<g>Added access for <h>%s<g> to network <h>%s<g>.", describeToken(token), gate.getNetworkId());
				}
				else
				{
					msg("<i><h>%s<i> already has access for network <h>%s<i>.", describeToken(token), gate.getNetworkId());
				}
			}
			else
			{
				if (access.removeRelation(token))
				{
					msg("<g>Removed access for <h>%s<g> from network <h>%s<g>.", describeToken(token), gate.getNetworkId());
				}
				else
				{
					msg("<i><h>%s<i> does not have access for network <h>%s<i>.", describeToken(token), gate.getNetworkId());
				}
			}
			return;
		}
		
		msg("<b>Use list, add, remove, or reset.");
	}
	
	private void listRelations(UGate gate, UNetworkAccess access)
	{
		msg("<i>Use access for <h>%s <i>/ <h>%s <i>/ <h>%s<i>:",
			gate.getScopeType().name(),
			GateScopeUtil.describeOwner(gate.getScopeType(), gate.getNetworkOwnerId()),
			String.valueOf(gate.getNetworkId())
		);
		if (gate.getScopeType() == GateScopeType.PLAYER)
		{
			msg("<k>Owner (implicit): <v>%s", GateScopeUtil.describeOwner(GateScopeType.PLAYER, gate.getNetworkOwnerId()));
		}
		Set<String> relations = access.getUseRelations();
		if (relations.isEmpty())
		{
			msg("<i>(no extra relations)");
			return;
		}
		List<String> sorted = new ArrayList<>(relations);
		sorted.sort(String.CASE_INSENSITIVE_ORDER);
		for (String token : sorted)
		{
			msg("<k>- <v>%s", describeToken(token));
		}
	}
	
	private String resolveRelationToken(String raw, GateScopeType scope)
	{
		if (raw == null) return null;
		String t = raw.trim();
		
		GateTeamRelation rel = GateTeamRelation.parse(t);
		if (rel != null)
		{
			return rel.name();
		}
		
		if (t.toLowerCase(Locale.ROOT).startsWith(UNetworkAccess.PLAYER_PREFIX)
			|| t.toLowerCase(Locale.ROOT).startsWith(UNetworkAccess.TEAM_PREFIX))
		{
			return UNetworkAccess.normalizeToken(t);
		}
		
		String playerId = IdUtil.getId(t);
		if (playerId != null)
		{
			Player online = IdUtil.getPlayer(playerId);
			if (online != null || IdUtil.getName(playerId) != null)
			{
				return UNetworkAccess.playerToken(playerId);
			}
		}
		
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		if (bridge != null && bridge.isAvailable())
		{
			String lookup = t;
			if (t.regionMatches(true, 0, "faction:", 0, 8) || t.regionMatches(true, 0, "team:", 0, 5))
			{
				lookup = t.substring(t.indexOf(':') + 1).trim();
			}
			String teamId = bridge.resolveTeamId(lookup);
			if (teamId != null) return UNetworkAccess.teamToken(teamId);
		}
		
		return null;
	}
	
	private static String describeToken(String token)
	{
		if (token == null) return "unknown";
		if (token.startsWith(UNetworkAccess.PLAYER_PREFIX))
		{
			String id = token.substring(UNetworkAccess.PLAYER_PREFIX.length());
			String name = IdUtil.getName(id);
			return "player:" + (name != null ? name : id);
		}
		if (token.startsWith(UNetworkAccess.TEAM_PREFIX))
		{
			String id = token.substring(UNetworkAccess.TEAM_PREFIX.length());
			TeamBridge bridge = TeamBridgeRegistry.getActive();
			String name = bridge != null ? bridge.getTeamDisplayName(id) : null;
			return "team:" + (name != null ? name : id);
		}
		return token;
	}
}
