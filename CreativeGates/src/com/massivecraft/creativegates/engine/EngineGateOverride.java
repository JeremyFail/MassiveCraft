package com.massivecraft.creativegates.engine;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.MPlayer;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.integration.team.TeamBridge;
import com.massivecraft.creativegates.integration.team.TeamBridgeRegistry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Access helpers for gate inspect/manage (override mode lives on {@link MPlayer}).
 */
public final class EngineGateOverride
{
	private EngineGateOverride()
	{
	}
	
	/**
	 * True if the sender may manage any gate without being the creator
	 * (override mode or bypass permission).
	 * 
	 * @param sender Command sender.
	 * @return True if the sender may manage any gate without being the creator.
	 */
	public static boolean canBypassOwnership(CommandSender sender)
	{
		if (sender == null) return false;
		if (Perm.CG_OVERRIDE_BYPASS.has(sender)) return true;
		if (!(sender instanceof Player)) return false;
		MPlayer mplayer = MPlayer.get(sender);
		return mplayer != null && mplayer.isOverriding();
	}
	
	/**
	 * True if the sender may create gates in worlds listed in
	 * {@code gateCreationDisabledWorlds} ({@link Perm#CREATE_BYPASSDISABLED},
	 * override mode, or {@link Perm#CG_OVERRIDE_BYPASS}).
	 *
	 * @param sender Command sender.
	 * @return True if world creation restrictions do not apply.
	 */
	public static boolean canBypassCreationWorldRestriction(CommandSender sender)
	{
		if (sender == null) return false;
		if (Perm.CREATE_BYPASSDISABLED.has(sender)) return true;
		return canBypassOwnership(sender);
	}
	
	/**
	 * True if the sender may skip {@code blocksrequired} frame materials
	 * ({@link Perm#CREATE_BYPASSFRAME}, override mode, or {@link Perm#CG_OVERRIDE_BYPASS}).
	 *
	 * @param sender Command sender.
	 * @return True if frame material requirements do not apply.
	 */
	public static boolean canBypassFrameRequirement(CommandSender sender)
	{
		if (sender == null) return false;
		if (Perm.CREATE_BYPASSFRAME.has(sender)) return true;
		return canBypassOwnership(sender);
	}
	
	/**
	 * True if the sender may change settings on the gate.
	 * FACTION gates use team rank perms only.
	 * Other scopes allow the creator, plus override/bypass.
	 * 
	 * @param sender Command sender.
	 * @param gate Gate to manage.
	 * @return True if the sender may change settings on the gate.
	 */
	public static boolean canManage(CommandSender sender, UGate gate)
	{
		if (sender == null || gate == null) return false;
		if (canBypassOwnership(sender)) return true;
		
		if (gate.getScopeType() == GateScopeType.FACTION)
		{
			if (!(sender instanceof Player)) return false;
			TeamBridge bridge = TeamBridgeRegistry.getActive();
			String teamId = gate.getNetworkOwnerId();
			return bridge != null && bridge.isAvailable() && teamId != null
				&& bridge.canManage((Player) sender, teamId);
		}
		
		return gate.isCreator(sender);
	}
	
	/**
	 * True if the sender may move an entire network identity (all gates on one chain).
	 * Granted by {@link Perm#CG_MANAGE_TRANSFERNETWORK}, override mode, or override bypass.
	 *
	 * @param sender Command sender.
	 * @return True if whole-network transfer is allowed.
	 */
	public static boolean canTransferWholeNetwork(CommandSender sender)
	{
		if (sender == null) return false;
		if (Perm.CG_MANAGE_TRANSFERNETWORK.has(sender)) return true;
		return canBypassOwnership(sender);
	}
	
	/**
	 * True if the sender may read full inscriptions on a secret gate.
	 * FACTION secret gates: any member of that faction. Other scopes: creator.
	 * Override/bypass always may.
	 * 
	 * @param sender Command sender.
	 * @param gate Gate to read.
	 * @return True if the sender may read full inscriptions on a secret gate.
	 */
	public static boolean canReadSecret(CommandSender sender, UGate gate)
	{
		if (sender == null || gate == null) return false;
		if (!gate.isRestricted()) return true;
		if (canBypassOwnership(sender)) return true;
		
		if (gate.getScopeType() == GateScopeType.FACTION)
		{
			if (!(sender instanceof Player)) return false;
			TeamBridge bridge = TeamBridgeRegistry.getActive();
			String teamId = gate.getNetworkOwnerId();
			if (bridge == null || !bridge.isAvailable() || teamId == null) return false;
			String playerTeam = bridge.getTeamId((Player) sender);
			return teamId.equals(playerTeam);
		}
		
		return gate.isCreator(sender);
	}
	
}
