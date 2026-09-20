package com.massivecraft.creativegates.engine;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.MPlayer;
import com.massivecraft.creativegates.entity.UGate;
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
	 * True if the sender may change settings on the gate.
	 * 
	 * @param sender Command sender.
	 * @param gate Gate to manage.
	 * @return True if the sender may change settings on the gate.
	 */
	public static boolean canManage(CommandSender sender, UGate gate)
	{
		if (sender == null || gate == null) return false;
		if (gate.isCreator(sender)) return true;
		return canBypassOwnership(sender);
	}
	
	/**
	 * True if the sender may read full inscriptions on a secret gate.
	 * 
	 * @param sender Command sender.
	 * @param gate Gate to read.
	 * @return True if the sender may read full inscriptions on a secret gate.
	 */
	public static boolean canReadSecret(CommandSender sender, UGate gate)
	{
		if (sender == null || gate == null) return false;
		if (!gate.isRestricted()) return true;
		if (gate.isCreator(sender)) return true;
		return canBypassOwnership(sender);
	}
	
}
