package com.massivecraft.massivecore.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.mson.MsonEvent;

import java.lang.reflect.Type;

public class AdapterMsonEventFix implements JsonDeserializer<MsonEvent>, JsonSerializer<MsonEvent>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static final AdapterMsonEventFix i = new AdapterMsonEventFix();
	public static AdapterMsonEventFix get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public JsonElement serialize(MsonEvent src, Type typeOfSrc, JsonSerializationContext context)
	{
		return Mson.getGson(false).toJsonTree(src);
	}

	@Override
	public MsonEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
	{
		MsonEvent ret = Mson.getGson(false).fromJson(json, MsonEvent.class);
		ret.repair();
		return ret;
	}
	
}
