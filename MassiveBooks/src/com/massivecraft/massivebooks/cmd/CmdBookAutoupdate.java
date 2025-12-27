package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeBooleanTrue;

import java.util.List;

public class CmdBookAutoupdate extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookAutoupdate()
	{
		// Parameters
		this.addParameter(TypeBooleanTrue.get(), "true/false", "toggle");
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.AUTOUPDATE));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesBookAutoupdate;
	}
	
	@Override
	public void perform() throws MassiveException
	{
		boolean currentState = mme.isUsingAutoUpdate();
		boolean targetState = this.readArg(!currentState);
		
		if (!MConf.get().autoupdatingServerbooks)
		{
			message(Lang.AUTOUPDATE_DERPWARN);
		}
		
		mme.setUsingAutoUpdate(targetState, true, true);
	}
	
}
