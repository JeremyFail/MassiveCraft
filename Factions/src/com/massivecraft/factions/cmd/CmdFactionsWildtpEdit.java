package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeWildTpPredefinedLocation;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.WildTpPredefinedLocation;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.type.primitive.TypeInteger;

/**
 * Admin subcommand {@code /f wildtp edit <name> <noClaimRadiusChunks>}.
 * Updates the no-claim chunk radius on an existing predefined location.
 * Only available when wildtp mode is {@code predefined} or {@code hybrid}.
 */
public class CmdFactionsWildtpEdit extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	/**
	 * Registers parameters and the {@link Perm#WILDTP_EDIT} requirement.
	 */
	public CmdFactionsWildtpEdit()
	{
		this.setDesc("edit a predefined wilderness teleport location's no-claim radius");
		this.addParameter(TypeWildTpPredefinedLocation.get(), "name");
		this.addParameter(TypeInteger.get(), "noClaimRadiusChunks");
		this.addRequirements(RequirementHasPerm.get(Perm.WILDTP_EDIT));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	/**
	 * Updates {@link WildTpPredefinedLocation#radiusNoPlayerClaims} for the named location.
	 *
	 * @throws MassiveException if arguments are invalid
	 */
	@Override
	public void perform() throws MassiveException
	{
		MConf conf = MConf.get();

		if (!conf.isWildTpModeUsingPredefined())
		{
			msg("<b>Editing predefined wilderness teleport locations is only available when wildtp mode is <h>predefined <b>or <h>hybrid<b> (currently <h>%s<b>).", conf.getWildTpTeleportMode().getId());
			return;
		}

		WildTpPredefinedLocation found = this.readArg();
		int radius = this.readArg();

		if (radius < 0)
		{
			msg("<b>No-claim radius must be zero or positive (chunks).");
			return;
		}

		int previous = found.radiusNoPlayerClaims;
		found.radiusNoPlayerClaims = radius;
		conf.changed();

		msg("<i>Updated wilderness teleport location <h>%s <i>no-claim radius from <h>%d <i>to <h>%d <i>chunks.",
			found.name, previous, found.radiusNoPlayerClaims);
	}

}
