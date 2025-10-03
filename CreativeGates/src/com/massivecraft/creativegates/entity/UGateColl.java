package com.massivecraft.creativegates.entity;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.ExitComparator;
import com.massivecraft.creativegates.NetworkIdEqualsPredicate;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.Coll;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.List;

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
	
	public List<UGate> getGateChain(String networkId)
	{
		return new MassiveList<>(this.getAll(new NetworkIdEqualsPredicate(networkId), ExitComparator.get()));
	}

}
