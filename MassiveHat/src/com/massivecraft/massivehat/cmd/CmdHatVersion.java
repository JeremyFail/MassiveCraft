package com.massivecraft.massivehat.cmd;

import com.massivecraft.massivecore.command.MassiveCommandVersion;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivehat.MassiveHat;
import com.massivecraft.massivehat.Perm;
import com.massivecraft.massivehat.entity.MConf;

import java.util.List;

public class CmdHatVersion extends MassiveCommandVersion
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdHatVersion()
	{
		super(MassiveHat.get());
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.VERSION));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesHatVersion;
	}
	
}
