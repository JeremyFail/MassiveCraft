package com.massivecraft.factions.entity;

import com.massivecraft.massivecore.Named;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.store.EntityInternal;
import com.massivecraft.massivecore.store.EntityInternalMap;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Vote extends EntityInternal<Vote> implements Named
{
	// -------------------------------------------- //
	// OVERRIDE: ENTITY
	// -------------------------------------------- //

	@Override
	public Vote load(Vote that)
	{
		this.creatorId = that.creatorId;
		this.creationMillis = that.creationMillis;
		this.options = that.options;
		this.id2Vote = that.id2Vote;

		return this;
	}

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private String creatorId;
	public String getCreatorId() { return this.creatorId; }

	private long creationMillis;
	public long getCreationMillis() { return this.creationMillis; }

	private String name;
	@Override public String getName() { return this.name; }

	private List<String> options;
	public List<String> getOptions() { return this.options; }

	private Map<String, String> id2Vote;
	public Map<String, String> getId2Vote() { return id2Vote; }

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	// For GSON
	private Vote()
	{
		this(null, 0, null, null);
	}

	public Vote(String creatorId, String name, List<String> options)
	{
		this(creatorId, System.currentTimeMillis(), name, options);
	}

	public Vote(String creatorId, long creationMillis, String name, List<String> options)
	{
		this.creatorId = creatorId;
		this.creationMillis = creationMillis;
		this.name = name;
		this.options = options;
		this.id2Vote = new MassiveMap<>();
	}

	// -------------------------------------------- //
	// OTHER
	// -------------------------------------------- //

	public void setVote(MPlayer mplayer, String choice)
	{
		if (mplayer == null) throw new NullPointerException("mplayer");
		if (choice == null) throw new NullPointerException("choice");

		if (!this.getOptions().contains(choice)) throw new IllegalArgumentException(choice + " is not in " + this.getOptions());

		id2Vote.put(mplayer.getId(), choice);
		this.changed();
	}

	public String getVote(MPlayer mplayer)
	{
		if (mplayer == null) throw new NullPointerException("mplayer");

		return this.getId2Vote().get(mplayer.getId());
	}

	public Faction getFaction()
	{
		EntityInternalMap<Vote> internalMap = (EntityInternalMap<Vote>) this.getContainer();
		return (Faction) internalMap.getEntity();
	}

	public void clean()
	{
		Faction faction = this.getFaction();
		for (Iterator<Entry<String, String>> it = this.id2Vote.entrySet().iterator(); it.hasNext();)
		{
			Entry<String, String> entry = it.next();
			String id = entry.getKey();

			// MPlayer must be a member
			if ( ! faction.getMPlayerIds().contains(id))
			{
				it.remove();
				break;
			}

			// And they must have the perm
			MPlayer mplayer = MPlayer.get(id);
			if (! MPerm.getPermVote().has(mplayer, faction, false))
			{
				it.remove();
				break;
			}
		}
	}
	
}
