package com.massivecraft.massivecore.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.Sound;

import java.lang.reflect.Type;

public class AdapterSound implements JsonDeserializer<Sound>, JsonSerializer<Sound>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static final AdapterSound i = new AdapterSound();
	public static AdapterSound get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public JsonElement serialize(Sound src, Type typeOfSrc, JsonSerializationContext context)
	{
		if (src == null) return JsonNull.INSTANCE;
		return new JsonPrimitive(src.name());
	}

	@Override
	public Sound deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
	{
		if (json == null) return null;
		if (json.equals(JsonNull.INSTANCE)) return null;
		return Sound.valueOf(json.getAsString());
	}

}
