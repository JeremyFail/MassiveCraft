package com.massivecraft.massivebooks.event;

import org.bukkit.event.Cancellable;

public abstract class MassiveBooksEventAbstractCancellable extends MassiveBooksEventAbstract implements Cancellable
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private boolean cancelled = false;
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public boolean isCancelled()
	{
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;		
	}
	
}
