package com.massivecraft.massivebooks.event;

import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MassiveBooksPowertoolReplaceLineEvent extends MassiveBooksPowertoolReplaceEventAbstract<String>
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
	
	private final String line;
	public String getLine() { return this.line; }
	
	private String replacement = null;
	@Override public String getReplacement() { return this.replacement; }
	@Override public void setReplacement(String replacement) { this.replacement = replacement; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public MassiveBooksPowertoolReplaceLineEvent(PlayerInteractEvent interactEvent, PlayerInteractEntityEvent interactEntityEvent, String line)
	{
		super(interactEvent, interactEntityEvent);
		this.line = line;
	}

}
