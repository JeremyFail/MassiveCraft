package com.massivecraft.massivecore.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

public class AdapterEntry implements JsonDeserializer<Entry<?, ?>>, JsonSerializer<Entry<?, ?>>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final AdapterEntry i = new AdapterEntry();
	public static AdapterEntry get() { return i; }

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public JsonElement serialize(Entry<?, ?> src, Type type, JsonSerializationContext context)
	{
		// NULL
		if (src == null) return JsonNull.INSTANCE;
		
		// Create Ret
		JsonArray ret = new JsonArray();
		
		// Fill Ret
		Object key = src.getKey();
		Object value = src.getValue();
		
		Type keyType = getKeyType(type);
		Type valueType = getValueType(type);
		
		JsonElement keyJson = context.serialize(key, keyType);
		JsonElement valueJson = context.serialize(value, valueType);
		
		ret.add(keyJson);
		ret.add(valueJson);
		
		// Return Ret
		return ret;
	}
	
	@Override
	public Entry<?, ?> deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException
	{
		// NULL
		if (json == null) return null;
		if (json instanceof JsonNull) return null;
		
		JsonArray jsonArray = (JsonArray)json;
		
		JsonElement keyJson = jsonArray.get(0);
		JsonElement valueJson = jsonArray.get(1);
		
		Type keyType = getKeyType(type);
		Type valueType = getValueType(type);
		
		Object key = context.deserialize(keyJson, keyType);
		Object value = context.deserialize(valueJson, valueType);
		
		return new SimpleEntry<>(key, value);
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	public static Type getKeyType(Type type)
	{
		return getType(type, 0);
	}
	public static Type getValueType(Type type)
	{
		return getType(type, 1);
	}
	public static Type getType(Type type, int index)
	{
		ParameterizedType ptype = (ParameterizedType)type;
		Type[] types = ptype.getActualTypeArguments();
		return types[index];
	}
	
}
