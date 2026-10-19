package com.massivecraft.creativegates.integration.team;

import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.factions.event.EventFactionsDisband;
import com.massivecraft.massivecore.Engine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * Destroys FACTION-scoped gates when a MassiveCraft faction is disbanded.
 */
public class EngineFactionsDisband extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static EngineFactionsDisband i = new EngineFactionsDisband();
	public static EngineFactionsDisband get() { return i; }
	
	// -------------------------------------------- //
	// LISTENER
	// -------------------------------------------- //
	
	/**
	 * After a successful disband, remove all FACTION gates for that faction id.
	 *
	 * @param event Disband event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDisband(EventFactionsDisband event)
	{
		if (event == null) return;
		String factionId = event.getFactionId();
		if (factionId == null || factionId.isEmpty()) return;
		UGateColl.get().destroyFactionGatesForTeam(factionId);
	}
	
}
