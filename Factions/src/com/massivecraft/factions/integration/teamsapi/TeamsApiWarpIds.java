package com.massivecraft.factions.integration.teamsapi;

import java.util.UUID;

/**
 * Sentinel UUIDs reused when faction-side models lack optional TeamsAPI-facing metadata (e.g. warp creator uuid).
 */
final class TeamsApiWarpIds
{
	/** Substitute creator id - MassiveCraft warps historically do not expose who created each warp via this bridge. */
	static final UUID UNKNOWN = new UUID(0L, 0L);

	/** Holds static sentinel values only; not instantiable. */
	private TeamsApiWarpIds()
	{

	}

}
