package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorFaction004WarpsPerms extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorFaction004WarpsPerms i = new MigratorFaction004WarpsPerms();
	public static MigratorFaction004WarpsPerms get() { return i; }
	private MigratorFaction004WarpsPerms()
	{
		super(Faction.class);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		JsonElement perms = entity.get("perms");
		if (perms == null || perms.isJsonNull() || !perms.isJsonObject()) return;

		JsonObject permsO = perms.getAsJsonObject();

		JsonElement home = permsO.remove("home");
		if (home != null) permsO.add("warp", home);

		JsonElement sethome = permsO.remove("sethome");
		if (home != null) permsO.add("setwarp", home);
	}

}
