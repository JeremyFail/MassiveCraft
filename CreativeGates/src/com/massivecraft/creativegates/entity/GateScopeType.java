package com.massivecraft.creativegates.entity;

/**
 * Namespace type for a gate network identity.
 * Chain membership uses scope together with optional owner id and network name.
 */
public enum GateScopeType
{
	/**
	 * Server-owned official routes. No player/team owner id.
	 */
	GLOBAL,
	
	/**
	 * Name-only public networks (legacy linking behavior). No owner id.
	 */
	SHARED,
	
	/**
	 * Personal networks owned by a player UUID.
	 */
	PLAYER,
	
	/**
	 * Team/faction networks owned by a stable team id when a bridge is available.
	 */
	FACTION,
	;
	
	/**
	 * Parses a scope from config or persistence text.
	 *
	 * @param raw Raw scope string, or null.
	 * @param fallback Value when raw is null/blank/unknown.
	 * @return Matching scope or fallback.
	 */
	public static GateScopeType parse(String raw, GateScopeType fallback)
	{
		if (raw == null || raw.isEmpty()) return fallback;
		try
		{
			return GateScopeType.valueOf(raw.trim().toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			return fallback;
		}
	}
	
	/**
	 * True when this scope stores a non-null network owner id.
	 *
	 * @return True for PLAYER and FACTION.
	 */
	public boolean requiresOwnerId()
	{
		return this == PLAYER || this == FACTION;
	}
}
