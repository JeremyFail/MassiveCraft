package com.massivecraft.factions.entity.migrator;

import com.massivecraft.massivecore.gson.JsonElement;
import com.massivecraft.massivecore.gson.JsonObject;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorFaction007CushionPerm extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorFaction007CushionPerm i = new MigratorFaction007CushionPerm();
	public static MigratorFaction007CushionPerm get() { return i; }
	private MigratorFaction007CushionPerm()
	{
		super(Faction.class);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		JsonObject perms = entity.getAsJsonObject("perms");
		if (perms == null) return;

		// Cushion perm (place/use cushions): copy from door (only if not already present)
		JsonElement doorPerm = perms.has("door") ? perms.get("door") : null;
		if (doorPerm != null && !perms.has("cushion"))
		{
			perms.add("cushion", doorPerm.deepCopy());
		}
	}
}
