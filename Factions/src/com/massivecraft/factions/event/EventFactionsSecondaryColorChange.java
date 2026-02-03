package com.massivecraft.factions.event;

import com.massivecraft.factions.entity.Faction;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a faction's secondary color is changed.
 */
public class EventFactionsSecondaryColorChange extends EventFactionsAbstractSender
{	
	// -------------------------------------------- //
	// REQUIRED EVENT CODE
	// -------------------------------------------- //
	
	private static final HandlerList handlers = new HandlerList();
	@Override public HandlerList getHandlers() { return handlers; }
	public static HandlerList getHandlerList() { return handlers; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final Faction faction;
	public Faction getFaction() { return this.faction; }
	
	private String newColor;
	public String getNewColor() { return this.newColor; }
	public void setNewColor(String newColor) { this.newColor = newColor; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public EventFactionsSecondaryColorChange(CommandSender sender, Faction faction, String newColor)
	{
		super(sender);
		this.faction = faction;
		this.newColor = newColor;
	}
	
}
