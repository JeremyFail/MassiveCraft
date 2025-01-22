package com.massivecraft.factions.event;

import com.massivecraft.factions.entity.Warp;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

public class EventFactionsWarpTeleport extends EventFactionsAbstractSender
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

	private final Warp warp;
	public Warp getWarp() { return this.warp; }

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public EventFactionsWarpTeleport(CommandSender sender, Warp warp)
	{
		super(sender);
		this.warp = warp;
	}
	
}
