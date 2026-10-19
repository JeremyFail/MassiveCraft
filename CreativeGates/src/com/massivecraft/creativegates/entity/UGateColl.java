package com.massivecraft.creativegates.entity;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.comparator.ChainIndexComparator;
import com.massivecraft.creativegates.util.NetworkIdentityEqualsPredicate;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.Coll;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.List;

/**
 * Collection of persisted gates with network-identity chain helpers.
 */
public class UGateColl extends Coll<UGate>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static UGateColl i = new UGateColl();
	public static UGateColl get() { return i; }

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
	public String fixId(Object oid)
	{
		if (oid instanceof Block)
		{
			oid = PS.valueOf((Block)oid);
		}
		
		if (oid instanceof Location)
		{
			oid = PS.valueOf((Location)oid);
		}
		
		if (oid instanceof PS)
		{
			UGate gate = CreativeGates.get().getIndex().get((PS)oid);
			if (gate == null) return null;
			return gate.getId();
		}
		
		return super.fixId(oid);
	}
	
	// -------------------------------------------- //
	// EXTRAS
	// -------------------------------------------- //
	
	/**
	 * Returns all gates sharing the given gate's full network identity,
	 * ordered by chain index then exit location.
	 *
	 * @param gate Reference gate; must not be null.
	 * @return Ordered chain list (may be empty).
	 */
	public List<UGate> getGateChain(UGate gate)
	{
		if (gate == null) throw new NullPointerException("gate");
		return new MassiveList<>(this.getAll(new NetworkIdentityEqualsPredicate(gate), ChainIndexComparator.get()));
	}
	
	/**
	 * Returns all gates matching an explicit network identity,
	 * ordered by chain index then exit location.
	 *
	 * @param scopeType Scope type.
	 * @param networkOwnerId Owner id or null.
	 * @param networkId Network name.
	 * @return Ordered chain list.
	 */
	public List<UGate> getGateChain(GateScopeType scopeType, String networkOwnerId, String networkId)
	{
		return new MassiveList<>(this.getAll(
			new NetworkIdentityEqualsPredicate(scopeType, networkOwnerId, networkId),
			ChainIndexComparator.get()
		));
	}
	
	/**
	 * Next chain index for a gate joining the given identity (max existing + 1, or 0 if empty).
	 *
	 * @param scopeType Scope type.
	 * @param networkOwnerId Owner id or null.
	 * @param networkId Network name.
	 * @return Append index.
	 */
	public int nextChainIndex(GateScopeType scopeType, String networkOwnerId, String networkId)
	{
		List<UGate> chain = this.getGateChain(scopeType, networkOwnerId, networkId);
		int max = -1;
		for (UGate gate : chain)
		{
			if (gate == null) continue;
			if (gate.getChainIndex() > max) max = gate.getChainIndex();
		}
		return max + 1;
	}
	
	/**
	 * Destroys every FACTION-scoped gate owned by the given team id.
	 *
	 * @param teamId Stable team/faction id; ignored if null/blank.
	 * @return Number of gates destroyed.
	 */
	public int destroyFactionGatesForTeam(String teamId)
	{
		if (teamId == null || teamId.isEmpty()) return 0;
		int destroyed = 0;
		for (UGate gate : new MassiveList<>(this.getAll()))
		{
			if (gate == null) continue;
			if (gate.getScopeType() != GateScopeType.FACTION) continue;
			if (!teamId.equals(gate.getNetworkOwnerId())) continue;
			gate.destroy();
			destroyed++;
		}
		UNetworkAccessColl.get().destroyFactionAccessForTeam(teamId);
		UFactionPermsColl.get().destroyForTeam(teamId);
		return destroyed;
	}

}
