package com.massivecraft.factions.adapter;

import com.massivecraft.massivecore.gson.JsonDeserializationContext;
import com.massivecraft.massivecore.gson.JsonDeserializer;
import com.massivecraft.massivecore.gson.JsonElement;
import com.massivecraft.massivecore.gson.JsonParseException;
import com.massivecraft.massivecore.gson.JsonSerializationContext;
import com.massivecraft.massivecore.gson.JsonSerializer;
import com.massivecraft.factions.entity.Board;

import java.lang.reflect.Type;

public class BoardAdapter implements JsonDeserializer<Board>, JsonSerializer<Board>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static BoardAdapter i = new BoardAdapter();
	public static BoardAdapter get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Board deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
	{
		return new Board(context.deserialize(json, Board.MAP_TYPE));
	}

	@Override
	public JsonElement serialize(Board src, Type typeOfSrc, JsonSerializationContext context)
	{
		return context.serialize(src.getMap(), Board.MAP_TYPE);
	}
	
}
