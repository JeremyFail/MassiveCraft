package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.factions.adapter.TerritoryAccessAdapter;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorTerritoryAccess001Restructure extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorTerritoryAccess001Restructure i = new MigratorTerritoryAccess001Restructure();
	public static MigratorTerritoryAccess001Restructure get() { return i; }
	private MigratorTerritoryAccess001Restructure()
	{
		super(TerritoryAccess.class);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		JsonElement factionIds = entity.remove("factionIds");
		JsonElement playerIds = entity.remove("playerIds");

		JsonArray grantedIds = new JsonArray();

		if (factionIds != null && factionIds.isJsonArray())
		{
			JsonArray factionIdsArr = factionIds.getAsJsonArray();
			grantedIds.addAll(factionIdsArr);
		}

		if (playerIds != null && playerIds.isJsonArray())
		{
			JsonArray playerIdsArr = playerIds.getAsJsonArray();
			grantedIds.addAll(playerIdsArr);
		}

		if (grantedIds.size() > 0) entity.add(TerritoryAccessAdapter.GRANTED_IDS, grantedIds);
	}

}
