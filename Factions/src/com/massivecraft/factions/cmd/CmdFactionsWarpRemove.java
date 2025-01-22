package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeWarp;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.factions.event.EventFactionsWarpRemove;
import com.massivecraft.massivecore.MassiveException;

public class CmdFactionsWarpRemove extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsWarpRemove()
	{
		// Parameters
		this.addParameter(TypeWarp.get(), "warp");
		this.addParameter(TypeFaction.get(), "faction", "you");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Faction faction = this.readArgAt(1, msenderFaction);
		Warp warp = TypeWarp.get(faction).read(this.argAt(0), sender);
		
		// Any and MPerm
		if ( ! MPerm.getPermSetwarp().has(msender, faction, true)) return;
		
		// Event
		EventFactionsWarpRemove event = new EventFactionsWarpRemove(sender, faction, warp);
		event.run();
		if (event.isCancelled()) return;
		warp = event.getWarp();

		// Apply
		warp.detach();
		
		// Inform
		faction.msg("%s<i> removed the warp <h>%s <i>for your faction.", msender.describeTo(msenderFaction, true), warp.getName());
		if (faction != msenderFaction)
		{
			msender.msg("<i>You have removed the warp <h>%s <i>for %s<i>.", warp.getName(), faction.getName(msender));
		}
	}
	
}
