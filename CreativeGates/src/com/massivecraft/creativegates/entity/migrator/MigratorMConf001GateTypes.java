package com.massivecraft.creativegates.entity.migrator;

import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.massivecore.gson.JsonArray;
import com.massivecraft.massivecore.gson.JsonElement;
import com.massivecraft.massivecore.gson.JsonObject;
import com.massivecraft.massivecore.gson.JsonPrimitive;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

/**
 * Migrates legacy global fill flags ({@code usingWater}, {@code useLavaInNether})
 * to unified string allow-lists {@code allowedGateTypes} /
 * {@code allowedHorizontalGateTypes} and {@code replaceWaterWithLavaInNether}.
 */
public class MigratorMConf001GateTypes extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static MigratorMConf001GateTypes i = new MigratorMConf001GateTypes();
	public static MigratorMConf001GateTypes get() { return i; }
	private MigratorMConf001GateTypes()
	{
		super(MConf.class);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void migrateInner(JsonObject entity)
	{
		boolean usingWater = getBoolean(entity, "usingWater", false);
		boolean useLavaInNether = getBoolean(entity, "useLavaInNether", true);
		
		JsonArray allowedVertical = new JsonArray();
		if (usingWater)
		{
			allowedVertical.add(new JsonPrimitive("WATER"));
		}
		else
		{
			allowedVertical.add(new JsonPrimitive("NETHER_PORTAL"));
		}
		
		// Horizontal gates always used water fill historically (lava via nether replacement).
		JsonArray allowedHorizontal = new JsonArray();
		allowedHorizontal.add(new JsonPrimitive("WATER"));
		
		entity.addProperty("replaceWaterWithLavaInNether", useLavaInNether);
		
		entity.add("allowedGateTypes", allowedVertical);
		entity.add("allowedHorizontalGateTypes", allowedHorizontal);
		
		entity.remove("usingWater");
		entity.remove("useLavaInNether");
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	private static boolean getBoolean(JsonObject entity, String field, boolean defaultValue)
	{
		JsonElement element = entity.get(field);
		if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) return defaultValue;
		JsonPrimitive primitive = element.getAsJsonPrimitive();
		if (!primitive.isBoolean()) return defaultValue;
		return primitive.getAsBoolean();
	}
	
}
