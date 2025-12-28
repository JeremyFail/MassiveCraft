package com.massivecraft.massivecore.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.massivecraft.massivecore.item.DataItemStack;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * This is a GSON serializer/deserializer for the Bukkit ItemStack.
 * Uses Bukkit's native ItemStack serialization methods which convert to/from Map.
 * Maintains backwards compatibility with the old DataItemStack format.
 */
public class AdapterItemStack implements JsonDeserializer<ItemStack>, JsonSerializer<ItemStack>
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //

	private static final AdapterItemStack i = new AdapterItemStack();
	public static AdapterItemStack get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context)
	{
		if (src == null) return null;
		
		// Use Bukkit's built-in serialization
		Map<String, Object> serialized = src.serialize();
		return context.serialize(serialized);
	}

	@Override
	public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
	{
		if (json == null || json.isJsonNull()) return null;
		
		try
		{
			// Check if this is the old DataItemStack format
			if (json.isJsonObject())
			{
				JsonObject obj = json.getAsJsonObject();
				
				// If it only has a "version" field and nothing else, it's an empty/corrupted DataItemStack
				if (obj.has("version") && obj.size() == 1)
				{
					return null; // Return null for empty items
				}
				
				// Old format has fields like "id", "count", "damage", "name", "lore", etc.
				// New format has "type", "amount", "meta", etc.
				if (obj.has("id") || obj.has("version"))
				{
					// This is old DataItemStack format - deserialize through DataItemStack
					DataItemStack dataItemStack = context.deserialize(json, DataItemStack.class);
					return dataItemStack.toBukkit();
				}
			}
			
			// New format - deserialize as Map and use Bukkit's deserialization
			Map<String, Object> map = context.deserialize(json, Map.class);
			
			// Use Bukkit's built-in deserialization
			return ItemStack.deserialize(map);
		}
		catch (Exception e)
		{
			throw new JsonParseException("Failed to deserialize ItemStack", e);
		}
	}

}
