package com.massivecraft.massivecore.cmd;

import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.entity.Multiverse;
import com.massivecraft.massivecore.entity.MultiverseColl;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;
import com.massivecraft.massivecore.util.Txt;

public class CmdMassiveCoreUsysMultiverseList extends MassiveCoreCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdMassiveCoreUsysMultiverseList()
	{
		// Parameters
		this.addParameter(Parameter.getPage());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		int page = this.readArg();
		
		// Pager Create
		Pager<Multiverse> pager = new Pager<>(this, "Multiverse List", page, MultiverseColl.get().getAll(), (Stringifier<Multiverse>) (multiverse, index) -> Txt.parse("<h>" + multiverse.getId() + " <i>has " + Txt.implodeCommaAndDot(multiverse.getUniverses(), "<aqua>%s", "<i>, ", " <i>and ", "<i>.")));
		
		// Pager Message
		pager.message();
	}
	
}
