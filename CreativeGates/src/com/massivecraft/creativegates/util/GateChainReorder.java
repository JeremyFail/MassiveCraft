package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Shared chain reorder helpers for commands and manage UI.
 */
public final class GateChainReorder
{
	private GateChainReorder()
	{
	}
	
	/**
	 * True if the player may reorder this gate's chain.
	 *
	 * @param player Actor.
	 * @param gate Gate.
	 * @return True when allowed.
	 */
	public static boolean canReorder(Player player, UGate gate)
	{
		if (player == null || gate == null) return false;
		if (!EngineGateOverride.canManage(player, gate)) return false;
		if (gate.getScopeType() == GateScopeType.SHARED && !EngineGateOverride.canBypassOwnership(player))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Moves the gate one step earlier in travel order.
	 *
	 * @param player Actor.
	 * @param gate Gate.
	 * @param message Whether to message.
	 * @return True if the position changed.
	 */
	public static boolean moveUp(Player player, UGate gate, boolean message)
	{
		if (!canReorder(player, gate))
		{
			if (message) deny(player, gate);
			return false;
		}
		List<UGate> chain = UGateColl.get().getGateChain(gate);
		int index = chain.indexOf(gate);
		if (index < 0)
		{
			if (message) MixinMessage.get().messageOne(player, Txt.parse("<b>Could not locate this gate in its chain."));
			return false;
		}
		if (index == 0)
		{
			if (message) MixinMessage.get().messageOne(player, Txt.parse("<i>Already at the start of the chain."));
			return false;
		}
		swapIndex(chain.get(index - 1), gate);
		if (message)
		{
			MixinMessage.get().messageOne(player, Txt.parse("<g>Moved gate up in the chain (position <h>%d<g>).", displayPos(gate)));
		}
		return true;
	}
	
	/**
	 * Moves the gate one step later in travel order.
	 *
	 * @param player Actor.
	 * @param gate Gate.
	 * @param message Whether to message.
	 * @return True if the position changed.
	 */
	public static boolean moveDown(Player player, UGate gate, boolean message)
	{
		if (!canReorder(player, gate))
		{
			if (message) deny(player, gate);
			return false;
		}
		List<UGate> chain = UGateColl.get().getGateChain(gate);
		int index = chain.indexOf(gate);
		if (index < 0)
		{
			if (message) MixinMessage.get().messageOne(player, Txt.parse("<b>Could not locate this gate in its chain."));
			return false;
		}
		if (index >= chain.size() - 1)
		{
			if (message) MixinMessage.get().messageOne(player, Txt.parse("<i>Already at the end of the chain."));
			return false;
		}
		swapIndex(gate, chain.get(index + 1));
		if (message)
		{
			MixinMessage.get().messageOne(player, Txt.parse("<g>Moved gate down in the chain (position <h>%d<g>).", displayPos(gate)));
		}
		return true;
	}
	
	/**
	 * 1-based position in the ordered chain, or 0 if missing.
	 *
	 * @param gate Gate.
	 * @return Display position.
	 */
	public static int displayPos(UGate gate)
	{
		if (gate == null) return 0;
		List<UGate> chain = UGateColl.get().getGateChain(gate);
		int index = chain.indexOf(gate);
		return index < 0 ? 0 : index + 1;
	}
	
	/**
	 * Chain size for the gate's identity.
	 *
	 * @param gate Gate.
	 * @return Size.
	 */
	public static int chainSize(UGate gate)
	{
		if (gate == null) return 0;
		return UGateColl.get().getGateChain(gate).size();
	}
	
	/**
	 * Sends the standard reorder-denied message.
	 *
	 * @param player Actor.
	 * @param gate Gate.
	 */
	public static void tellDenied(Player player, UGate gate)
	{
		deny(player, gate);
	}
	
	/**
	 * Sends the standard reorder-denied message.
	 * 
	 * @param player The player who is being denied reordering.
	 * @param gate The gate that is being denied reordering.
	 */
	private static void deny(Player player, UGate gate)
	{
		if (gate != null && gate.getScopeType() == GateScopeType.SHARED
			&& !EngineGateOverride.canBypassOwnership(player))
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>SHARED networks cannot be reordered. Use override, or ask staff."));
			return;
		}
		MixinMessage.get().messageOne(player, Txt.parse("<b>You may not reorder that gate."));
	}
	
	/**
	 * Swaps the index of two gates.
	 * 
	 * @param a The first gate to swap.
	 * @param b The second gate to swap.
	 */
	private static void swapIndex(UGate a, UGate b)
	{
		int ai = a.getChainIndex();
		int bi = b.getChainIndex();
		a.setChainIndex(bi);
		b.setChainIndex(ai);
	}
}
