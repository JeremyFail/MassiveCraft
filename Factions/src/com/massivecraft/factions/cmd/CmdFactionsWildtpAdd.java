package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.WildTpPredefinedLocation;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeInteger;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin subcommand {@code /f wildtp add <name> [noClaimRadiusChunks]}.
 * Saves the player's current location as a predefined wilderness teleport spot.
 * Only available when wildtp mode is {@code predefined} or {@code hybrid}.
 */
public class CmdFactionsWildtpAdd extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	/**
	 * Registers parameters and the {@link Perm#WILDTP_ADD} requirement.
	 */
	public CmdFactionsWildtpAdd()
	{
		this.setDesc("add a predefined wilderness teleport location at your position");
		this.addParameter(TypeString.get(), "name");
		this.addParameter(TypeInteger.get(), "noClaimRadiusChunks", "default");
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.WILDTP_ADD));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	/**
	 * Adds a named predefined location at the sender's feet.
	 *
	 * @throws MassiveException if arguments are invalid
	 */
	@Override
	public void perform() throws MassiveException
	{
		MConf conf = MConf.get();

		if (!conf.isWildTpModeUsingPredefined())
		{
			msg("<b>Adding predefined locations is only available when wildtp mode is <h>predefined <b>or <h>hybrid<b> (currently <h>%s<b>).", conf.getWildTpTeleportMode().getId());
			return;
		}

		String name = this.readArg();
		int radius = this.readArg(conf.wildTpPredefinedDefaultRadiusNoPlayerClaims);
		if (radius < 0)
		{
			msg("<b>No-claim radius must be zero or positive (chunks).");
			return;
		}

		if (name == null || name.trim().isEmpty())
		{
			msg("<b>You must provide a location name.");
			return;
		}
		name = name.trim();

		List<WildTpPredefinedLocation> list = conf.wildTpPredefinedLocations;
		if (list == null)
		{
			list = new ArrayList<>();
			conf.wildTpPredefinedLocations = list;
		}

		for (WildTpPredefinedLocation existing : list)
		{
			if (existing != null && existing.name != null && existing.name.equalsIgnoreCase(name))
			{
				msg("<b>A predefined wildtp location named <h>%s <b>already exists.", existing.name);
				return;
			}
		}

		Location loc = me.getLocation();
		WildTpPredefinedLocation entry = new WildTpPredefinedLocation(
			name,
			loc.getWorld().getName(),
			loc.getX(),
			loc.getY(),
			loc.getZ(),
			radius,
			false
		);
		list.add(entry);
		conf.changed();

		msg("<i>Added predefined wildtp location <h>%s <i>at <h>%.1f %.1f %.1f <i>in <h>%s <i>(no-claim radius <h>%d <i>chunks).",
			entry.name, entry.x, entry.y, entry.z, entry.world, entry.radiusNoPlayerClaims);
	}

}
