package com.massivecraft.factions.util;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.CmdFactionsFlagSet;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility for building and displaying the faction flag table (YES/NOO with optional click-to-toggle).
 * Used by /f flag view, manage, and by the set command when re-displaying after a change.
 */
public final class FlagTableUtil
{
	private FlagTableUtil() { }

	// -------------------------------------------- //
	// PUBLIC API
	// -------------------------------------------- //

	public static void displayTable(CommandSender sender, Faction faction, int page, boolean management,
		MassiveCommand commandForPagination) throws MassiveException
	{
		MPlayer msender = MPlayer.get(sender);

		if (management)
		{
			boolean mayManage = mayManage(sender, faction, msender);
			if (!mayManage)
			{
				String deniedMessage = MPerm.getPermFlags().createDeniedMessage(msender, faction);
				MixinMessage.get().msgOne(sender, deniedMessage);
				return;
			}
		}

		boolean showAllFlags = !(sender instanceof Player) || (msender != null && msender.isOverriding());
		List<MFlag> flags = MFlag.getAll().stream()
			.filter(f -> showAllFlags || f.isVisible())
			.collect(Collectors.toList());

		displayTable(sender, faction, page, flags, management, commandForPagination);
	}

	public static void displayTable(CommandSender sender, Faction faction, int page, List<MFlag> flags,
		boolean management, MassiveCommand commandForPagination)
	{
		MPlayer msender = MPlayer.get(sender);

		if (flags.isEmpty())
		{
			MixinMessage.get().msgOne(sender, "<i>No flags to display.");
			return;
		}

		String titlePrefix = management ? "Manage" : "View";
		String title = titlePrefix + " Flags for " + faction.describeTo(msender);

		int pageHeight = (sender instanceof Player) ? Txt.PAGEHEIGHT_PLAYER : Txt.PAGEHEIGHT_CONSOLE;
		int pageCount = Math.max(1, (int) Math.ceil((double) flags.size() / pageHeight));

		if (page < 1 || page > pageCount)
		{
			MixinMessage.get().messageOne(sender, Txt.getMessageInvalid(pageCount));
			return;
		}

		int from = (page - 1) * pageHeight;
		int to = Math.min(from + pageHeight, flags.size());
		List<MFlag> pageFlags = flags.subList(from, to);

		List<Mson> messages = new MassiveList<>();
		List<String> paginationArgs = new MassiveList<>(faction.getId(), String.valueOf(page));
		messages.add(Txt.titleizeMson(title, pageCount, page, commandForPagination, paginationArgs));

		if (sender instanceof Player)
		{
			String hint = management ? "(click YES/NOO to toggle)" : "(hover over YES or NOO for details)";
			messages.add(Mson.mson(hint).color(ChatColor.GRAY));
		}

		for (MFlag flag : pageFlags)
		{
			boolean value = faction.getFlag(flag);
			messages.add(buildFlagRow(flag, value, faction, page, msender, management));
		}

		MixinMessage.get().messageOne(sender, messages);
	}

	// -------------------------------------------- //
	// PERMISSION
	// -------------------------------------------- //

	private static boolean mayManage(CommandSender sender, Faction faction, MPlayer msender)
	{
		return !(sender instanceof Player)
			|| Perm.FLAG_MANAGE_BYPASS.has(sender)
			|| (msender != null && msender.isOverriding())
			|| (faction == msender.getFaction() && MPerm.getPermFlags().has(msender, faction, false));
	}

	// -------------------------------------------- //
	// ROW / TOOLTIP
	// -------------------------------------------- //

	private static Mson buildFlagRow(MFlag flag, boolean value, Faction faction, int page, MPlayer msender, boolean management)
	{
		List<Mson> parts = new MassiveList<>();

		boolean canEdit = management && (flag.isEditable() || (msender != null && msender.isOverriding()));
		String yesNo = value ? "YES" : "NOO";
		ChatColor valueColor = value ? ChatColor.GREEN : ChatColor.RED;
		String tooltip = buildFlagCellTooltip(flag, value, canEdit, management);

		Mson cell = Mson.mson(yesNo).color(valueColor).tooltipParse(tooltip);
		if (canEdit)
		{
			String setValue = value ? "no" : "yes";
			String clickCommandLine = CmdFactionsFlagSet.buildSetCommandLine(flag.getId(), setValue, faction.getId(), "manage", String.valueOf(page));
			cell = cell.command(clickCommandLine);
		}
		parts.add(cell);
		parts.add(Mson.SPACE);

		ChatColor nameColor = !flag.isVisible() ? ChatColor.GRAY : (flag.isEditable() ? ChatColor.AQUA : ChatColor.LIGHT_PURPLE);
		parts.add(Mson.mson(flag.getName()).color(nameColor));
		parts.add(Mson.SPACE);
		parts.add(Mson.mson(flag.getDesc()).color(ChatColor.YELLOW));

		return Mson.mson(parts);
	}

	private static String buildFlagCellTooltip(MFlag flag, boolean value, boolean canEdit, boolean management)
	{
		StringBuilder sb = new StringBuilder();
		if (value)
		{
			sb.append("<g>").append(flag.getDescYes());
		}
		else
		{
			sb.append("<b>").append(flag.getDescNo());
		}

		if (management && canEdit)
		{
			sb.append(value ? "\n<aqua>Click to turn off" : "\n<aqua>Click to turn on");
		}

		if (!flag.isEditable())
		{
			if (canEdit)
			{
				sb.append("\n<gold>Editable in override mode");
			}
			else
			{
				sb.append("\n<pink>Not editable");
			}
		}

		if (!flag.isVisible())
		{
			sb.append("\n<silver>Not visible to players");
		}

		return sb.toString();
	}
}
