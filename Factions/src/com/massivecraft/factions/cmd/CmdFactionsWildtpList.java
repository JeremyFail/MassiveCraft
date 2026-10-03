package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.WildTpPredefinedLocation;
import com.massivecraft.factions.util.WildTpPredefinedSelector;
import com.massivecraft.factions.util.WildTpPredefinedSelector.Status;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.pager.Msonifier;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.util.Txt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Subcommand {@code /f wildtp list [page]}.
 * Lists configured predefined wildtp locations (name, world, coordinates).
 * Hover shows validity and details; with {@link Perm#WILDTP_LIST_TELEPORT}, entries are clickable.
 */
public class CmdFactionsWildtpList extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	/**
	 * Registers page parameter and {@link Perm#WILDTP_LIST}.
	 */
	public CmdFactionsWildtpList()
	{
		this.setDesc("list predefined wilderness teleport locations");
		this.addParameter(Parameter.getPage());
		this.addRequirements(RequirementHasPerm.get(Perm.WILDTP_LIST));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	/**
	 * Pages through configured predefined locations.
	 *
	 * @throws MassiveException if arguments are invalid
	 */
	@Override
	public void perform() throws MassiveException
	{
		final int page = this.readArg();
		MConf conf = MConf.get();

		List<WildTpPredefinedLocation> configured = conf.wildTpPredefinedLocations;
		List<WildTpPredefinedLocation> items = new ArrayList<>();
		if (configured != null)
		{
			for (WildTpPredefinedLocation loc : configured)
			{
				if (loc == null || loc.name == null || loc.name.isEmpty()) continue;
				items.add(loc);
			}
		}
		items.sort(Comparator.comparing(loc -> loc.name, String.CASE_INSENSITIVE_ORDER));

		if (items.isEmpty())
		{
			msg("<i>There are no predefined wildtp locations configured.");
			return;
		}

		final boolean canTeleport = Perm.WILDTP_LIST_TELEPORT.has(sender, false);
		final CmdFactionsWildtpGoto cmdGoto = CmdFactions.get().cmdFactionsWildtp.cmdFactionsWildtpGoto;
		final String enforcement = conf.isWildTpPredefinedRadiusSoft() ? "soft" : "strict";

		final Msonifier<WildTpPredefinedLocation> msonifier = (loc, index) -> {
			Status status = WildTpPredefinedSelector.inspect(loc, me);

			// Build the line based on the status and radius clear
			String line;
			if (status.usable && status.radiusClear)
			{
				line = Txt.parse("<lime>Valid <h>%s <i>(%s)", loc.name, loc.world == null ? "?" : loc.world);
			}
			else if (status.usable)
			{
				line = Txt.parse("<yellow>Usable <h>%s <i>(%s)", loc.name, loc.world == null ? "?" : loc.world);
			}
			else
			{
				line = Txt.parse("<rose>Invalid <h>%s <i>(%s)", loc.name, loc.world == null ? "?" : loc.world);
			}

			Mson mson = Mson.fromParsedMessage(line)
				.tooltipParse(buildTooltip(loc, status, enforcement, canTeleport));

			if (canTeleport)
			{
				mson = mson.command(cmdGoto, loc.name);
			}
			return mson;
		};

		Pager<WildTpPredefinedLocation> pager = new Pager<>(this, "Wildtp Locations", page, items, msonifier);
		pager.message();
	}

	/**
	 * Builds a multi-line hover with location details and validity.
	 *
	 * @param loc configured location
	 * @param status inspect result
	 * @param enforcement current radius enforcement mode id
	 * @param canTeleport whether the viewer may click-teleport
	 * @return tooltip string for {@link Mson#tooltipParse(String)}
	 */
	private static String buildTooltip(WildTpPredefinedLocation loc, Status status, String enforcement, boolean canTeleport)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<aqua>Name: <yellow>").append(loc.name).append('\n');
		sb.append("<aqua>World: <yellow>").append(loc.world == null || loc.world.isEmpty() ? "?" : loc.world).append('\n');
		sb.append("<aqua>Coords: <yellow>")
			.append(String.format("%.1f, %.1f, %.1f", loc.x, loc.y, loc.z)).append('\n');
		sb.append("<aqua>No-Claim Radius: <yellow>").append(Math.max(0, loc.radiusNoPlayerClaims)).append(" chunks\n");
		sb.append("<aqua>Enforcement: <yellow>").append(enforcement).append('\n');

		if (status.territory != null)
		{
			sb.append("<aqua>Territory: <yellow>").append(status.territory).append('\n');
		}

		sb.append("<aqua>Radius clear: ").append(status.radiusClear ? "<lime>yes" : "<rose>no").append('\n');

		if (status.usable)
		{
			sb.append("<aqua>Status: <lime>OK for /f wildtp");
			if (!status.radiusClear)
			{
				sb.append('\n').append("<gray>Soft fallback - claims in radius");
			}
		}
		else
		{
			sb.append("<aqua>Status: <rose>Skipped by /f wildtp\n");
			sb.append("<aqua>Reason: <yellow>").append(status.reason != null ? status.reason : "unknown");
		}

		if (canTeleport)
		{
			sb.append('\n').append("<gray>Click to teleport to this location");
		}

		return sb.toString();
	}

}
