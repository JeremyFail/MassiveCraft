package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.gate.GateSetting;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Read-only sectioned chat inspect for a gate (owner, network, settings as YES/NOO).
 */
public final class GateInspectUtil
{
	private GateInspectUtil()
	{
	}
	
	/**
	 * Shows full inspect if the sender may read secrets; otherwise the legacy restricted message.
	 *
	 * @param sender Viewer.
	 * @param gate Target gate.
	 * @param page Page number (1-based).
	 * @param commandForPagination Optional command for clickable page title; may be null.
	 * @return False if secret blocked the inspect.
	 */
	public static boolean show(CommandSender sender, UGate gate, int page, MassiveCommand commandForPagination)
	{
		if (sender == null || gate == null) return false;
		
		if (!EngineGateOverride.canReadSecret(sender, gate))
		{
			MixinMessage.get().messageOne(sender, Txt.parse("<b>The gate is restricted and you are not the creator."));
			return false;
		}
		
		List<Mson> lines = buildLines(gate);
		
		int pageHeight = (sender instanceof Player) ? Txt.PAGEHEIGHT_PLAYER : Txt.PAGEHEIGHT_CONSOLE;
		int pageCount = Math.max(1, (int) Math.ceil((double) lines.size() / pageHeight));
		if (page < 1 || page > pageCount)
		{
			MixinMessage.get().messageOne(sender, Txt.getMessageInvalid(pageCount));
			return true;
		}
		
		int from = (page - 1) * pageHeight;
		int to = Math.min(from + pageHeight, lines.size());
		
		List<Mson> messages = new MassiveList<>();
		MixinMessage.get().messageOne(sender, Txt.parse("<i>Some gate inscriptions are revealed:"));
		
		if (commandForPagination != null)
		{
			messages.add(Txt.titleizeMson("Gate Inspect", pageCount, page, commandForPagination, Collections.emptyList()));
		}
		else
		{
			messages.add(Txt.titleize("Gate Inspect " + page + "/" + pageCount));
		}
		messages.addAll(lines.subList(from, to));
		MixinMessage.get().messageOne(sender, messages);
		return true;
	}
	
	/**
	 * Shows inspect at page 1 without pagination command.
	 * 
	 * @param sender The viewer.
	 * @param gate The target gate.
	 * @return True if the inspect was shown.
	 */
	public static boolean show(CommandSender sender, UGate gate)
	{
		return show(sender, gate, 1, null);
	}
	
	/**
	 * Builds the lines for the inspect table.
	 * 
	 * @param gate The target gate.
	 * @return A list of Mson objects representing the lines.
	 */
	private static List<Mson> buildLines(UGate gate)
	{
		List<Mson> lines = new MassiveList<>();
		
		String ownerName = IdUtil.getName(gate.getCreatorId());
		if (ownerName == null || ownerName.isEmpty()) ownerName = "unknown";
		
		lines.add(kv("Owner", ownerName));
		lines.add(kv("Network", String.valueOf(gate.getNetworkId())));
		lines.add(kv("Gates", String.valueOf(gate.getGateChain().size())));
		
		lines.add(Mson.mson("<a>-------"));
		lines.add(settingRow(GateSetting.SECRET, gate));
		
		lines.add(Mson.mson("<a>-------"));
		lines.add(settingRow(GateSetting.ENTRY, gate));
		lines.add(settingRow(GateSetting.EXIT, gate));
		
		// page 2
		lines.add(settingRow(GateSetting.PLAYERS, gate));
		lines.add(settingRow(GateSetting.MOBS, gate));
		lines.add(settingRow(GateSetting.VEHICLES, gate));
		
		return lines;
	}
	
	/**
	 * Builds a key-value pair for the inspect table.
	 * 
	 * @param key The key.
	 * @param value The value.
	 * @return A Mson object representing the key-value pair.
	 */
	private static Mson kv(String key, String value)
	{
		return Mson.mson(Txt.parse("<k>%s: <v>%s", key, value));
	}
	
	/**
	 * Builds a row for a gate setting.
	 * 
	 * @param setting The setting to build the row for.
	 * @param gate The gate to build the row for.
	 * @return A Mson object representing the row.
	 */
	private static Mson settingRow(GateSetting setting, UGate gate)
	{
		boolean value = setting.get(gate);
		String yesNo = value ? "YES" : "NOO";
		ChatColor color = value ? ChatColor.GREEN : ChatColor.RED;
		
		return Mson.mson(
			Mson.mson(yesNo).color(color),
			Mson.SPACE,
			Mson.mson(setting.getDisplayName()).color(ChatColor.AQUA),
			Mson.SPACE,
			Mson.mson(setting.getDescription()).color(ChatColor.YELLOW)
		);
	}
	
}
