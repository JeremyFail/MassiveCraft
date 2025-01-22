package com.massivecraft.factions.entity;

import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.entity.MassiveCoreMConf;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.Coll;
import com.massivecraft.massivecore.util.MUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class BoardColl extends Coll<Board> implements BoardInterface
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static BoardColl i = new BoardColl();
	public static BoardColl get() { return i; }
	private BoardColl()
	{
		this.setCreative(true);
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
	// OVERRIDE: COLL
	// -------------------------------------------- //
	
	@Override
	public String fixId(Object oid)
	{
		if (oid == null) return null;
		if (oid instanceof String) return (String)oid;
		if (oid instanceof Board) return ((Board)oid).getId();

		boolean debug = MassiveCoreMConf.get().debugEnabled;
		String ret = MUtil.extract(String.class, "worldName", oid);
		if (ret != null && debug)
		{
			System.out.println("extracted world name from " + oid);
		}
		return ret;
	}
	
	// -------------------------------------------- //
	// OVERRIDE: BOARD
	// -------------------------------------------- //
	
	@Override
	public TerritoryAccess getTerritoryAccessAt(PS ps)
	{
		if (ps == null) throw new NullPointerException("ps");
		Board board = this.get(ps.getWorld());
		if (board == null) return null;
		return board.getTerritoryAccessAt(ps);
	}
	
	@Override
	public Faction getFactionAt(PS ps)
	{
		if (ps == null) throw new NullPointerException("ps");
		Board board = this.get(ps.getWorld());
		if (board == null) return null;
		return board.getFactionAt(ps);
	}
	
	// SET

	@Override
	public void setTerritoryAccessAt(PS ps, TerritoryAccess territoryAccess)
	{
		if (ps == null) throw new NullPointerException("ps");
		Board board = this.get(ps.getWorld());
		if (board == null) return;
		board.setTerritoryAccessAt(ps, territoryAccess);
	}
	
	@Override
	public void setFactionAt(PS ps, Faction faction)
	{
		if (ps == null) throw new NullPointerException("ps");
		Board board = this.get(ps.getWorld());
		if (board == null) return;
		board.setFactionAt(ps, faction);
	}
	
	// REMOVE
	
	@Override
	public void removeAt(PS ps)
	{
		if (ps == null) throw new NullPointerException("ps");
		Board board = this.get(ps.getWorld());
		if (board == null) return;
		board.removeAt(ps);
	}
	
	@Override
	public void removeAll(Faction faction)
	{
		for (Board board : this.getAll())
		{
			board.removeAll(faction);
		}
	}
	
	// CHUNKS
	
	@Override
	public Set<PS> getChunks(Faction faction)
	{
		return this.getAll().stream()
			.flatMap(board -> board.getChunks(faction).stream())
			.collect(Collectors.toSet());
	}
	
	@Override
	public Set<PS> getChunks(String factionId)
	{
		return this.getAll().stream()
			.flatMap(board -> board.getChunks(factionId).stream())
			.collect(Collectors.toSet());
	}

	@Override
	@Deprecated
	public Map<Faction, Set<PS>> getFactionToChunks()
	{
		return this.getFactionToChunks(false);
	}
	
	@Override
	public Map<Faction, Set<PS>> getFactionToChunks(boolean withWorld)
	{
		// Create
		Map<Faction, Set<PS>> ret = null;
		
		// Fill
		for (Board board : this.getAll())
		{
			// Use the first board directly
			Map<Faction, Set<PS>> factionToChunks = board.getFactionToChunks(withWorld);
			if (ret == null)
			{
				ret = factionToChunks;
				continue;
			}
			
			// Merge the following boards
			for (Entry<Faction, Set<PS>> entry : factionToChunks.entrySet())
			{
				Faction faction = entry.getKey();
				Set<PS> chunks = ret.get(faction);
				if (chunks == null)
				{
					ret.put(faction, entry.getValue());
				}
				else
				{
					chunks.addAll(entry.getValue());
				}
			}
		}
		
		// Enforce create
		if (ret == null) ret = new MassiveMap<>();
		
		// Return
		return ret;
	}

	@Override
	public Map<String, Map<Faction, Set<PS>>> getWorldToFactionToChunks(boolean withWorld)
	{
		return this.getAll().stream()
			.collect(Collectors.toMap(Board::getId, board -> board.getFactionToChunks(withWorld)));
	}
	
	// COUNT
	
	@Override
	public int getCount(Faction faction)
	{
		return this.getCount(faction.getId());
	}
	
	@Override
	public int getCount(String factionId)
	{
		return this.getAll().stream()
			.mapToInt(board -> board.getCount(factionId))
			.sum();
	}
	
	@Override
	public Map<Faction, Long> getFactionToCount()
	{
		// Get them all and map them to sets of entries
		return this.getAll().stream()
			.map(Board::getFactionToCount)
			.map(Map::entrySet)
			.flatMap(Set::stream)
		// Collect the entries in a map of <Faction, Long> by summing the values
			.collect(Collectors.groupingBy(
				Entry::getKey,
				Collectors.summingLong(Entry::getValue)
			))
		;
	}
	
	// COUNT
	
	@Override
	public boolean hasClaimed(Faction faction)
	{
		return this.hasClaimed(faction.getId());
	}
	
	@Override
	public boolean hasClaimed(String factionId)
	{
		return this.getAll().stream()
			.anyMatch(board -> board.hasClaimed(factionId));
	}
	
	// NEARBY DETECTION
	
	@Override
	public boolean isBorderPs(PS ps)
	{
		if (ps == null) throw new NullPointerException("ps");
		Board board = this.get(ps.getWorld());
		if (board == null) return false;
		return board.isBorderPs(ps);
	}

	@Override
	public boolean isAnyBorderPs(Set<PS> pss)
	{
		return pss.stream().anyMatch(this::isBorderPs);
	}
	
	@Override
	public boolean isConnectedPs(PS ps, Faction faction)
	{
		if (ps == null) throw new NullPointerException("ps");
		Board board = this.get(ps.getWorld());
		if (board == null) return false;
		return board.isConnectedPs(ps, faction);
	}
	
	@Override
	public boolean isAnyConnectedPs(Set<PS> pss, Faction faction)
	{
		for (PS ps : pss)
		{
			if (this.isConnectedPs(ps, faction)) return true;
		}
		return false;
	}
	
	// -------------------------------------------- //
	// WORLDS
	// -------------------------------------------- //
	
	public Set<String> getClaimedWorlds(Faction faction)
	{
		return getClaimedWorlds(faction.getId());
	}
	
	public Set<String> getClaimedWorlds(String factionId)
	{
		if (factionId == null) throw new NullPointerException("factionId");
		return this.getAll().stream()
			.filter(board -> board.hasClaimed(factionId))
			.map(Board::getId)
			.collect(Collectors.toSet());
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	// Distance -1 returns 0 chunks always.
	// Distance 0 returns 1 chunk only (the one supplied).
	// Distance 1 returns 3x3 = 9 chunks.
	public static Set<PS> getNearbyChunks(PS psChunk, int distance)
	{
		// Fix Args
		if (psChunk == null) throw new NullPointerException("psChunk");
		psChunk = psChunk.getChunk(true);
		
		// Create
		Set<PS> ret = new MassiveSet<>();
		if (distance < 0) return ret;
		
		// Fill
		int chunkX = psChunk.getChunkX();
		int xmin = chunkX - distance;
		int xmax = chunkX + distance;
		
		int chunkZ = psChunk.getChunkZ();
		int zmin = chunkZ - distance;
		int zmax = chunkZ + distance;
		
		for (int x = xmin; x <= xmax; x++)
		{
			PS psChunkX = psChunk.withChunkX(x);
			for (int z = zmin; z <= zmax; z++)
			{
				ret.add(psChunkX.withChunkZ(z));
			}
		}
		
		// Return
		return ret;
	}
	
	public static Set<PS> getNearbyChunks(Collection<PS> chunks, int distance)
	{
		// Fix Args
		if (chunks == null) throw new NullPointerException("chunks");
		
		// Create
		Set<PS> ret = new MassiveSet<>();
		
		if (distance < 0) return ret;
		
		// Fill
		for (PS chunk : chunks)
		{
			ret.addAll(getNearbyChunks(chunk, distance));
		}
		
		// Return
		return ret;
	}
	
	public static Set<Faction> getDistinctFactions(Collection<PS> chunks)
	{
		// Fix Args
		if (chunks == null) throw new NullPointerException("chunks");
		
		// Create
		Set<Faction> ret = new MassiveSet<>();
		
		// Fill
		for (PS chunk : chunks)
		{
			Faction faction = get().getFactionAt(chunk);
			if (faction == null) continue;
			ret.add(faction);
		}
		
		// Return
		return ret;
	}
	
	public static Map<PS, Faction> getChunkFaction(Collection<PS> chunks)
	{
		// Create
		Map<PS, Faction> ret = new MassiveMap<>();
		
		// Fill
		Faction none = FactionColl.get().getNone();
		for (PS chunk : chunks)
		{
			chunk = chunk.getChunk(true);
			Faction faction = get().getFactionAt(chunk);
			if (faction == null) faction = none;
			ret.put(chunk, faction);
		}
		
		// Return
		return ret;
	}

	public static List<Collection<PS>> getForests(Collection<PS> pss)
	{
		List<Collection<PS>> forests = new MassiveList<>();
		List<PS> discovered = new MassiveList<>();

		outer:
		for (PS ps : pss)
		{
			if (discovered.contains(ps)) continue outer;

			List<PS> forest = new MassiveList<>();
			forests.add(forest);

			Stack<PS> stack = new Stack<>();
			stack.push(ps);
			inner:
			while (!stack.empty())
			{
				PS elm = stack.pop();
				if (discovered.contains(elm)) continue inner;
				System.out.println(elm);
				discovered.add(elm);
				forest.add(elm);

				addIfInSource(elm.withChunkX(elm.getChunkX() + 1), stack, pss);
				addIfInSource(elm.withChunkX(elm.getChunkX() - 1), stack, pss);
				addIfInSource(elm.withChunkZ(elm.getChunkZ() + 1), stack, pss);
				addIfInSource(elm.withChunkZ(elm.getChunkZ() - 1), stack, pss);
			}
		}

		return forests;
	}

	private static void addIfInSource(PS ps, Stack<PS> stack, Collection<PS> source)
	{
		if (source.contains(ps)) stack.push(ps);
	}

}
