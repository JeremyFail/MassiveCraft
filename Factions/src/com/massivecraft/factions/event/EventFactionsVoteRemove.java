package com.massivecraft.factions.event;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Vote;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

public class EventFactionsVoteRemove extends EventFactionsAbstractSender
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

	private Vote vote;
	public Vote getVote() { return this.vote; }
	public void setVote(Vote vote) { this.vote = vote; }

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public EventFactionsVoteRemove(CommandSender sender, Faction faction, Vote vote)
	{
		super(sender);
		this.faction = faction;
		this.vote = vote;
	}
	
}
