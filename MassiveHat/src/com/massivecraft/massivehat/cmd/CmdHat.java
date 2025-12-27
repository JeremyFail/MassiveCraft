package com.massivecraft.massivehat.cmd;

import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivehat.Perm;
import com.massivecraft.massivehat.entity.MConf;

import java.util.List;

public class CmdHat extends MassiveCommand
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static final CmdHat i = new CmdHat();
	public static CmdHat get() { return i; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public final CmdHatUse cmdHatUse = new CmdHatUse();
	public final CmdHatConfig cmdHatConfig = new CmdHatConfig();
	public final CmdHatVersion cmdHatVersion = new CmdHatVersion();
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdHat()
	{
		// Children
		this.addChild(this.cmdHatUse);
		this.addChild(this.cmdHatConfig);
		this.addChild(this.cmdHatVersion);
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.BASECOMMAND));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesHat;
	}
	
}
