package com.massivecraft.creativegates.ui;

import com.massivecraft.creativegates.cmd.CmdCg;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.gate.GateSetting;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Factions-style clickable YES/NOO chat table for gate settings (Dialog fallback).
 */
public final class GateManageChat
{
	private GateManageChat()
	{
	}
	
	/**
	 * Opens the manage chat table at page 1 for a player.
	 * 
	 * @param player The player to open the manage chat table for.
	 * @param gate The gate to manage.
	 * @param page The page number to open.
	 */
	public static void open(Player player, UGate gate, int page)
	{
		display(player, gate, page);
	}
	
	/**
	 * Displays the paginated manage table.
	 *
	 * @param sender The viewer.
	 * @param gate The target gate.
	 * @param page The 1-based page number to display.
	 */
	public static void display(CommandSender sender, UGate gate, int page)
	{
		if (sender == null || gate == null) return;
		
		List<GateSetting> settings = Arrays.asList(GateSetting.values());
		
		String ownerName = IdUtil.getName(gate.getCreatorId());
		if (ownerName == null || ownerName.isEmpty()) ownerName = "unknown";
		String title = "Manage Gate " + gate.getNetworkId() + " (" + ownerName + ")";
		
		int pageHeight = (sender instanceof Player) ? Txt.PAGEHEIGHT_PLAYER : Txt.PAGEHEIGHT_CONSOLE;
		boolean showFill = sender instanceof Player && GateFillPicker.canChangeFill((Player) sender, gate);
		int extraLines = showFill ? 1 : 0;
		int pageCount = Math.max(1, (int) Math.ceil((double) (settings.size() + extraLines) / pageHeight));
		
		if (page < 1 || page > pageCount)
		{
			MixinMessage.get().messageOne(sender, Txt.getMessageInvalid(pageCount));
			return;
		}
		
		int from = (page - 1) * pageHeight;
		int to = Math.min(from + pageHeight, settings.size());
		
		List<Mson> messages = new MassiveList<>();
		List<String> paginationArgs = new MassiveList<>(gate.getId(), String.valueOf(page));
		messages.add(Txt.titleizeMson(title, pageCount, page, CmdCg.get().cmdCgManage, paginationArgs));
		
		if (sender instanceof Player)
		{
			messages.add(Mson.mson("(click YES/NOO to toggle)").color(ChatColor.GRAY));
		}
		
		if (from < settings.size())
		{
			List<GateSetting> pageSettings = settings.subList(from, to);
			for (GateSetting setting : pageSettings)
			{
				messages.add(buildRow(setting, gate, page));
			}
		}
		
		// Fill control sits after Vehicles; show when this page would include that "slot".
		int fillIndex = settings.size();
		if (showFill && fillIndex >= from && fillIndex < from + pageHeight)
		{
			messages.add(buildFillRow(gate));
		}
		
		MixinMessage.get().messageOne(sender, messages);
	}
	
	/**
	 * Builds a single row of the manage chat table.
	 * 
	 * @param setting The setting to build the row for.
	 * @param gate The gate to manage.
	 * @param page The page number to display.
	 * @return The built row.
	 */
	private static Mson buildRow(GateSetting setting, UGate gate, int page)
	{
		boolean value = setting.get(gate);
		String yesNo = value ? "YES" : "NOO";
		ChatColor valueColor = value ? ChatColor.GREEN : ChatColor.RED;
		
		String setValue = value ? "no" : "yes";
		String clickCommand = CmdCg.get().cmdCgManage.cmdCgManageSet.getCommandLine(
			gate.getId(),
			setting.getId(),
			setValue,
			String.valueOf(page)
		);
		
		Mson cell = Mson.mson(yesNo).color(valueColor)
			.tooltipParse(Txt.parse(value ? "<g>Enabled" : "<b>Disabled") + "\n" + Txt.parse("<i>Click to toggle."))
			.command(clickCommand);
		
		return Mson.mson(
			cell,
			Mson.SPACE,
			Mson.mson(setting.getDisplayName()).color(ChatColor.AQUA),
			Mson.SPACE,
			Mson.mson(setting.getDescription()).color(ChatColor.YELLOW)
		);
	}
	
	/**
	 * Clickable fill row: opens {@link GateFillPicker} via {@link com.massivecraft.creativegates.cmd.CmdCgManageFill}.
	 */
	private static Mson buildFillRow(UGate gate)
	{
		String fillName = GateFillPicker.currentFillLabel(gate);
		String clickCommand = CmdCg.get().cmdCgManage.cmdCgManageFill.getCommandLine(gate.getId());
		
		return Mson.mson(
			Mson.mson("Gate Fill").color(ChatColor.GREEN),
			Mson.mson(": ").color(ChatColor.GRAY),
			Mson.mson(fillName).color(ChatColor.LIGHT_PURPLE)
				.tooltipParse(Txt.parse("<i>The current gate fill block/particle. Click to modify."))
				.command(clickCommand)
		);
	}
	
}
