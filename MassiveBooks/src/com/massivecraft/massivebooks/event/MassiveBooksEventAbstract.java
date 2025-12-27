package com.massivecraft.massivebooks.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public abstract class MassiveBooksEventAbstract extends Event implements Runnable
{
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void run()
	{
		Bukkit.getPluginManager().callEvent(this);
	}
	
}