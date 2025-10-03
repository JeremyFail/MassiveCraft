package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

import java.util.Set;

public class MigratorMPerm002MoveStandard extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorMPerm002MoveStandard i = new MigratorMPerm002MoveStandard();
	public static MigratorMPerm002MoveStandard get() { return i; }
	private MigratorMPerm002MoveStandard()
	{
		super(MPerm.class);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		JsonElement jsonStandard = entity.remove("standard");
		if (jsonStandard == null || !jsonStandard.isJsonArray()) return;

		JsonArray jsonArray = jsonStandard.getAsJsonArray();
		Set<String> result = new MassiveSet<>();
		jsonArray.forEach(e -> result.add(e.getAsString()));

		String id = entity.get("name").getAsString();

		// This is hacky but we utilise that names and ids are the same
		MConf.get().perm2default.put(id, result);
		MConf.get().changed();
	}

}
