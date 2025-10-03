package com.massivecraft.factions.entity;

import com.massivecraft.massivecore.Named;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.ps.PSFormatHumanSpace;
import com.massivecraft.massivecore.store.EntityInternal;
import com.massivecraft.massivecore.store.EntityInternalMap;
import com.massivecraft.massivecore.util.Txt;

public class Warp extends EntityInternal<Warp> implements Named
{
	// -------------------------------------------- //
	// OVERRIDE: ENTITY
	// -------------------------------------------- //

	@Override
	public Warp load(Warp that)
	{
		this.name = that.name;
		this.location = that.location;

		return this;
	}

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private String name;
	public String getName() { return name; }
	public void setName(String name) { this.name = name; this.changed(); }

	private PS location;
	public PS getLocation() { return location; }
	public void setLocation(PS location) { this.location = location; this.changed(); }

	public String getWorld() { return this.getLocation().getWorld(); }

	public Faction getFaction()
	{
		EntityInternalMap<?> internalMap = (EntityInternalMap<?>) this.getContainer();
		return (Faction) internalMap.getEntity();
	}

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public Warp()
	{
		this(null, null);
	}

	public Warp(String name, PS location)
	{
		this.name = name;
		this.location = location;
	}

	// -------------------------------------------- //
	// VISUAL
	// -------------------------------------------- //

	public String getVisual(Object senderObject)
	{
		return Txt.parse("<h>%s<reset>:%s", this.getName(), this.getLocation().toString(PSFormatHumanSpace.get()));
	}

	// -------------------------------------------- //
	// VALID
	// -------------------------------------------- //

	public boolean verifyIsValid()
	{
		if (this.isValid()) return true;
		Faction fac = this.getFaction();
		this.detach();
		fac.msg("<b>Your faction warp <h>%s <b>has been un-set since it is no longer in your territory.", this.getName());
		return false;
	}

	public boolean isValidFor(Faction faction)
	{
		if (!MConf.get().warpsMustBeInClaimedTerritory) return true;
		if (BoardColl.get().getFactionAt(this.getLocation()) == faction) return true;
		return false;
	}

	public boolean isValid()
	{
		return this.isValidFor(this.getFaction());
	}
}
