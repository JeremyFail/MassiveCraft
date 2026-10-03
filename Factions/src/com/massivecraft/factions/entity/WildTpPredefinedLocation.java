package com.massivecraft.factions.entity;

/**
 * Operator-defined wilderness teleport location stored on {@link MConf#wildTpPredefinedLocations}.
 * Coordinates are block positions; {@link #radiusNoPlayerClaims} is measured in chunks.
 */
public class WildTpPredefinedLocation
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	/** Unique display/lookup name for this location. */
	public String name = "";
	/** Bukkit world name the coordinates belong to. */
	public String world = "";
	/** Block X coordinate. */
	public double x = 0D;
	/** Block Y coordinate (feet). */
	public double y = 64D;
	/** Block Z coordinate. */
	public double z = 0D;
	/**
	 * Chebyshev chunk radius that must be free of disallowed claims for a "clear" location.
	 * Must be â‰¥ 0.
	 */
	public int radiusNoPlayerClaims = 0;
	/**
	 * When true, warzone/safezone claims inside {@link #radiusNoPlayerClaims} are allowed.
	 * Player faction claims are still disallowed for a clear location.
	 */
	public boolean allowWarzoneSafezoneClaims = false;

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	/**
	 * No-arg constructor for Gson / config deserialization.
	 */
	public WildTpPredefinedLocation()
	{

	}

	/**
	 * Creates a fully specified predefined location.
	 *
	 * @param name location name
	 * @param world world name
	 * @param x block X
	 * @param y block Y
	 * @param z block Z
	 * @param radiusNoPlayerClaims chunk radius for claim checks
	 * @param allowWarzoneSafezoneClaims whether warzone/safezone in radius is allowed
	 */
	public WildTpPredefinedLocation(String name, String world, double x, double y, double z, int radiusNoPlayerClaims, boolean allowWarzoneSafezoneClaims)
	{
		this.name = name;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.radiusNoPlayerClaims = radiusNoPlayerClaims;
		this.allowWarzoneSafezoneClaims = allowWarzoneSafezoneClaims;
	}

}
