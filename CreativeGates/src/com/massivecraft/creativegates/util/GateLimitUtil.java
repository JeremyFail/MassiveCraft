package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Enforces configurable gate/network limits on create and transfer.
 */
public final class GateLimitUtil
{
	private GateLimitUtil()
	{
	}
	
	/**
	 * True if the actor may skip limit checks.
	 *
	 * @param sender Actor.
	 * @return True when limits.bypass applies, or override is configured to bypass limits.
	 */
	public static boolean canBypassLimits(CommandSender sender)
	{
		if (sender == null) return false;
		if (Perm.LIMITS_BYPASS.has(sender)) return true;
		if (!MConf.get().isOverrideBypassesLimits()) return false;
		return EngineGateOverride.canBypassOwnership(sender);
	}
	
	/**
	 * Validates that adding one gate under the destination identity is within caps.
	 * Does not apply per-owner caps to GLOBAL.
	 *
	 * @param sender Actor (for messaging/bypass).
	 * @param scope Destination scope.
	 * @param networkOwnerId Destination owner or null.
	 * @param networkId Destination network name.
	 * @param creatorId Creator id for SHARED spam cap (usually the actor).
	 * @param excludeGate Gate being moved (excluded from counts), or null for create.
	 * @param messageOnFail Whether to message the actor.
	 * @return True if allowed.
	 */
	public static boolean checkCanAdd(
		CommandSender sender,
		GateScopeType scope,
		String networkOwnerId,
		String networkId,
		String creatorId,
		UGate excludeGate,
		boolean messageOnFail
	)
	{
		if (canBypassLimits(sender)) return true;
		if (scope == null) return false;
		
		MConf conf = MConf.get();
		
		int perNetwork = conf.getMaxGatesPerNetwork(scope);
		if (perNetwork > 0)
		{
			int count = countNetwork(scope, networkOwnerId, networkId, excludeGate);
			if (count >= perNetwork)
			{
				fail(sender, messageOnFail, "This network already has the maximum of " + perNetwork + " gates.");
				return false;
			}
		}
		
		if (scope == GateScopeType.PLAYER && networkOwnerId != null)
		{
			int maxGates = conf.getMaxGatesPerPlayerOwner();
			if (maxGates > 0)
			{
				int count = countOwnerGates(GateScopeType.PLAYER, networkOwnerId, excludeGate);
				if (count >= maxGates)
				{
					fail(sender, messageOnFail, "You already own the maximum of " + maxGates + " player gates.");
					return false;
				}
			}
			int maxNetworks = conf.getMaxNetworksPerPlayer();
			if (maxNetworks > 0 && !networkExists(GateScopeType.PLAYER, networkOwnerId, networkId, excludeGate))
			{
				int networks = countDistinctNetworks(GateScopeType.PLAYER, networkOwnerId, excludeGate);
				if (networks >= maxNetworks)
				{
					fail(sender, messageOnFail, "You already have the maximum of " + maxNetworks + " player networks.");
					return false;
				}
			}
		}
		
		if (scope == GateScopeType.FACTION && networkOwnerId != null)
		{
			int maxGates = conf.getMaxGatesPerFactionOwner();
			if (maxGates > 0)
			{
				int count = countOwnerGates(GateScopeType.FACTION, networkOwnerId, excludeGate);
				if (count >= maxGates)
				{
					fail(sender, messageOnFail, "That " + FactionTerm.get() + " already has the maximum of " + maxGates + " gates.");
					return false;
				}
			}
			int maxNetworks = conf.getMaxNetworksPerFaction();
			if (maxNetworks > 0 && !networkExists(GateScopeType.FACTION, networkOwnerId, networkId, excludeGate))
			{
				int networks = countDistinctNetworks(GateScopeType.FACTION, networkOwnerId, excludeGate);
				if (networks >= maxNetworks)
				{
					fail(sender, messageOnFail, "That " + FactionTerm.get() + " already has the maximum of " + maxNetworks + " networks.");
					return false;
				}
			}
		}
		
		if (scope == GateScopeType.SHARED && creatorId != null)
		{
			int maxShared = conf.getMaxSharedGatesCreatedPerPlayer();
			if (maxShared > 0)
			{
				int count = countSharedCreatedBy(creatorId, excludeGate);
				if (count >= maxShared)
				{
					fail(sender, messageOnFail, "You already created the maximum of " + maxShared + " shared gates.");
					return false;
				}
			}
		}
		
		return true;
	}
	
	private static void fail(CommandSender sender, boolean message, String text)
	{
		if (!message || sender == null) return;
		MixinMessage.get().messageOne(sender, Txt.parse("<b>%s", text));
	}
	
	private static int countNetwork(GateScopeType scope, String ownerId, String networkId, UGate exclude)
	{
		int n = 0;
		for (UGate gate : UGateColl.get().getGateChain(scope, ownerId, networkId))
		{
			if (gate == null || gate == exclude) continue;
			n++;
		}
		return n;
	}
	
	private static int countOwnerGates(GateScopeType scope, String ownerId, UGate exclude)
	{
		int n = 0;
		for (UGate gate : UGateColl.get().getAll())
		{
			if (gate == null || gate == exclude) continue;
			if (gate.getScopeType() != scope) continue;
			if (!ownerId.equals(gate.getNetworkOwnerId())) continue;
			n++;
		}
		return n;
	}
	
	private static int countDistinctNetworks(GateScopeType scope, String ownerId, UGate exclude)
	{
		java.util.Set<String> names = new java.util.HashSet<>();
		for (UGate gate : UGateColl.get().getAll())
		{
			if (gate == null || gate == exclude) continue;
			if (gate.getScopeType() != scope) continue;
			if (!ownerId.equals(gate.getNetworkOwnerId())) continue;
			String id = gate.getNetworkId();
			if (id != null) names.add(id.toLowerCase());
		}
		return names.size();
	}
	
	private static boolean networkExists(GateScopeType scope, String ownerId, String networkId, UGate exclude)
	{
		for (UGate gate : UGateColl.get().getGateChain(scope, ownerId, networkId))
		{
			if (gate == null || gate == exclude) continue;
			return true;
		}
		return false;
	}
	
	private static int countSharedCreatedBy(String creatorId, UGate exclude)
	{
		int n = 0;
		for (UGate gate : UGateColl.get().getAll())
		{
			if (gate == null || gate == exclude) continue;
			if (gate.getScopeType() != GateScopeType.SHARED) continue;
			if (!creatorId.equalsIgnoreCase(gate.getCreatorId())) continue;
			n++;
		}
		return n;
	}
	
	/**
	 * Convenience creator id for a player sender.
	 *
	 * @param sender Actor.
	 * @return Id string or null.
	 */
	public static String creatorIdOf(CommandSender sender)
	{
		if (sender instanceof Player) return IdUtil.getId((Player) sender);
		return IdUtil.getId(sender);
	}
}
