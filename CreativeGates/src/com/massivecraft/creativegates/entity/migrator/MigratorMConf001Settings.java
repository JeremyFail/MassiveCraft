package com.massivecraft.creativegates.entity.migrator;

import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.massivecore.gson.JsonArray;
import com.massivecraft.massivecore.gson.JsonElement;
import com.massivecraft.massivecore.gson.JsonObject;
import com.massivecraft.massivecore.gson.JsonPrimitive;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

/**
 * Migrates legacy MConf fields for this release:
 * <ul>
 *   <li>Fill flags ({@code usingWater}, {@code useLavaInNether}) → allow-lists</li>
 *   <li>{@code materialMode} → {@code materialManage}</li>
 *   <li>Removes obsolete {@code materialSecret}</li>
 * </ul>
 */
public class MigratorMConf001Settings extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static MigratorMConf001Settings i = new MigratorMConf001Settings();
	public static MigratorMConf001Settings get() { return i; }
	private MigratorMConf001Settings()
	{
		super(MConf.class);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void migrateInner(JsonObject entity)
	{
		migrateGateTypes(entity);
		migrateMaterials(entity);
	}
	
	// -------------------------------------------- //
	// GATE TYPES
	// -------------------------------------------- //
	
	private static void migrateGateTypes(JsonObject entity)
	{
		boolean hasLegacy = entity.has("usingWater") || entity.has("useLavaInNether");
		if (!hasLegacy && entity.has("allowedGateTypes")) return;
		
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
	// MATERIALS
	// -------------------------------------------- //
	
	private static void migrateMaterials(JsonObject entity)
	{
		JsonElement mode = entity.get("materialMode");
		if (mode != null && !mode.isJsonNull() && !entity.has("materialManage"))
		{
			entity.add("materialManage", mode);
		}
		entity.remove("materialMode");
		entity.remove("materialSecret");
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
