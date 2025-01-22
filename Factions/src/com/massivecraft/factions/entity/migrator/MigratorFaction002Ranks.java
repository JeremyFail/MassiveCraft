package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.store.MStore;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MigratorFaction002Ranks extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorFaction002Ranks i = new MigratorFaction002Ranks();
	public static MigratorFaction002Ranks get() { return i; }
	private MigratorFaction002Ranks()
	{
		super(Faction.class);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		String idLeader = MStore.createId();
		String idOfficer = MStore.createId();
		String idMember = MStore.createId();
		String idRecruit = MStore.createId();

		Rank leader = new Rank("Leader", 400, "**");
		Rank officer = new Rank("Officer", 300, "*");
		Rank member = new Rank("Member", 200, "+");
		Rank recruit = new Rank("Recruit", 100, "-");

		Map<String, Rank> map = new MassiveMap<>();
		map.put(idLeader, leader);
		map.put(idOfficer, officer);
		map.put(idMember, member);
		map.put(idRecruit, recruit);

		JsonElement jsonMap = MassiveCore.gson.toJsonTree(map, (new TypeToken<Map<String,Rank>>(){}).getType());
		entity.add("ranks", jsonMap);

		JsonElement priorPerms = entity.get("perms");
		Map<String, Set<String>> newPerms = getPerms(priorPerms, idLeader, idOfficer, idMember, idRecruit);

		JsonElement jsonPerms = MassiveCore.gson.toJsonTree(newPerms, (new TypeToken<Map<String,Set<String>>>(){}).getType());
		entity.add("perms", jsonPerms);
	}

	private Map<String, Set<String>> getPerms(JsonElement priorPerms, String leaderId, String officerId, String memberId, String recruitId)
	{
		// We start with default values ...
		Map<String, Set<String>> ret = new MassiveMap<>();
		for (MPerm mperm : MPerm.getAll())
		{
			// This should work since MConf and MPerm is loaded /before/ Factions
			//Set<String> value = new MassiveSet<>(mperm.getStandard());
			Set<String> value = new MassiveSet<>(MConf.get().perm2default.get(mperm.getId()));

			if (value.remove("LEADER")) value.add(leaderId);
			if (value.remove("OFFICER")) value.add(officerId);
			if (value.remove("MEMBER")) value.add(memberId);
			if (value.remove("RECRUIT")) value.add(recruitId);
			ret.put(mperm.getId(), value);
		}

		if (priorPerms != null)
		{
			Map<String,Set<String>> permMap = MassiveCore.gson.fromJson(priorPerms, (new TypeToken<Map<String,Set<String>>>(){}).getType());
			// ... and if anything is explicitly set we use that info ...
			Iterator<Map.Entry<String, Set<String>>> iter = permMap.entrySet().iterator();
			while (iter.hasNext())
			{
				// ... for each entry ...
				Map.Entry<String, Set<String>> entry = iter.next();

				// ... extract id and remove null values ...
				String id = entry.getKey();
				if (id == null)
				{
					iter.remove();
					continue;
				}

				Set<String> value = entry.getValue();
				if (value.remove("LEADER")) value.add(leaderId);
				if (value.remove("OFFICER")) value.add(officerId);
				if (value.remove("MEMBER")) value.add(memberId);
				if (value.remove("RECRUIT")) value.add(recruitId);
				ret.put(id, value);
			}
		}

		return ret;
	}
	
}
