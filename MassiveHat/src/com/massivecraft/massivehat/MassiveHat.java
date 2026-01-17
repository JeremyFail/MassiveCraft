package com.massivecraft.massivehat;

import com.massivecraft.massivecore.MassivePlugin;
import com.massivecraft.massivehat.cmd.CmdHat;
import com.massivecraft.massivehat.cmd.CmdHatUse;
import com.massivecraft.massivehat.engine.EngineHatSwitch;
import com.massivecraft.massivehat.entity.MConfColl;

public class MassiveHat extends MassivePlugin
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static MassiveHat i;
	public static MassiveHat get() { return i; }
	public MassiveHat() { MassiveHat.i = this; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void onEnableInner()
	{
		// Activate
		this.activate(
			// Coll
			MConfColl.class,
		
			// Engine
			EngineHatSwitch.class,
			
			// Command
			CmdHat.class,
			CmdHatUse.class
		);
	}
	
}
