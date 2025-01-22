package com.massivecraft.factions.event;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Warp;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

public class EventFactionsWarpRemove extends EventFactionsAbstractSender
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

	private Warp warp;
	public Warp getWarp() { return this.warp; }
	public void setWarp(Warp warp) { this.warp = warp; }

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public EventFactionsWarpRemove(CommandSender sender, Faction faction, Warp warp)
	{
		super(sender);
		this.faction = faction;
		this.warp = warp;
	}
	
}
