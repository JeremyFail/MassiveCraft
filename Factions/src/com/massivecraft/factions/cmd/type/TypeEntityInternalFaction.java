package com.massivecraft.factions.cmd.type;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.command.type.TypeAbstractChoice;
import com.massivecraft.massivecore.store.EntityInternal;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;


public abstract class TypeEntityInternalFaction<E extends EntityInternal<E>> extends TypeAbstractChoice<E>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	protected TypeEntityInternalFaction(Class<E> clazz)
	{
		super(clazz);

		this.faction = null;
		this.setAll(Collections.emptyList());
	}

	protected TypeEntityInternalFaction(Class<E> clazz, Faction faction)
	{
		super(clazz);
		if (faction == null) throw new NullPointerException("faction");

		this.faction = faction;

		this.setAll(this.getAll(faction));
	}

	protected abstract Collection<E> getAll(Faction faction);
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private final Faction faction;
	public Faction getFaction() { return this.faction; }

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public boolean isValid(String arg, CommandSender sender)
	{
		// In the generic case accept all
		if (this.getAll().isEmpty()) return true;
		else return super.isValid(arg, sender);
	}

	@Override
	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		if (this.getFaction() != null) return super.getTabList(sender, arg);

		// Default to tab list for the sender
		Faction faction = MPlayer.get(sender).getFaction();

		// Create
		Set<String> ret = new MassiveSet<>();

		// Fill
		for (E value : this.getAll(faction))
		{
			ret.addAll(this.createTabs(value));
		}

		// Return
		return ret;
	}

}
