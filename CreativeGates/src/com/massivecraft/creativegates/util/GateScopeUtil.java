package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.integration.team.TeamBridge;
import com.massivecraft.creativegates.integration.team.TeamBridgeRegistry;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

/**
 * Scope permission and owner-resolution helpers for create/convert.
 */
public final class GateScopeUtil
{
	private GateScopeUtil()
	{
	}
	
	/**
	 * Scope-specific create permission for the given scope.
	 *
	 * @param scope Scope type.
	 * @return Matching perm, or null if scope is null.
	 */
	public static Perm getCreatePerm(GateScopeType scope)
	{
		if (scope == null) return null;
		switch (scope)
		{
			case SHARED: return Perm.CREATE_SCOPE_SHARED;
			case PLAYER: return Perm.CREATE_SCOPE_PLAYER;
			case FACTION: return Perm.CREATE_SCOPE_FACTION;
			case GLOBAL: return Perm.CREATE_SCOPE_GLOBAL;
			default: return null;
		}
	}
	
	/**
	 * True if FACTION scope can be offered (a team bridge is active).
	 *
	 * @return True when a factions/teams integration is available.
	 */
	public static boolean isFactionScopeAvailable()
	{
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		return bridge != null && bridge.isAvailable();
	}
	
	/**
	 * True if the scope is currently selectable for create/convert (perms + bridge for FACTION).
	 *
	 * @param permissible Actor.
	 * @param scope Target scope.
	 * @param verbose Whether to message on deny.
	 * @return True if the scope may be used.
	 */
	public static boolean isScopeOffered(Permissible permissible, GateScopeType scope, boolean verbose)
	{
		if (scope == null) return false;
		if (!com.massivecraft.creativegates.entity.MConf.get().isScopeEnabled(scope))
		{
			if (verbose && permissible instanceof CommandSender)
			{
				MixinMessage.get().messageOne((CommandSender) permissible, Txt.parse("<b>Scope <h>%s<b> is disabled on this server.", scope.name()));
			}
			return false;
		}
		if (scope == GateScopeType.FACTION)
		{
			if (!isFactionScopeAvailable())
			{
				if (verbose && permissible instanceof CommandSender)
				{
					MixinMessage.get().messageOne((CommandSender) permissible, Txt.parse("<b>%s scope is not available.", FactionTerm.capital()));
				}
				return false;
			}
			// Skip FACTION in create/cycle UI when the actor is not in a team.
			if (permissible instanceof Player)
			{
				TeamBridge bridge = TeamBridgeRegistry.getActive();
				String teamId = bridge != null ? bridge.getTeamId((Player) permissible) : null;
				if (teamId == null || teamId.isEmpty())
				{
					if (verbose)
					{
						MixinMessage.get().messageOne((Player) permissible, Txt.parse("<b>You must be in a %s to use %s scope.", FactionTerm.get(), FactionTerm.get()));
					}
					return false;
				}
			}
		}
		return hasCreateScopePerm(permissible, scope, verbose);
	}
	
	/**
	 * True if the permissible may create/convert into the scope (permission only).
	 *
	 * @param permissible Actor.
	 * @param scope Target scope.
	 * @param verbose Whether to message on deny.
	 * @return True if permitted.
	 */
	public static boolean hasCreateScopePerm(Permissible permissible, GateScopeType scope, boolean verbose)
	{
		Perm perm = getCreatePerm(scope);
		if (perm == null) return false;
		return perm.has(permissible, verbose);
	}
	
	/**
	 * Resolves network owner id for a create under the given scope for the player.
	 * Returns null for GLOBAL/SHARED. For FACTION requires an active bridge and team.
	 *
	 * @param player Creating player.
	 * @param scope Target scope.
	 * @param messageOnFail Whether to message failures.
	 * @return Owner id, empty string means validation failed, or null for no-owner scopes.
	 */
	public static String resolveOwnerIdForCreate(Player player, GateScopeType scope, boolean messageOnFail)
	{
		if (player == null || scope == null) return "";
		if (!scope.requiresOwnerId()) return null;
		
		if (scope == GateScopeType.PLAYER)
		{
			return IdUtil.getId(player);
		}
		
		if (!isFactionScopeAvailable())
		{
			if (messageOnFail)
			{
				MixinMessage.get().messageOne(player, Txt.parse("<b>%s scope is not available.", FactionTerm.capital()));
			}
			return "";
		}
		TeamBridge bridge = TeamBridgeRegistry.getActive();
		
		String teamId = bridge.getTeamId(player);
		if (teamId == null || teamId.isEmpty())
		{
			if (messageOnFail)
			{
				MixinMessage.get().messageOne(player, Txt.parse("<b>You must be in a %s to create a %s network.", FactionTerm.get(), FactionTerm.get()));
			}
			return "";
		}
		
		if (!bridge.canCreate(player, teamId))
		{
			if (messageOnFail)
			{
				MixinMessage.get().messageOne(player, Txt.parse("<b>You may not create gates for that %s.", FactionTerm.get()));
			}
			return "";
		}
		
		return teamId;
	}
	
