package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeWildTpPredefinedLocation;
import com.massivecraft.factions.entity.WildTpPredefinedLocation;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.mixin.MixinTeleport;
import com.massivecraft.massivecore.mixin.TeleporterException;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.teleport.Destination;
import com.massivecraft.massivecore.teleport.DestinationSimple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Admin subcommand {@code /f wildtp goto <name>}.
 * Teleports to a named predefined location with no safety revalidation
 * (so admins can inspect broken spots). Uses MassiveCore permission-based TP delay.
 */
public class CmdFactionsWildtpGoto extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	/**
	 * Registers name parameter and {@link Perm#WILDTP_LIST_TELEPORT}.
	 */
	public CmdFactionsWildtpGoto()
	{
		this.addAliases("tp");
		this.setDesc("teleport to a predefined wilderness teleport location");
		this.addParameter(TypeWildTpPredefinedLocation.get(), "name");
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.WILDTP_LIST_TELEPORT));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	/**
	 * Teleports the player to the named predefined location (no wildtp safety checks).
	 * Delay comes from MassiveCore {@code permissionToTpdelay} for the sender.
	 *
	 * @throws MassiveException if arguments are invalid
	 */
	@Override
	public void perform() throws MassiveException
	{
		WildTpPredefinedLocation found = this.readArg();

		World world = Bukkit.getWorld(found.world);
		if (world == null)
		{
			msg("<b>World <h>%s <b>for location <h>%s <b>is not loaded.", found.world, found.name);
			return;
		}

		Location location = new Location(world, found.x, found.y, found.z);
		Destination destination = new DestinationSimple(PS.valueOf(location), found.name);

		try
		{
			// Permissible overload → MassiveCoreMConf.getTpdelay(sender) (e.g. massivecore.notpdelay → 0)
			MixinTeleport.get().teleport(me, destination, sender);
		}
		catch (TeleporterException e)
		{
			msg("<b>%s", e.getMessage());
		}
	}

}
