package com.massivecraft.factions.adapter;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.massivecore.store.migrator.MigratorUtil;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

public class TerritoryAccessAdapter implements JsonDeserializer<TerritoryAccess>, JsonSerializer<TerritoryAccess>
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //

	public static final String HOST_FACTION_ID = "hostFactionId";
	public static final String HOST_FACTION_ALLOWED = "hostFactionAllowed";
	public static final String GRANTED_IDS = "grantedIds";
	public static final String CHUNK_NAME = "chunkName";
	
	public static final Type SET_OF_STRING_TYPE = new TypeToken<Set<String>>(){}.getType();
			
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TerritoryAccessAdapter i = new TerritoryAccessAdapter();
	public static TerritoryAccessAdapter get() { return i; }
	
	//----------------------------------------------//
	// OVERRIDE
	//----------------------------------------------//

	@Override
	public TerritoryAccess deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
	{
		// isDefault <=> simple hostFactionId string
		if (json.isJsonPrimitive())
		{
			String hostFactionId = json.getAsString();
			return TerritoryAccess.valueOf(hostFactionId);
		}

		// Otherwise object
		JsonObject obj = json.getAsJsonObject();

		// Prepare variables
		String hostFactionId = null;
		Boolean hostFactionAllowed = null;
		Set<String> grantedIds = Collections.emptySet();
		String chunkName = null;

		// Read variables (test old values first)
		JsonElement element = null;
		
		element = obj.get(HOST_FACTION_ID);
		hostFactionId = element.getAsString();
		
		element = obj.get(HOST_FACTION_ALLOWED);
		if (element != null) hostFactionAllowed = element.getAsBoolean();

		element = obj.get(GRANTED_IDS);
		if (element != null) grantedIds = context.deserialize(element, SET_OF_STRING_TYPE);

		element = obj.get(CHUNK_NAME);
		if (element != null) chunkName = element.getAsString();
		
		return TerritoryAccess.valueOf(hostFactionId, hostFactionAllowed, grantedIds, chunkName);
	}

	@Override
	public JsonElement serialize(TerritoryAccess src, Type typeOfSrc, JsonSerializationContext context)
	{
		if (src == null) return null;

		// isDefault <=> simple hostFactionId string
		if (src.isDefault())
		{
			return new JsonPrimitive(src.getHostFactionId());
		}

		// Otherwise object
		JsonObject obj = new JsonObject();
		
		obj.addProperty(HOST_FACTION_ID, src.getHostFactionId());
		
		if (!src.isHostFactionAllowed())
		{
			obj.addProperty(HOST_FACTION_ALLOWED, src.isHostFactionAllowed());
		}
		
		if (!src.getGrantedIds().isEmpty())
		{
			obj.add(GRANTED_IDS, context.serialize(src.getGrantedIds(), SET_OF_STRING_TYPE));
		}

		if (src.getChunkName() != null)
		{
			obj.addProperty(CHUNK_NAME, src.getChunkName());
		}

		obj.add(MigratorUtil.VERSION_FIELD_NAME, new JsonPrimitive(src.version));

		return obj;
	}
	
}