	/**
	 * Human-readable owner label for inspect/manage.
	 *
	 * @param scope Scope type.
	 * @param networkOwnerId Owner id or null.
	 * @return Display string.
	 */
	public static String describeOwner(GateScopeType scope, String networkOwnerId)
	{
		if (scope == null) scope = GateScopeType.SHARED;
		switch (scope)
		{
			case GLOBAL:
				return "server";
			case SHARED:
				return "shared";
			case PLAYER:
			{
				String name = IdUtil.getName(networkOwnerId);
				return name != null && !name.isEmpty() ? name : String.valueOf(networkOwnerId);
			}
			case FACTION:
			{
				TeamBridge bridge = TeamBridgeRegistry.getActive();
				if (bridge != null && networkOwnerId != null)
				{
					String display = bridge.getTeamDisplayName(networkOwnerId);
					if (display != null && !display.isEmpty()) return display;
				}
				return networkOwnerId != null ? networkOwnerId : ("unknown " + FactionTerm.get());
			}
			default:
				return "unknown";
		}
	}
	
	/**
	 * True if the player may convert this gate into a different offered scope.
	 *
	 * @param player Actor.
	 * @param gate Gate being managed.
	 * @return True when a scope cycle/button should be shown.
	 */
	public static boolean canChangeGateScope(Player player, UGate gate)
	{
		if (player == null || gate == null) return false;
		if (!com.massivecraft.creativegates.engine.EngineGateOverride.canManage(player, gate)) return false;
		GateScopeType next = nextOfferedScope(player, gate.getScopeType());
		return next != null && next != gate.getScopeType();
	}
	
	/**
	 * Next scope after {@code current} that the player may create/convert into.
	 *
	 * @param player Actor.
	 * @param current Current scope (may be null).
	 * @return Next offered scope, or null if none.
	 */
	public static GateScopeType nextOfferedScope(Player player, GateScopeType current)
	{
		if (player == null) return null;
		GateScopeType[] order = {
			GateScopeType.PLAYER,
			GateScopeType.SHARED,
			GateScopeType.FACTION,
			GateScopeType.GLOBAL
		};
		int start = 0;
		if (current != null)
		{
			for (int i = 0; i < order.length; i++)
			{
				if (order[i] == current)
				{
					start = i;
					break;
				}
			}
		}
		for (int offset = 1; offset <= order.length; offset++)
		{
			GateScopeType next = order[(start + offset) % order.length];
			if (!isScopeOffered(player, next, false)) continue;
			return next;
		}
		return null;
	}
	
	/**
	 * Cycles create-scope preference for the player among scopes they may use.
	 *
	 * @param player Player.
	 * @return Newly selected scope, or null if none available.
	 */
	public static GateScopeType cycleCreateScope(Player player)
	{
		if (player == null) return null;
		com.massivecraft.creativegates.entity.MPlayer mplayer = com.massivecraft.creativegates.entity.MPlayer.get(player);
		GateScopeType current = mplayer.getCreateScope();
		GateScopeType next = nextOfferedScope(player, current);
		if (next == null) return current;
		mplayer.setCreateScope(next);
		return next;
	}
	
	/**
	 * Messages the sender about a selected create scope.
	 *
	 * @param sender Recipient.
	 * @param scope Selected scope.
	 */
	public static void tellCreateScope(CommandSender sender, GateScopeType scope)
	{
		if (sender == null || scope == null) return;
		MixinMessage.get().messageOne(sender, Txt.parse("<i>Create scope set to <h>%s<i>.", scope.name()));
	}
}
