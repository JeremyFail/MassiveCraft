package com.massivecraft.creativegates.ui;

import com.massivecraft.creativegates.cmd.CmdCg;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.gate.GateSetting;
import com.massivecraft.creativegates.util.GateChainReorder;
import com.massivecraft.creativegates.util.GateNetworkRename;
import com.massivecraft.creativegates.util.GateScopeUtil;
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
		boolean showScopeAction = sender instanceof Player && GateScopeUtil.canChangeGateScope((Player) sender, gate);
		boolean showNetworkAction = sender instanceof Player;
		boolean showReorder = sender instanceof Player && GateChainReorder.canReorder((Player) sender, gate);
		// Scope/Network/Chain Order move to action rows when editable; keep them in identity otherwise.
		int identityLinesOnPage1 = 2 // creator, owner
			+ (showScopeAction ? 0 : 1)
			+ (showNetworkAction ? 0 : 1)
			+ (showReorder ? 0 : 1);
		int actionLines = (showScopeAction ? 1 : 0) + (showNetworkAction ? 1 : 0) + (showReorder ? 2 : 0);
		int hintLines = (sender instanceof Player) ? 1 : 0;
		int fillLines = showFill ? 1 : 0;
		int totalContent = identityLinesOnPage1 + actionLines + hintLines + settings.size() + fillLines;
		int pageCount = Math.max(1, (int) Math.ceil((double) totalContent / pageHeight));
		
		if (page < 1 || page > pageCount)
		{
			MixinMessage.get().messageOne(sender, Txt.getMessageInvalid(pageCount));
			return;
		}
		
		List<Mson> content = new MassiveList<>();
		List<UGate> chain = UGateColl.get().getGateChain(gate);
		int chainPos = chain.indexOf(gate) + 1;
		int chainSize = chain.size();
		content.add(Mson.mson(Txt.parse("<k>Creator: <v>%s", ownerName)));
		if (!showScopeAction)
		{
			content.add(Mson.mson(Txt.parse("<k>Scope: <v>%s", gate.getScopeType().name())));
		}
		content.add(Mson.mson(Txt.parse("<k>Network Owner: <v>%s", GateScopeUtil.describeOwner(gate.getScopeType(), gate.getNetworkOwnerId()))));
		if (!showNetworkAction)
		{
			content.add(Mson.mson(Txt.parse("<k>Network: <v>%s", String.valueOf(gate.getNetworkId()))));
		}
		if (!showReorder)
		{
			content.add(Mson.mson(Txt.parse(
				"<k>Chain Order: <v>%s",
				chainPos > 0 ? (chainPos + "/" + chainSize) : String.valueOf(chainSize)
			)));
		}
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			if (showScopeAction)
			{
				content.add(buildScopeCycleRow(player, gate));
			}
			content.add(buildNetworkSuggestRow(player, gate));
			if (showReorder)
			{
				content.add(buildReorderRow(gate, true, chainPos, chainSize));
				content.add(buildReorderRow(gate, false, chainPos, chainSize));
			}
			content.add(Mson.mson("(click YES/NOO to toggle; access via /cg manage access)").color(ChatColor.GRAY));
		}
		for (GateSetting setting : settings)
		{
			content.add(buildRow(setting, gate, page));
		}
		if (showFill)
		{
			content.add(buildFillRow(gate));
		}
		
		int from = (page - 1) * pageHeight;
		int to = Math.min(from + pageHeight, content.size());
		
		List<Mson> messages = new MassiveList<>();
		List<String> paginationArgs = new MassiveList<>(gate.getId(), String.valueOf(page));
		messages.add(Txt.titleizeMson(title, pageCount, page, CmdCg.get().cmdCgManage, paginationArgs));
		messages.addAll(content.subList(from, to));
		
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
	 * Builds the scope cycle row.
	 * 
	 * @param player The player who is viewing the scope cycle row.
	 * @param gate The gate that is being viewed.
	 * @return The scope cycle row.
	 */
	private static Mson buildScopeCycleRow(Player player, UGate gate)
	{
		GateScopeType next = GateScopeUtil.nextOfferedScope(player, gate.getScopeType());
		Mson value = Mson.mson(gate.getScopeType().name()).color(ChatColor.YELLOW);
		if (next != null && next != gate.getScopeType())
		{
			value = value
				.tooltipParse(Txt.parse("<i>Click to change scope to <h>%s<i>.", next.name()))
				.command(CmdCg.get().cmdCgManage.cmdCgManageScope.getCommandLine(
					next.name(),
					"auto",
					gate.getId()
				));
		}
		else
		{
			value = value.tooltipParse(Txt.parse("<i>No other scopes available to cycle."));
		}
		return Mson.mson(
			Mson.mson("Scope").color(ChatColor.AQUA),
			Mson.mson(": ").color(ChatColor.GRAY),
			value
		);
	}
	
	/**
	 * Builds the network suggest row.
	 * 
	 * @param player The player who is viewing the network suggest row.
	 * @param gate The gate that is being viewed.
	 * @return The network suggest row.
	 */
	private static Mson buildNetworkSuggestRow(Player player, UGate gate)
	{
		Mson value = Mson.mson(String.valueOf(gate.getNetworkId())).color(ChatColor.LIGHT_PURPLE)
			.tooltipParse(GateNetworkRename.costTooltip(player));
		if (GateNetworkRename.canAfford(player))
		{
			// Suggest the command prefix only; looking-gate supplies the target. Trailing space for typing the new name.
			String suggest = CmdCg.get().cmdCgManage.cmdCgManageNetwork.getCommandLine() + " ";
			value = value
				.tooltipParse(GateNetworkRename.costTooltip(player) + "\n" + Txt.parse("<i>Click to type a new name."))
				.suggest(suggest);
		}
		return Mson.mson(
			Mson.mson("Network").color(ChatColor.GREEN),
			Mson.mson(": ").color(ChatColor.GRAY),
			value
		);
	}
	
	/**
	 * Builds the reorder row.
	 * 
	 * @param gate The gate that is being viewed.
	 * @param up Whether the gate should be moved up.
	 * @param chainPos The position of the gate in the chain.
	 * @param chainSize The size of the chain.
	 * @return The reorder row.
	 */
	private static Mson buildReorderRow(UGate gate, boolean up, int chainPos, int chainSize)
	{
		String mode = up ? "up" : "down";
		String label = up ? "Move Earlier (+)" : "Move Later (-)";
		String clickCommand = CmdCg.get().cmdCgManage.cmdCgManageReorder.getCommandLine(mode, "0", gate.getId());
		String tip = Txt.parse(
			"<k>Chain Order: <h>%d/%d\n",
			chainPos > 0 ? chainPos : 0,
			chainSize
		) + Txt.parse(up ? "<i>Move earlier in the travel chain." : "<i>Move later in the travel chain.");
		return Mson.mson(
			Mson.mson(label).color(up ? ChatColor.GREEN : ChatColor.GRAY)
				.tooltipParse(tip)
				.command(clickCommand)
		);
	}
	
	/**
	 * Clickable fill row: opens {@link GateFillPicker} via {@link com.massivecraft.creativegates.cmd.CmdCgManageFill}.
	 * 
	 * @param gate The gate that is being viewed.
	 * @return The fill row.
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
