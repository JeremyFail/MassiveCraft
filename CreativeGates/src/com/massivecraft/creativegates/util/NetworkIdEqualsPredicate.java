package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.entity.UGate;

import java.util.function.Predicate;

/**
 * Legacy name-only matcher.
 * Prefer {@link NetworkIdentityEqualsPredicate} for chain membership.
 *
 * @deprecated Use {@link NetworkIdentityEqualsPredicate}.
 */
@Deprecated
public class NetworkIdEqualsPredicate implements Predicate<UGate>
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final String networkId;
	public String getNetworkId() { return this.networkId; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public NetworkIdEqualsPredicate(String networkId)
	{
		this.networkId = networkId;
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public boolean test(UGate ugate)
	{
		if (ugate == null || ugate.getNetworkId() == null || this.networkId == null) return false;
		return ugate.getNetworkId().equalsIgnoreCase(this.networkId);
	}
	
}
