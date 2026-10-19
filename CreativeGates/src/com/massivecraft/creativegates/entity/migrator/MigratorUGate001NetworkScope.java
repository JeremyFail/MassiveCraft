package com.massivecraft.creativegates.entity.migrator;

import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.massivecore.gson.JsonObject;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

/**
 * Migrates legacy name-only gates to scoped identity:
 * {@code scopeType=SHARED}, {@code networkOwnerId=null}, and a default {@code chainIndex}.
 * Network name and creator are unchanged so prior same-name connectivity is preserved.
 */
public class MigratorUGate001NetworkScope extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static MigratorUGate001NetworkScope i = new MigratorUGate001NetworkScope();
	public static MigratorUGate001NetworkScope get() { return i; }
	private MigratorUGate001NetworkScope()
	{
		super(UGate.class);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void migrateInner(JsonObject entity)
	{
		if (entity == null) return;
		
		if (!entity.has("scopeType") || entity.get("scopeType").isJsonNull())
		{
			entity.addProperty("scopeType", GateScopeType.SHARED.name());
		}
		
		if (!entity.has("networkOwnerId"))
		{
			entity.add("networkOwnerId", null);
		}
		
		if (!entity.has("chainIndex") || entity.get("chainIndex").isJsonNull())
		{
			entity.addProperty("chainIndex", 0);
		}
	}
	
}
