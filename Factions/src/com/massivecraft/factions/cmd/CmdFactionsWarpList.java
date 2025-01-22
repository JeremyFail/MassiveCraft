package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;
import com.massivecraft.massivecore.ps.PSFormatDesc;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsWarpList extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsWarpList()
	{
		// Parameters
		this.addParameter(TypeFaction.get(), "faction", "you");
		this.addParameter(Parameter.getPage());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Faction faction = this.readArg(msenderFaction);
		int idx = this.readArg();

		// Any and MPerm
		if ( ! MPerm.getPermWarp().has(msender, faction, true)) return;

		Pager<Warp> pager = new Pager<>(this, "Warps for " + faction.getName(), idx, faction.getWarps().getAll());
		pager.setMsonifier((Stringifier<Warp>)  (warp, i) ->
			Txt.parse("<lime>%s<i>: %s", warp.getName(), warp.getLocation().getBlock(true).toString(PSFormatDesc.get())));

		pager.message();
	}
	
}
