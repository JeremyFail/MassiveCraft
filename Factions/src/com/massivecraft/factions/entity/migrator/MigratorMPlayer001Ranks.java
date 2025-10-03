package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

import java.util.Collection;

public class MigratorMPlayer001Ranks extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorMPlayer001Ranks i = new MigratorMPlayer001Ranks();
	public static MigratorMPlayer001Ranks get() { return i; }
	private MigratorMPlayer001Ranks()
	{
		super(MPlayer.class);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		// Get role
		JsonElement jsonRole = entity.remove("role");
		String role;
		if (jsonRole == null)
		{
			// The role can be null.
			// Then they are probably recruit in the default faction (Wilderness).
			role = null;
		}
		else
		{
			role = jsonRole.getAsString();
		}

		// Get faction
		JsonElement jsonFaction = entity.get("factionId");

		String factionId;
		if (jsonFaction == null) factionId = MConf.get().defaultPlayerFactionId;
		else factionId = jsonFaction.getAsString();

		Faction faction = FactionColl.get().get(factionId);
		if (faction == null) faction = FactionColl.get().getNone();

		// Get rank
		Rank rank = null;
		if (role != null)
		{
			Collection<Rank> ranks = faction.getRanks().getAll();
			for (Rank r : ranks)
			{
				if (!r.getName().equalsIgnoreCase(role)) continue;
				rank = r;
				break;
			}
		}
		if (rank == null) rank = faction.getLowestRank();

		entity.add("rankId", new JsonPrimitive(rank.getId()));
	}
	
}
