package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.entity.UNetworkAccessColl;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Renames a single gate's network (moves it to a new identity, same scope/owner).
 */
public final class GateNetworkRename
{
	private GateNetworkRename()
	{
	}
	
	/**
	 * XP levels required to rename when consumption is enabled (0 when free / bypass).
	 *
	 * @param player Actor.
	 * @return Levels to charge, or 0.
	 */
	public static int xpCostLevels(Player player)
	{
		if (player == null) return 0;
		if (EngineGateOverride.canBypassOwnership(player)) return 0;
		MConf conf = MConf.get();
		if (!conf.isNetworkRenameConsumesXp()) return 0;
		return Math.max(0, conf.getNetworkRenameXpLevels());
	}
	
	/**
	 * True if the player can afford the configured rename XP cost.
	 *
	 * @param player Actor.
	 * @return True when free or enough levels.
	 */
	public static boolean canAfford(Player player)
	{
		int cost = xpCostLevels(player);
		if (cost <= 0) return true;
		return player.getLevel() >= cost;
	}
	
	/**
	 * Tooltip / chat hover describing XP cost and affordability.
	 *
	 * @param player Actor.
	 * @return Parsed tooltip lines.
	 */
	public static String costTooltip(Player player)
	{
		int cost = xpCostLevels(player);
		if (cost <= 0)
		{
			return Txt.parse("Change which network this gate belongs to.");
		}
		if (canAfford(player))
		{
			return Txt.parse("Change which network this gate belongs to.\n<k>Cost: <h>%d <k>XP level%s.", cost, cost == 1 ? "" : "s");
		}
		return Txt.parse(
			"Change which network this gate belongs to.\n<b>Need <h>%d <b>XP level%s (you have <h>%d<b>).",
			cost,
			cost == 1 ? "" : "s",
			player.getLevel()
		);
	}
	
	/**
	 * Normalizes a typed network name (strip colors, trim).
	 *
	 * @param raw Raw input.
	 * @return Normalized name, or null if empty.
	 */
	public static String normalizeName(String raw)
	{
		if (raw == null) return null;
		String name = ChatColor.stripColor(raw).trim();
		return name.isEmpty() ? null : name;
	}
	
	/**
	 * Renames the gate's network identity (keeps scope and owner).
	 *
	 * @param player Actor.
	 * @param gate Gate.
	 * @param rawName New network name.
	 * @param message Whether to message.
	 * @return True on success.
	 */
	public static boolean rename(Player player, UGate gate, String rawName, boolean message)
	{
		if (player == null || gate == null) return false;
		if (!EngineGateOverride.canManage(player, gate))
		{
			if (message) MixinMessage.get().messageOne(player, Txt.parse("<b>You may not manage that gate."));
			return false;
		}
		
		String newName = normalizeName(rawName);
		if (newName == null)
		{
			if (message) MixinMessage.get().messageOne(player, Txt.parse("<b>Network name cannot be empty."));
			return false;
		}
		
		String oldName = gate.getNetworkId();
		if (oldName != null && oldName.equalsIgnoreCase(newName))
		{
			if (message) MixinMessage.get().messageOne(player, Txt.parse("<i>That gate is already on network <h>%s<i>.", newName));
			return false;
		}
		
		if (!canAfford(player))
		{
			if (message)
			{
				MixinMessage.get().messageOne(player, Txt.parse(
					"<b>You need <h>%d <b>XP level%s to rename a network.",
					xpCostLevels(player),
					xpCostLevels(player) == 1 ? "" : "s"
				));
			}
			return false;
		}
		
		GateScopeType scope = gate.getScopeType();
		String ownerId = gate.getNetworkOwnerId();
		String creatorId = GateLimitUtil.creatorIdOf(player);
		if (!GateLimitUtil.checkCanAdd(player, scope, ownerId, newName, creatorId, gate, message))
		{
			return false;
		}
		
		gate.setNetworkId(newName);
		gate.setChainIndex(UGateColl.get().nextChainIndex(scope, ownerId, newName));
		UNetworkAccessColl.get().onIdentityChanged(scope, ownerId, oldName, scope, ownerId, newName);
		GateAccessUtil.ensureAccess(gate);
		
		int cost = xpCostLevels(player);
		if (cost > 0)
		{
			player.giveExpLevels(-cost);
		}
		
		if (message)
		{
			if (cost > 0)
			{
				MixinMessage.get().messageOne(player, Txt.parse(
					"<g>Network changed to <h>%s<g> (spent <h>%d <g>XP level%s).",
					newName,
					cost,
					cost == 1 ? "" : "s"
				));
			}
			else
			{
				MixinMessage.get().messageOne(player, Txt.parse("<g>Network changed to <h>%s<g>.", newName));
			}
		}
		return true;
	}
}
