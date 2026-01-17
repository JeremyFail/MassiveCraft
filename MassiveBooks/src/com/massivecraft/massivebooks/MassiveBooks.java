package com.massivecraft.massivebooks;

import com.massivecraft.massivebooks.cmd.CmdBook;
import com.massivecraft.massivebooks.entity.MBookColl;
import com.massivecraft.massivebooks.entity.MConfColl;
import com.massivecraft.massivebooks.entity.migrator.MigratorMBook001IntIdToString;
import com.massivecraft.massivecore.MassivePlugin;
import com.massivecraft.massivecore.util.MUtil;

import java.util.List;

public class MassiveBooks extends MassivePlugin 
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static MassiveBooks i;
	public static MassiveBooks get() { return i; }
	public MassiveBooks() { MassiveBooks.i = this; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<Class<?>> getClassesActiveMigrators()
	{
		return MUtil.list(
			MigratorMBook001IntIdToString.class
		);
	}
	
	@Override
	public void onEnableInner()
	{
		// Activate
		this.activateAuto();
	}
	
	@Override
	public List<Class<?>> getClassesActiveColls()
	{
		return MUtil.list(
			MConfColl.class,
			MBookColl.class
		);
	}
	
	@Override
	public List<Class<?>> getClassesActiveCommands()
	{
		return MUtil.list(
			CmdBook.class
		);
	}
	
	@Override
	public List<Class<?>> getClassesActiveEngines()
	{
		return MUtil.list(
			EngineMain.class,
			EnginePowertool.class
		);
	}
	
}
