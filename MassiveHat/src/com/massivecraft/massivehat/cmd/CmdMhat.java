package com.massivecraft.massivehat.cmd;

import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivehat.Perm;
import com.massivecraft.massivehat.entity.MConf;

import java.util.List;

/**
 * Root command that is an alias for /massivehat use.
 * Like /f home is an alias for /f warp go home.
 * Default /mhat; add /hat, /usehat etc. via aliasesMhat in config.
 */
public class CmdMhat extends MassiveCommand
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //

	private static final CmdMhat i = new CmdMhat();
	public static CmdMhat get() { return i; }

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdMhat()
	{
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.BASECOMMAND));
		this.addRequirements(RequirementHasPerm.get(Perm.USE));

		this.setDesc("Use the block in hand as a hat (same as /massivehat use)");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesMhat());
	}

	@Override
	public void perform()
	{
		CmdHat.get().cmdHatUse.execute(me, new MassiveList<>());
	}
}
