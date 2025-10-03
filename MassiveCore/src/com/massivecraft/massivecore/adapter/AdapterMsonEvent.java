package com.massivecraft.massivecore.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.massivecraft.massivecore.mson.MsonEvent;

import java.lang.reflect.Type;

public class AdapterMsonEvent implements JsonDeserializer<MsonEvent>, JsonSerializer<MsonEvent>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static final AdapterMsonEvent i = new AdapterMsonEvent();
	public static AdapterMsonEvent get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public JsonElement serialize(MsonEvent src, Type typeOfSrc, JsonSerializationContext context)
	{
		return MsonEvent.toJson(src);
	}

	@Override
	public MsonEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
	{
		return MsonEvent.fromJson(json);
	}
	
}
