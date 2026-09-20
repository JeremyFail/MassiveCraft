package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;

import java.util.List;

public class CmdCg extends MassiveCommand
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static CmdCg i = new CmdCg();
	public static CmdCg get() { return i; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdCgInspect cmdCgInspect = new CmdCgInspect();
	public CmdCgManage cmdCgManage = new CmdCgManage();
	public CmdCgOverride cmdCgOverride = new CmdCgOverride();
	public CmdCgWorld cmdCgWorld = new CmdCgWorld();
	public CmdCgConfig cmdCgConfig = new CmdCgConfig();
	public CmdCgVersion cmdCgVersion = new CmdCgVersion();
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdCg()
	{
		// Children
		this.addChild(this.cmdCgInspect);
		this.addChild(this.cmdCgManage);
		this.addChild(this.cmdCgOverride);
		this.addChild(this.cmdCgWorld);
		this.addChild(this.cmdCgConfig);
		this.addChild(this.cmdCgVersion);
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.CG));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCg());
	}
	
}
