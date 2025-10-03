package com.massivecraft.factions.entity;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.Entity;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Board extends Entity<Board> implements BoardInterface
{
	public static final transient Type MAP_TYPE = new TypeToken<Map<PS, TerritoryAccess>>(){}.getType();
	
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	public static Board get(Object oid)
	{
		return BoardColl.get().get(oid);
	}
	
	// -------------------------------------------- //
	// OVERRIDE: ENTITY
	// -------------------------------------------- //
	
	@Override
	public Board load(Board that)
	{
		this.map = that.map;
		
		return this;
	}
	
	@Override
	public boolean isDefault()
	{
		if (this.map == null) return true;
		if (this.map.isEmpty()) return true;
		return false;
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private ConcurrentSkipListMap<PS, TerritoryAccess> map;
	public Map<PS, TerritoryAccess> getMap() { return Collections.unmodifiableMap(this.map); }
	public Map<PS, TerritoryAccess> getMapRaw() { return this.map; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public Board()
	{
		this.map = new ConcurrentSkipListMap<>();
	}
	
	public Board(Map<PS, TerritoryAccess> map)
	{
		this.map = new ConcurrentSkipListMap<>(map);
	}
	
	// -------------------------------------------- //
	// OVERRIDE: BOARD
	// -------------------------------------------- //
	
	// GET
	
	@Override
	public TerritoryAccess getTerritoryAccessAt(PS ps)
	{
		if (ps == null) throw new NullPointerException("ps");

		ps = ps.getChunkCoords(true);
		TerritoryAccess ret = this.map.get(ps);
		if (ret == null || ret.getHostFaction() == null) ret = TerritoryAccess.valueOf(Factions.ID_NONE);
		return ret;
	}
	
	@Override
	public Faction getFactionAt(PS ps)
	{
		return this.getTerritoryAccessAt(ps).getHostFaction();
	}
	
	// SET
	
	@Override
	public void setTerritoryAccessAt(PS ps, TerritoryAccess territoryAccess)
	{
		ps = ps.getChunkCoords(true);
		
		if (territoryAccess == null || (territoryAccess.getHostFactionId().equals(Factions.ID_NONE) && territoryAccess.isDefault()))
		{	
			this.map.remove(ps);
		}
		else
		{
			this.map.put(ps, territoryAccess);
		}
		
		this.changed();
	}
	
	@Override
	public void setFactionAt(PS ps, Faction faction)
	{
		TerritoryAccess territoryAccess = null;
		if (faction != null)
		{
			territoryAccess = TerritoryAccess.valueOf(faction.getId());
		}
		this.setTerritoryAccessAt(ps, territoryAccess);
	}
	
	// REMOVE
	
	@Override
	public void removeAt(PS ps)
	{
		this.setTerritoryAccessAt(ps, null);
	}
	
	@Override
	public void removeAll(Faction faction)
	{
		this.getChunks(faction).forEach(this::removeAt);
	}
	
	// CHUNKS
	
	@Override
	public Set<PS> getChunks(Faction faction)
	{
		return this.getChunks(faction.getId());
	}
	
	@Override
	public Set<PS> getChunks(String factionId)
	{
		return this.map.entrySet().stream()
			.filter(e -> e.getValue().getHostFactionId().equals(factionId))
			.map(Entry::getKey)
			.map(ps -> ps.withWorld(this.getId()))
			.collect(Collectors.toSet());
	}

	@Override
	@Deprecated
	public Map<Faction, Set<PS>> getFactionToChunks()
	{
		return this.getFactionToChunks(true);
	}
	
	@Override
	public Map<Faction, Set<PS>> getFactionToChunks(boolean withWorld)
	{
		Function<Entry<PS, TerritoryAccess>, PS> mapper = Entry::getKey;
		if (withWorld) mapper = mapper.andThen(ps -> ps.withWorld(this.getId()));

		return map.entrySet().stream().collect(Collectors.groupingBy(
			entry -> entry.getValue().getHostFaction(), // This specifies how to get the key
			Collectors.mapping(mapper, Collectors.toSet()) // This maps the entries and puts them in the collection
		));
	}

	@Override
	public Map<String, Map<Faction, Set<PS>>> getWorldToFactionToChunks(boolean withWorld)
	{
		return Collections.singletonMap(this.getId(), this.getFactionToChunks(withWorld));
	}
	
	// COUNT
	
	@Override
	public int getCount(Faction faction)
	{
		if (faction == null) throw new NullPointerException("faction");

		return this.getCount(faction.getId());
	}
	
	@Override
	public int getCount(String factionId)
	{
		if (factionId == null) throw new NullPointerException("factionId");

		return (int) this.map.values().stream()
				   .map(TerritoryAccess::getHostFactionId)
				   .filter(factionId::equals)
				   .count();
	}
	
	@Override
	public Map<Faction, Long> getFactionToCount()
	{
		return this.map.entrySet().stream()
			.collect(Collectors.groupingBy(
				e -> e.getValue().getHostFaction(),
				Collectors.counting()
			));
	}
	
	// CLAIMED
	
	@Override
	public boolean hasClaimed(Faction faction)
	{
		return this.hasClaimed(faction.getId());
	}
	
	@Override
	public boolean hasClaimed(String factionId)
	{
		return this.map.values().stream()
			.map(TerritoryAccess::getHostFactionId)
			.anyMatch(factionId::equals);
	}
	
	// NEARBY DETECTION
		
	// Is this coord NOT completely surrounded by coords claimed by the same faction?
	// Simpler: Is there any nearby coord with a faction other than the faction here?
	@Override
	public boolean isBorderPs(PS ps)
	{
		ps = ps.getChunk(true);
		
		PS nearby = null;
		Faction faction = this.getFactionAt(ps);
		
		nearby = ps.withChunkX(ps.getChunkX() +1);
		if (faction != this.getFactionAt(nearby)) return true;
		
		nearby = ps.withChunkX(ps.getChunkX() -1);
		if (faction != this.getFactionAt(nearby)) return true;
		
		nearby = ps.withChunkZ(ps.getChunkZ() +1);
		if (faction != this.getFactionAt(nearby)) return true;
		
		nearby = ps.withChunkZ(ps.getChunkZ() -1);
		if (faction != this.getFactionAt(nearby)) return true;
		
		return false;
	}

	@Override
	public boolean isAnyBorderPs(Set<PS> pss)
	{
		return pss.stream().anyMatch(this::isBorderPs);
	}

	// Is this coord connected to any coord claimed by the specified faction?
	@Override
	public boolean isConnectedPs(PS ps, Faction faction)
	{
		ps = ps.getChunk(true);
		
		PS nearby = null;
		
		nearby = ps.withChunkX(ps.getChunkX() +1);
		if (faction == this.getFactionAt(nearby)) return true;
		
		nearby = ps.withChunkX(ps.getChunkX() -1);
		if (faction == this.getFactionAt(nearby)) return true;
		
		nearby = ps.withChunkZ(ps.getChunkZ() +1);
		if (faction == this.getFactionAt(nearby)) return true;
		
		nearby = ps.withChunkZ(ps.getChunkZ() -1);
		if (faction == this.getFactionAt(nearby)) return true;
		
		return false;
	}
	
	@Override
	public boolean isAnyConnectedPs(Set<PS> pss, Faction faction)
	{
		return pss.stream().anyMatch(ps -> this.isConnectedPs(ps, faction));
	}
	
}
