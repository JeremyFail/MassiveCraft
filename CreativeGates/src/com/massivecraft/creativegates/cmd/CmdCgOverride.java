package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeBooleanYes;
import com.massivecraft.massivecore.mixin.MixinDisplayName;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;

import java.util.List;

public class CmdCgOverride extends MassiveCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdCgOverride()
	{
		// Parameters
		this.addParameter(TypeBooleanYes.get(), "on/off", "flip");
		
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG_OVERRIDE));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgOverride());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		MPlayer mplayer = MPlayer.get(this.me);
		
		boolean target = this.readArg(!mplayer.isOverriding());
		mplayer.setOverriding(target);
		
		String desc = Txt.parse(mplayer.isOverriding() ? "<g>ENABLED" : "<b>DISABLED");
		String display = MixinDisplayName.get().getDisplayName(this.me, this.me);
		
		msg("<i>%s %s <i>override mode.", display, desc);
		CreativeGates.get().log(Txt.parse("<i>%s %s <i>override mode.", MixinDisplayName.get().getDisplayName(this.me, IdUtil.getConsole()), desc));
	}
	
}
