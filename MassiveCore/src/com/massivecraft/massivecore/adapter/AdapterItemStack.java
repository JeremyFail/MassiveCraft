package com.massivecraft.massivecore.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.massivecraft.massivecore.item.DataItemStack;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;

/**
 * This is a GSON serializer/deserializer for the Bukkit ItemStack.
 * Uses DataItemStack for all serialization.
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
		
		// Convert to DataItemStack and let GSON serialize it normally
		DataItemStack dataItemStack = new DataItemStack(src);
		
		return context.serialize(dataItemStack);
	}

	@Override
	public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
	{
		if (json == null || json.isJsonNull()) return null;
		
		// Deserialize as DataItemStack and convert to ItemStack
		DataItemStack dataItemStack = context.deserialize(json, DataItemStack.class);
		return dataItemStack == null ? null : dataItemStack.toBukkit();
	}

}
