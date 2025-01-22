package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPermColl;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.function.Predicate;

public class CmdFactionsPermList extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsPermList()
	{
		// Parameters
		this.addParameter(Parameter.getPage());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Parameter
		int page = this.readArg();
		
		// Pager create
		String title = String.format("Perms for %s", msenderFaction.describeTo(msender));
		final Pager<MPerm> pager = new Pager<>(this, title, page, (Stringifier<MPerm>) (mp, i) -> mp.getDesc(true, true));
		final Predicate<MPerm> predicate = msender.isOverriding() ? null : MPerm::isVisible;
		
		Bukkit.getScheduler().runTaskAsynchronously(Factions.get(), () -> {
			// Get items
			List<MPerm> items = MPermColl.get().getAll(predicate);

			// Pager items
			pager.setItems(items);

			// Pager message
			pager.message();
		});
	}

}
