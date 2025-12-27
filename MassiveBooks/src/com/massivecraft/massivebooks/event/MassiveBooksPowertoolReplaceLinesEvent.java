package com.massivecraft.massivebooks.event;

import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class MassiveBooksPowertoolReplaceLinesEvent extends MassiveBooksPowertoolReplaceEventAbstract<List<String>>
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
	
	private final List<String> lines;
	public List<String> getLines() { return this.lines; }
	
	private List<String> replacement = null;
	@Override public List<String> getReplacement() { return this.replacement; }
	@Override public void setReplacement(List<String> replacement) { this.replacement = replacement; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public MassiveBooksPowertoolReplaceLinesEvent(PlayerInteractEvent interactEvent, PlayerInteractEntityEvent interactEntityEvent, List<String> lines)
	{
		super(interactEvent, interactEntityEvent);
		this.lines = lines;
	}

}
