package com.massivecraft.factions.entity;

import com.google.gson.JsonObject;
import com.massivecraft.massivecore.store.Coll;
import com.massivecraft.massivecore.store.Modification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class MPermColl extends Coll<MPerm>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static MPermColl i = new MPermColl();
	public static MPermColl get() { return i; }
	private MPermColl()
	{
		this.setLowercasing(true);
	}

	// -------------------------------------------- //
	// STACK TRACEABILITY
	// -------------------------------------------- //
	
	@Override
	public void onTick()
	{
		super.onTick();
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void setActive(boolean active)
	{
		super.setActive(active);
		if (!active) return;
		MPerm.setupStandardPerms();
	}

	private boolean initing = false;

	// Change the ids
	@Override
	public synchronized void loadFromRemoteFixed(String id, Entry<JsonObject, Long> remoteEntry)
	{
		boolean renamed = false;
		if (initing)
		{
			if ("sethome".equalsIgnoreCase(id))
			{
				id = "setwarp";
				renamed = true;
			}
			if ("home".equalsIgnoreCase(id))
			{
				id = "home";
				renamed = true;
			}
		}

		super.loadFromRemoteFixed(id, remoteEntry);
		if (renamed)
		{
			this.putIdentifiedModificationFixed(id, Modification.LOCAL_ATTACH);
			this.syncIdFixed(id);
		}
	}

	@Override
	public void initLoadAllFromRemote()
	{
		this.initing = true;
		super.initLoadAllFromRemote();
		this.removeAtRemoteFixed("sethome");
		this.removeAtRemoteFixed("home");
		this.initing = false;
	}
	
	// -------------------------------------------- //
	// EXTRAS
	// -------------------------------------------- //
	
	public List<MPerm> getAll(boolean registered)
	{
		// Create
		List<MPerm> ret = new ArrayList<>();
		
		// Fill
		for (MPerm mperm : this.getAll())
		{
			if (mperm.isRegistered() != registered) continue;
			ret.add(mperm);
		}
		
		// Return
		return ret;
	}
	
}
