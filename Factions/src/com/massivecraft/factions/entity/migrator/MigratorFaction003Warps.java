package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.MStore;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

import java.util.Map;

public class MigratorFaction003Warps extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorFaction003Warps i = new MigratorFaction003Warps();
	public static MigratorFaction003Warps get() { return i; }
	private MigratorFaction003Warps()
	{
		super(Faction.class);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		JsonElement jsonHome = entity.remove("home");
		if (jsonHome == null ||jsonHome.isJsonNull()) return;

		if (!jsonHome.isJsonObject()) throw new RuntimeException("not JsonObject " + jsonHome);

		PS psHome = MassiveCore.gson.fromJson(jsonHome, PS.class);
		Warp warp = new Warp("home", psHome);

		Map<String, Warp> warps = new MassiveMap<>();
		warps.put(MStore.createId(), warp);

		JsonElement jsonMap = MassiveCore.gson.toJsonTree(warps, (new TypeToken<Map<String,Warp>>(){}).getType());
		entity.add("warps", jsonMap);
	}

}
