package com.massivecraft.massivebooks.event;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class MassiveBooksPowertoolReplaceEventAbstract<T> extends MassiveBooksEventAbstractCancellable
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	// Original Bukkit Events
	
	private final PlayerInteractEvent interactEvent;
	public PlayerInteractEvent getInteractEvent() { return this.interactEvent; }
	
	private final PlayerInteractEntityEvent interactEntityEvent;
	public PlayerInteractEntityEvent getInteractEntityEvent() { return this.interactEntityEvent; }
	
	// Extracted
	
	private final Player player;
	public Player getPlayer() { return this.player; }
	
	private final Action action;
	public Action getAction() { return this.action; }
	
	private final Block blockClicked;
	public Block getBlockClicked() { return this.blockClicked; }
	
	private final BlockFace blockFace;
	public BlockFace getBlockFace() { return this.blockFace; }
	
	private final Entity entityClicked;
	public Entity getEntityClicked() { return this.entityClicked; }
	
	// Replacement
	
	public abstract T getReplacement();
	public abstract void setReplacement(T replacement);
	
	// Error
	
	private String error = null;
	public String getError() { return this.error; }
	public void setError(String error) { this.error = error; }
	public boolean hasError() { return this.error != null; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public MassiveBooksPowertoolReplaceEventAbstract(PlayerInteractEvent interactEvent, PlayerInteractEntityEvent interactEntityEvent)
	{
		this.interactEvent = interactEvent;
		this.interactEntityEvent = interactEntityEvent;
		
		// Extract
		if (interactEvent != null)
		{
			this.player = interactEvent.getPlayer();
			this.action = interactEvent.getAction();
			this.blockClicked = interactEvent.getClickedBlock();
			this.blockFace = interactEvent.getBlockFace();
			this.entityClicked = null;
		}
		else if (interactEntityEvent != null)
		{
			this.player = interactEntityEvent.getPlayer();
			this.action = null;
			this.blockClicked = null;
			this.blockFace = null;
			this.entityClicked = interactEntityEvent.getRightClicked();
		}
		else
		{
			throw new NullPointerException("It's ok for either interactEvent or interactEntityEvent to be null, but now both at the same time!");
		}
	}

}
