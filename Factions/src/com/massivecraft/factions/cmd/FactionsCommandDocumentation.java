package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqFactionWarpsEnabled;
import com.massivecraft.massivecore.mson.Mson;

public class FactionsCommandDocumentation extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public FactionsCommandDocumentation()
	{
		this.addRequirements(ReqFactionWarpsEnabled.get());
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public int num = 1;

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void senderFields(boolean set)
	{
		super.senderFields(set);

		num = 1;
	}

	// -------------------------------------------- //
	// MESSAGE
	// -------------------------------------------- //

	public void msgDoc(String msg, Object... args)
	{
		msg = "<lime>" + this.num++ + ") <i>" + msg;
		msg(msg, args);
	}

	public void messageDoc(Mson message)
	{
		Mson mson = mson(Mson.parse("<lime>" + this.num++ + ") "), message);
		message(mson);
	}

}
