package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.UGate;

import java.util.function.Predicate;

/**
 * Matches gates that share a full network identity:
 * scope type, owner id (nullable), and case-insensitive network name.
 */
public class NetworkIdentityEqualsPredicate implements Predicate<UGate>
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final GateScopeType scopeType;
	private final String networkOwnerId;
	private final String networkId;
	
	/**
	 * @return Scope type used for matching.
	 */
	public GateScopeType getScopeType() { return this.scopeType; }
	
	/**
	 * @return Network owner id used for matching (may be null).
	 */
	public String getNetworkOwnerId() { return this.networkOwnerId; }
	
	/**
	 * @return Network name used for matching.
	 */
	public String getNetworkId() { return this.networkId; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	/**
	 * Creates a predicate from an existing gate's identity.
	 *
	 * @param gate Source gate; must not be null.
	 */
	public NetworkIdentityEqualsPredicate(UGate gate)
	{
		if (gate == null) throw new NullPointerException("gate");
		this.scopeType = gate.getScopeType();
		this.networkOwnerId = gate.getNetworkOwnerId();
		this.networkId = gate.getNetworkId();
	}
	
	/**
	 * Creates a predicate from explicit identity parts.
	 *
	 * @param scopeType Scope type; null treated as SHARED.
	 * @param networkOwnerId Owner id or null.
	 * @param networkId Network display name.
	 */
	public NetworkIdentityEqualsPredicate(GateScopeType scopeType, String networkOwnerId, String networkId)
	{
		this.scopeType = scopeType != null ? scopeType : GateScopeType.SHARED;
		this.networkOwnerId = networkOwnerId;
		this.networkId = networkId;
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public boolean test(UGate ugate)
	{
		if (ugate == null) return false;
		if (ugate.getScopeType() != this.scopeType) return false;
		String otherOwner = ugate.getNetworkOwnerId();
		if (this.networkOwnerId == null ? otherOwner != null : !this.networkOwnerId.equals(otherOwner)) return false;
		String otherNetwork = ugate.getNetworkId();
		if (this.networkId == null || otherNetwork == null) return this.networkId == otherNetwork;
		return this.networkId.equalsIgnoreCase(otherNetwork);
	}
	
}
