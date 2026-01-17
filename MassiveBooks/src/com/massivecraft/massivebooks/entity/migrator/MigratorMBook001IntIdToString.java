package com.massivecraft.massivebooks.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

/**
 * Migrates MBook from old MassiveCraft V2 (no version field) to new V3 format
 * (which now has a version field).
 * 
 * Changes:
 * - Converts integer item IDs to string material names (e.g. 387 -> "WRITTEN_BOOK")
 * - Adds entity version field (version 1)
 * 
 * Legacy - no version field:
 * {
 *   "item": {
 *     "id": 387,  // Integer ID
 *     "name": "§f§btest §f§oby §fSteve",
 *     "lore": ["COPYRIGHTED"],
 *     "title": "§btest",
 *     "author": "Steve",
 *     "pages": ["test"]
 *   }
 * }
 * 
 * V1 Format:
 * {
 *   "version": 1,
 *   "item": {
 *     "id": "WRITTEN_BOOK",  // String material name
 *     "name": "§f§btest §f§oby §fSteve",
 *     "lore": ["COPYRIGHTED"],
 *     "title": "§btest",
 *     "author": "Steve",
 *     "generation": "ORIGINAL",
 *     "pages": ["test"]
 *   }
 * }
 */
public class MigratorMBook001IntIdToString extends MigratorRoot
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	// The only valid book ID in v2 format
	private static final int LEGACY_WRITTEN_BOOK_ID = 387;
	private static final String WRITTEN_BOOK_MATERIAL = "WRITTEN_BOOK";
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static MigratorMBook001IntIdToString i = new MigratorMBook001IntIdToString();
	public static MigratorMBook001IntIdToString get() { return i; }
	private MigratorMBook001IntIdToString()
	{
		super(MBook.class);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void migrateInner(JsonObject entity)
	{
		// Get the item object
		JsonElement itemElement = entity.get("item");
		if (itemElement == null || itemElement.isJsonNull() || !itemElement.isJsonObject())
		{
			// No item data, nothing to migrate
			return;
		}
		
		JsonObject item = itemElement.getAsJsonObject();
		
		// Get the id field
		JsonElement idElement = item.get("id");
		if (idElement == null || idElement.isJsonNull())
		{
			// No id field, cannot migrate
			return;
		}
		
		// Check if ID is an integer (v2 format)
		// If id is already a string, no migration needed (assume already v3 format)
		if (idElement.isJsonPrimitive() && idElement.getAsJsonPrimitive().isNumber())
		{
			int legacyId = idElement.getAsInt();
			
			// Validate that it's a written book
			if (legacyId != LEGACY_WRITTEN_BOOK_ID)
			{
				throw new IllegalStateException(
					"Invalid legacy book ID: " + legacyId + 
					". Expected " + LEGACY_WRITTEN_BOOK_ID + " (WRITTEN_BOOK). " +
					"Cannot migrate this book - it may be corrupted."
				);
			}
			
			// Convert integer ID to string material name
			item.addProperty("id", WRITTEN_BOOK_MATERIAL);
			
			// Note: We preserve all other fields as-is, including:
			// - lore
			// - title
			// - author
			// - pages
		}
	}
}
