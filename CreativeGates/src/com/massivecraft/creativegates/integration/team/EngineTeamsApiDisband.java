package com.massivecraft.creativegates.integration.team;

import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.massivecore.Engine;
import com.skyblockexp.teamsapi.event.TeamDeleteEvent;
import com.skyblockexp.teamsapi.model.Team;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * Destroys FACTION-scoped gates when a TeamsAPI team is deleted.
 * Providers are encouraged to fire {@link TeamDeleteEvent}; if they do not,
 * the use-time safety net in {@link com.massivecraft.creativegates.entity.UGate} still cleans up.
 */
public class EngineTeamsApiDisband extends Engine
{
	private static EngineTeamsApiDisband i = new EngineTeamsApiDisband();
	public static EngineTeamsApiDisband get() { return i; }
	
	/**
	 * After a non-cancelled team delete, remove FACTION gates for that team id.
	 *
	 * @param event Team delete event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTeamDelete(TeamDeleteEvent event)
	{
		if (event == null) return;
		Team team = event.getTeam();
		if (team == null || team.getId() == null) return;
		UGateColl.get().destroyFactionGatesForTeam(team.getId().toString());
	}
}
