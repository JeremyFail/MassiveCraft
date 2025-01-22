package com.massivecraft.factions.entity;

import com.massivecraft.massivecore.store.EntityInternal;
import com.massivecraft.massivecore.store.EntityInternalMap;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;

import java.util.Map.Entry;
import java.util.Set;

public class Rank extends EntityInternal<Rank> implements MPerm.MPermable
{
	// -------------------------------------------- //
	// OVERRIDE: ENTITY
	// -------------------------------------------- //

	@Override
	public Rank load(Rank that)
	{
		this.name = that.name;

		return this;
	}

	@Override
	public void preDetach(String id)
	{
		for (Faction f : FactionColl.get().getAll())
		{
			for (Entry<String, Set<String>> entry : f.getPerms().entrySet())
			{
				Set<String> value = entry.getValue();
				if (value == null) throw new NullPointerException(entry.getKey());
				value.remove(id);
			}
		}
	}

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private String name;
	public String getName() { return this.name; }
	public void setName(String name) { this.name = name; this.changed(); }

	private int priority;
	public int getPriority() { return this.priority; }
	public void setPriority(int priority) { this.priority = priority; this.changed(); }

	private String prefix;
	public String getPrefix() { return this.prefix; }
	public void setPrefix(String prefix) { this.prefix = prefix; this.changed(); }

	public Faction getFaction()
	{
		EntityInternalMap<Rank> internalMap = (EntityInternalMap<Rank>) this.getContainer();
		return (Faction) internalMap.getEntity();
	}

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	// For GSON
	private Rank()
	{
		this(null,0, "");
	}

	public Rank(String name, int priority, String prefix)
	{
		this.name = name;
		this.priority = priority;
		this.prefix = prefix;
	}

	public Rank copy()
	{
		return new Rank(this.name, this.priority, this.prefix);
	}

	// -------------------------------------------- //
	// VISUAL
	// -------------------------------------------- //

	@Override
	public String getDisplayName(Object senderObject)
	{
		String ret = this.getVisual();
		ret += Txt.parse(" of ") + this.getFaction().getDisplayName(senderObject);
		return ret;
	}

	public String getVisual()
	{
		String ret = "";
		ret += ChatColor.GREEN.toString();
		ret += this.getPrefix();
		ret += this.getName();
		ret += " (" + this.getPriority() + ")";
		return ret;
	}

	// -------------------------------------------- //
	// RANK PRIORITY
	// -------------------------------------------- //

	public boolean isLessThan(Rank otherRank)
	{
		if (this.getContainer() != otherRank.getContainer()) throw new IllegalArgumentException(this.getId() + " : " + otherRank.getId());

		return this.getPriority() < otherRank.getPriority();
	}

	public boolean isMoreThan(Rank otherRank)
	{
		if (this.getContainer() != otherRank.getContainer()) throw new IllegalArgumentException(this.getId() + " : " + otherRank.getId());

		return this.getPriority() > otherRank.getPriority();
	}

	public boolean isAtLeast(Rank otherRank)
	{
		if (this.getContainer() != otherRank.getContainer()) throw new IllegalArgumentException(this.getId() + " : " + otherRank.getId());

		return this.getPriority() >= otherRank.getPriority();
	}

	public boolean isAtMost(Rank otherRank)
	{
		if (this.getContainer() != otherRank.getContainer()) throw new IllegalArgumentException(this.getId() + " : " + otherRank.getId());

		return this.getPriority() <= otherRank.getPriority();
	}

	public boolean isLeader()
	{
		for (Rank otherRank : this.getContainer().getAll())
		{
			if (otherRank == this) continue;

			if (otherRank.isMoreThan(this)) return false;
		}
		return true;
	}

	public Rank getRankAbove()
	{
		Rank ret = null;
		for (Rank otherRank : this.getContainer().getAll())
		{
			if (otherRank == this) continue;
			if (otherRank.isLessThan(this)) continue;
			if (ret != null && ret.isLessThan(otherRank)) continue;

			ret = otherRank;
		}
		return ret;
	}

	public Rank getRankBelow()
	{
		Rank ret = null;
		for (Rank otherRank : this.getContainer().getAll())
		{
			if (otherRank == this) continue;
			if (otherRank.isMoreThan(this)) continue;
			if (ret != null && ret.isMoreThan(otherRank)) continue;

			ret = otherRank;
		}
		return ret;
	}

}
