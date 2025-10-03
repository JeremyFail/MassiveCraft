package com.massivecraft.massivecore.cmd;

import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.store.TypeMultiverse;
import com.massivecraft.massivecore.entity.Multiverse;
import com.massivecraft.massivecore.util.Txt;


public class CmdMassiveCoreUsysMultiverseShow extends MassiveCoreCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdMassiveCoreUsysMultiverseShow()
	{
		// Parameters
		this.addParameter(TypeMultiverse.get(), "multiverse").setDesc("the multiverse to show info about");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		Multiverse multiverse = this.readArg();
		
		message(Txt.titleize("Multiverse: "+multiverse.getId()));
		
		for (String universe : multiverse.getUniverses())
		{
			if (universe.equals(MassiveCore.DEFAULT)) continue;
			msg("<aqua>"+universe+"<i>: "+Txt.implodeCommaAndDot(multiverse.getWorlds(universe), "<h>%s", "<i>, ", " <i>and ", "<i>."));
		}
		msg("<aqua>default<i>: for all other worlds.");
	}

	
}
