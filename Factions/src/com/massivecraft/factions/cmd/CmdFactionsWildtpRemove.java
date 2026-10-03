package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeWildTpPredefinedLocation;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.WildTpPredefinedLocation;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;

/**
 * Admin subcommand {@code /f wildtp remove <name>}.
 * Removes a predefined wilderness teleport location by name.
 * Only available when wildtp mode is {@code predefined} or {@code hybrid}.
 */
public class CmdFactionsWildtpRemove extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	/**
	 * Registers the name parameter and the {@link Perm#WILDTP_REMOVE} requirement.
	 */
	public CmdFactionsWildtpRemove()
	{
		this.setDesc("remove a predefined wildtp location by name");
		this.addParameter(TypeWildTpPredefinedLocation.get(), "name");
		this.addRequirements(RequirementHasPerm.get(Perm.WILDTP_REMOVE));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	/**
	 * Removes the named predefined location from MConf.
	 *
	 * @throws MassiveException if arguments are invalid
	 */
	@Override
	public void perform() throws MassiveException
	{
		MConf conf = MConf.get();

		if (!conf.isWildTpModeUsingPredefined())
		{
			msg("<b>Removing predefined locations is only available when wildtp mode is <h>predefined <b>or <h>hybrid<b> (currently <h>%s<b>).", conf.getWildTpTeleportMode().getId());
			return;
		}

		WildTpPredefinedLocation found = this.readArg();
		conf.wildTpPredefinedLocations.remove(found);
		conf.changed();

		msg("<i>Removed predefined wildtp location <h>%s<i>.", found.name);
	}

}
