package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

import java.util.Iterator;

public class MigratorMConf004Rank extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorMConf004Rank i = new MigratorMConf004Rank();
	public static MigratorMConf004Rank get() { return i; }
	private MigratorMConf004Rank() { super(MConf.class); }


	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		migrateRename(entity, "denyCommandsTerritoryRelation");
		migrateRename(entity, "denyCommandsDistanceRelation");
		migrateRename(entity, "denyCommandsDistanceBypassIn");

		entity.remove("defaultPlayerRole");
		entity.remove("herochatFactionName");
		entity.remove("herochatFactionNick");
		entity.remove("herochatFactionFormat");
		entity.remove("herochatFactionColor");
		entity.remove("herochatFactionDistance");
		entity.remove("herochatFactionIsShortcutAllowed");
		entity.remove("herochatFactionCrossWorld");
		entity.remove("herochatFactionMuted");
		entity.remove("herochatFactionWorlds");
		entity.remove("herochatAlliesName");
		entity.remove("herochatAlliesNick");
		entity.remove("herochatAlliesFormat");
		entity.remove("herochatAlliesColor");
		entity.remove("herochatAlliesDistance");
		entity.remove("herochatAlliesIsShortcutAllowed");
		entity.remove("herochatAlliesCrossWorld");
		entity.remove("herochatAlliesMuted");
		entity.remove("herochatAlliesWorlds");
	}

	private void migrateRename(JsonObject entity, String name)
	{
		JsonElement element = entity.get(name);
		if (element.isJsonObject())
		{
			JsonObject map = element.getAsJsonObject();
			if (map.has("MEMBER"))
			{
				JsonElement e = map.remove("MEMBER");
				map.add("FACTION", e);
			}
		}
		if (element.isJsonArray())
		{
			JsonArray array = element.getAsJsonArray();
			boolean success = false;
			for (Iterator<JsonElement> it = array.iterator(); it.hasNext(); )
			{
				JsonElement e = it.next();
				if (!e.getAsString().equals("MEMBER")) continue;
				it.remove();
				success = true;
			}
			if (success)
			{
				array.add("FACTION");
			}
		}
	}
	
}
