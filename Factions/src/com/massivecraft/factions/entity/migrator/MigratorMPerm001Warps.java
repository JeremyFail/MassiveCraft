package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorMPerm001Warps extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorMPerm001Warps i = new MigratorMPerm001Warps();
	public static MigratorMPerm001Warps get() { return i; }
	private MigratorMPerm001Warps()
	{
		super(MPerm.class);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		JsonElement jsonName = entity.get("name");
		String name = jsonName.getAsString();
		if (name.equalsIgnoreCase("home")) name = "warp";
		if (name.equalsIgnoreCase("sethome")) name = "setwarp";

		entity.addProperty("name", name);

		JsonElement jsonDesc = entity.get("desc");
		String desc = jsonDesc.getAsString();
		if (desc.equalsIgnoreCase("teleport home")) desc = "teleport to warp";
		if (desc.equalsIgnoreCase("set the home")) desc = "set warps";

		entity.addProperty("desc", desc);
	}

}
