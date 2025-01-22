package com.massivecraft.factions;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPlayerColl;
import com.massivecraft.massivecore.collections.MassiveSet;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This Index class contains the MPlayer <--> Faction index.
 *
 * In the background it's powered by WeakHashMaps and all public methods are synchronized.
 * That should increase thread safety but no thread safety is actually guarranteed.
 * That is because the mplayer.getFaction() method is not threadsafe.
 * TODO: Something to fix in the future perhaps?
 */
public class FactionsIndex
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static FactionsIndex i = new FactionsIndex();
	public static FactionsIndex get() { return i; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final Map<MPlayer, Faction> mplayer2faction;
	private final Map<Faction, Set<MPlayer>> faction2mplayers;
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	private FactionsIndex()
	{
		this.mplayer2faction = new WeakHashMap<>();
		this.faction2mplayers = new WeakHashMapCreativeImpl();
	}
	
	// -------------------------------------------- //
	// IS CONNECTED
	// -------------------------------------------- //
	
	private boolean isConnected(MPlayer mplayer, Faction faction)
	{
		if (mplayer == null) throw new NullPointerException("mplayer");
		if (faction == null) throw new NullPointerException("faction");
		
		return mplayer.getFaction() == faction;
	}
	
	// -------------------------------------------- //
	// GET
	// -------------------------------------------- //
	
	public synchronized Faction getFaction(MPlayer mplayer)
	{
		return this.mplayer2faction.get(mplayer);
	}
	
	public synchronized Set<MPlayer> getMPlayers(Faction faction)
	{
		return new MassiveSet<>(this.faction2mplayers.get(faction));
	}
	
	// -------------------------------------------- //
	// UPDATE
	// -------------------------------------------- //
	
	public synchronized void updateAll()
	{
		if (!MPlayerColl.get().isActive()) throw new IllegalStateException("The MPlayerColl is not yet fully activated.");

		MPlayerColl.get().getAll().forEach(this::update);
	}
	
	public synchronized void update(MPlayer mplayer)
	{
		if (mplayer == null) throw new NullPointerException("mplayer");
		if (!FactionColl.get().isActive()) throw new IllegalStateException("The FactionColl is not yet fully activated.");

		// TODO: This is not optimal but here we remove a player from the index when
		// the mplayer object is deattached. Optimally it should be removed
		// automatically because it is stored as a weak reference.
		// But sometimes that doesn't happen so we do this instead.
		// Is there a memory leak somewhere?
		if (!mplayer.attached())
		{
			Faction factionIndexed = this.mplayer2faction.remove(mplayer);
			if (factionIndexed != null)
			{
				faction2mplayers.get(factionIndexed).remove(mplayer);
			}
			return;
		}
		
		Faction factionActual = mplayer.getFaction();
		Faction factionIndexed = this.getFaction(mplayer);
		
		Set<Faction> factions = new MassiveSet<>();
		if (factionActual != null) factions.add(factionActual);
		if (factionIndexed != null) factions.add(factionIndexed);
		
		for (Faction faction : factions)
		{
			boolean connected = this.isConnected(mplayer, faction);
			if (connected)
			{
				this.faction2mplayers.get(faction).add(mplayer);
			}
			else
			{
				this.faction2mplayers.get(faction).remove(mplayer);
			}
		}
		
		this.mplayer2faction.put(mplayer, factionActual);
	}
	
	public synchronized void update(Faction faction)
	{
		if (faction == null) throw new NullPointerException("faction");

		this.getMPlayers(faction).forEach(this::update);
	}
	
	// -------------------------------------------- //
	// MAP
	// -------------------------------------------- //
	
	private static abstract class WeakHashMapCreative<K, V> extends java.util.WeakHashMap<K, V>
	{
		@SuppressWarnings("unchecked")
		@Override
		public V get(Object key)
		{
			V ret = super.get(key);
			
			if (ret == null)
			{
				ret = this.createValue();
				this.put((K)key, ret);
			}
			
			return ret;
		}
		
		public abstract V createValue();
	}
	
	private static class WeakHashMapCreativeImpl extends WeakHashMapCreative<Faction, Set<MPlayer>>
	{
		@Override
		public Set<MPlayer> createValue()
		{
			return Collections.newSetFromMap(new WeakHashMap<>());
		}
	}
	
}
