package com.massivecraft.massivebooks;

import com.massivecraft.massivebooks.event.MassiveBooksPowertoolReplaceLineEvent;
import com.massivecraft.massivebooks.event.MassiveBooksPowertoolReplaceLinesEvent;
import com.massivecraft.massivebooks.event.MassiveBooksPowertoolReplaceTagEvent;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.mixin.MixinDisplayName;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnginePowertool extends Engine
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	public final static String START = "{";
	public final static String END = "}";
	
	public final static String ESC_START = "\\"+START;
	public final static String ESC_END = "\\"+END;
	
	public final static Pattern pattern = Pattern.compile(ESC_START+"([^"+ESC_START+ESC_END+"]+)"+ESC_END);
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final EnginePowertool i = new EnginePowertool();
	public static EnginePowertool get() { return i; }
	
	// -------------------------------------------- //
	// LISTENER: INTERACT EVENTS
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.LOW)
	public void handleInteractEvent(PlayerInteractEvent interactEvent)
	{
		if (interactEvent.getAction() != Action.LEFT_CLICK_AIR && interactEvent.getAction() != Action.LEFT_CLICK_BLOCK) return;
		this.handleInteractEvent(interactEvent, null);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void handleInteractEvent(PlayerInteractEntityEvent interactEntityEvent)
	{
		// Ignore Off Hand
		if (isOffHand(interactEntityEvent)) return;
		
		// TODO: Do we even want to use this? Perhaps a sniper-edit search implementation will be better? 
		this.handleInteractEvent(null, interactEntityEvent);
	}
	
	public void handleInteractEvent(final PlayerInteractEvent interactEvent, final PlayerInteractEntityEvent interactEntityEvent)
	{
		// Get some data
		final Player player;
		final Cancellable cancellable;
		if (interactEvent != null)
		{
			player = interactEvent.getPlayer();
			cancellable = interactEvent;
		}
		else if (interactEntityEvent != null)
		{
			player = interactEntityEvent.getPlayer();
			cancellable = interactEntityEvent;
		}
		else
		{
			throw new NullPointerException("It's ok for either interactEvent or interactEntityEvent to be null, but not both at the same time!");
		}
		
		// If the player is holding a written book ...
		final ItemStack item = InventoryUtil.getMainHand(player);
		if (item == null) return;
		final Material itemType = item.getType();
		if (itemType != Material.WRITTEN_BOOK) return;
		
		// ... and that written book is a powertool ...
		if (!BookUtil.containsFlag(item, Const.POWERTOOL)) return;
		
		// ... cancel the event since we are trying to use the powertool for sure ...
		cancellable.setCancelled(true);
		
		// ... extract the rawlines from the powertool ...
		List<String> rawlines = getRawlines(item);
		
		// ... run the replace event on the lines ...
		MassiveBooksPowertoolReplaceLinesEvent event = new MassiveBooksPowertoolReplaceLinesEvent(interactEvent, interactEntityEvent, rawlines);
		event.run();
		
		// ... did we have an error? ...
		if (event.getError() != null)
		{
			MixinMessage.get().messageOne(player, event.getError());
			return;
		}
		
		// ... how nice! now lets try to run these chats/commands ...
		List<String> lines = event.getReplacement();
		int lineIndex = 0;
		for (String line : lines)
		{
			lineIndex++;
			if (!shouldLineBeExecuted(line)) continue;
			try
			{
				player.chat(line);
				MixinMessage.get().messageOne(player, Lang.getPowertoolRan(lineIndex, line));
			}
			catch (Exception e)
			{
				MixinMessage.get().messageOne(player, Lang.getPowertoolRan(lineIndex, line, e.getMessage()));
				break;
			}
		}
	}
	
	// -------------------------------------------- //
	// LISTENER: REPLACE LINES/LINE
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onReplaceLinesEventNormal(MassiveBooksPowertoolReplaceLinesEvent event)
	{
		List<String> replacement = new ArrayList<>();
		int lineIndex = 0;
		for (String line : event.getLines())
		{
			lineIndex++;
			MassiveBooksPowertoolReplaceLineEvent innerEvent = new MassiveBooksPowertoolReplaceLineEvent(event.getInteractEvent(), event.getInteractEntityEvent(), line);
			innerEvent.run();
			if (innerEvent.hasError())
			{
				event.setError(Lang.getPowertoolIssueAtLine(lineIndex, innerEvent.getError()));
				event.setCancelled(true);
				return;
			}
			replacement.add(innerEvent.getReplacement());
		}
		event.setReplacement(replacement);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onReplaceLinesEvent(MassiveBooksPowertoolReplaceLinesEvent event)
	{
		if (event.hasError()) return;
		if (!shouldAnyLineBeExecuted(event.getReplacement()))
		{
			event.setError(Lang.POWERTOOL_NO_RUNNABLE_LINES);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onReplaceLineEvent(MassiveBooksPowertoolReplaceLineEvent event)
	{
		// We build the replacement in this string buffer
		StringBuffer replacementBuffer = new StringBuffer();
		
		// A matcher to match all the tags in the line
		Matcher matcher = pattern.matcher(event.getLine());

		// For each tag we find
		while (matcher.find())
		{
			// The fullmatch is something like "{me.name}"
			//String fullmatch = matcher.group(0);
			
			// The tag is something like "me.name"
			String tag = matcher.group(1);
			
			// Run the tag-replace-event
			MassiveBooksPowertoolReplaceTagEvent tagEvent = new MassiveBooksPowertoolReplaceTagEvent(event.getInteractEvent(), event.getInteractEntityEvent(), tag);
			tagEvent.run();
			
			// I can haz replacement?
			String tagReplacement = tagEvent.getReplacement();
			
			// No replacement?
			if (tagReplacement == null && !tagEvent.hasError())
			{
				tagEvent.setError(Lang.getPowertoolUnknownTag(tag));
			}
			
			// Did we fail
			if (tagEvent.hasError())
			{
				event.setError(tagEvent.getError());
				event.setCancelled(true);
				return;
			}
			
			// Append the replacement
			matcher.appendReplacement(replacementBuffer, tagReplacement);
		}
		
		// Append the rest
		matcher.appendTail(replacementBuffer);
		
		// And finally we set the replacement
		event.setReplacement(replacementBuffer.toString());
	}
	
	// -------------------------------------------- //
	// LISTENER: REPLACE TAGS
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onReplaceTagEvent(MassiveBooksPowertoolReplaceTagEvent event)
	{
		Player me;
		Player you;
		Block block;
		final String tag = event.getTag();
		
		
		switch (tag)
		{
			case "me.name":
				me = event.getPlayer();
				event.setReplacement(me.getName());
				break;
			case "me.id":
				me = event.getPlayer();
				event.setReplacement(String.valueOf(me.getEntityId()));
				break;
			case "me.displayname":
				me = event.getPlayer();
				event.setReplacement(MixinDisplayName.get().getDisplayName(me, me));
				break;
			case "you.name":
				you = IdUtil.getAsPlayer(event.getEntityClicked());
				if (you == null)
				{
					event.setError(Lang.POWERTOOL_REQUIRES_YOU);
				}
				else
				{
					event.setReplacement(you.getName());
				}
				break;
			case "you.id":
				you = IdUtil.getAsPlayer(event.getEntityClicked());
				if (you == null)
				{
					event.setError(Lang.POWERTOOL_REQUIRES_YOU);
				}
				else
				{
					event.setReplacement(String.valueOf(you.getEntityId()));
				}
				break;
			case "you.displayname":
				me = event.getPlayer();
				you = IdUtil.getAsPlayer(event.getEntityClicked());
				if (you == null)
				{
					event.setError(Lang.POWERTOOL_REQUIRES_YOU);
				}
				else
				{
					event.setReplacement(MixinDisplayName.get().getDisplayName(you, me));
				}
				break;
			case "block.x":
				block = event.getBlockClicked();
				if (block == null)
				{
					event.setError(Lang.POWERTOOL_REQUIRES_BLOCK);
				}
				else
				{
					event.setReplacement(String.valueOf(block.getX()));
				}
				break;
			case "block.y":
				block = event.getBlockClicked();
				if (block == null)
				{
					event.setError(Lang.POWERTOOL_REQUIRES_BLOCK);
				}
				else
				{
					event.setReplacement(String.valueOf(block.getY()));
				}
				break;
			case "block.z":
				block = event.getBlockClicked();
				if (block == null)
				{
					event.setError(Lang.POWERTOOL_REQUIRES_BLOCK);
				}
				else
				{
					event.setReplacement(String.valueOf(block.getZ()));
				}
				break;
		}
	}
	
	// -------------------------------------------- //
	// UTILITIES AND SUB-LOGIC
	// -------------------------------------------- //
	
	public static List<String> getRawlines(ItemStack item)
	{
		List<String> ret = new MassiveList<>();
		
		List<String> pages = BookUtil.getPages(item);
		if (pages == null) return ret;
		
		for (String page : pages)
		{
			if (page == null) continue;
			for (String line : Txt.PATTERN_NEWLINE.split(page))
			{
				line = line.trim();
				// There seems to be a bug in Spigot 1.9
				// This bug adds black colors around newlines.
				// Since colors should not be part of chat window output
				// We just strip all colors from each line.
				line = ChatColor.stripColor(line);
				ret.add(line);
			}
		}
		
		return ret;
	}
	
	public static boolean shouldLineBeExecuted(String line)
	{
		if (line == null) return false;
		if (line.length() == 0) return false;
		if (line.startsWith("#")) return false;
		return true;
	}
	
	public static boolean shouldAnyLineBeExecuted(List<String> lines)
	{
		if (lines == null) return false;
		for (String line : lines)
		{
			if (shouldLineBeExecuted(line)) return true;
		}
		return false;
	}
	
}
