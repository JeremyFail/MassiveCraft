package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.factions.event.EventFactionsWarpAdd;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.ps.PS;

public class CmdFactionsWarpAdd extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsWarpAdd()
	{
		// Parameters
		this.addParameter(TypeString.get(), "name");
		this.addParameter(TypeFaction.get(), "faction", "you");

		// Aliases
		this.addAliases("create");

		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		String name = this.readArg();
		Faction faction = this.readArg(msenderFaction);

		
		PS ps = PS.valueOf(me.getLocation());
		Warp warp = new Warp(name, ps);
		
		// MPerm
		if ( ! MPerm.getPermSetwarp().has(msender, faction, true)) return;

		if (faction.getWarps().getAll().stream().map(Warp::getName).anyMatch(s -> s.equalsIgnoreCase(name)))
		{
			throw new MassiveException().addMsg("<b>There is already a warp called <h>%s<b>.", name);
		}
		
		// Verify
		if (!msender.isOverriding() && !warp.isValidFor(faction))
		{
			throw new MassiveException().addMsg("<b>Sorry, your faction warps can only be set inside your own claimed territory.");
		}

		if (!msender.isOverriding() && faction.getWarps().size() >= MConf.get().warpsMax && MConf.get().warpsMax >= 0)
		{
			throw new MassiveException().addMsg("<b>You can at most have <h>%d <b>warps at a time.", MConf.get().warpsMax);
		}
		
		// Event
		EventFactionsWarpAdd event = new EventFactionsWarpAdd(sender, faction, warp);
		event.run();
		if (event.isCancelled()) return;
		warp = event.getNewWarp();

		// Apply
		faction.getWarps().attach(warp);
		
		// Inform
		faction.msg("%s<i> added the warp <h>%s <i>to your faction. You can now use:", msender.describeTo(msenderFaction, true), warp.getName());
		faction.sendMessage(CmdFactions.get().cmdFactionsWarp.cmdFactionsWarpGo.getTemplateWithArgs(null, warp.getName()));
		if (faction != msenderFaction)
		{
			msender.msg("<i>You added the warp <h>%s <i>to %s<i>.", warp.getName(), faction.describeTo(msender));
		}
	}
	
}
