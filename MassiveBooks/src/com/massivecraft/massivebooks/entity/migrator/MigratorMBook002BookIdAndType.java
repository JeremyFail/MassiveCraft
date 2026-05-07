package com.massivecraft.massivebooks.entity.migrator;

import com.massivecraft.massivecore.gson.JsonArray;
import com.massivecraft.massivecore.gson.JsonElement;
import com.massivecraft.massivecore.gson.JsonObject;
import com.massivecraft.massivebooks.Const;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

import java.util.UUID;

/**
 * Migrates MBook from version 1 to version 2.
 *
 * Adds:
 * - bookId: unique UUID (string) for this saved book, used to link physical items to this MBook
 * - bookType: "SERVER_BOOK" for all existing books
 * - copyrighted: true/false; migrated from item lore (if lore contained "COPYRIGHTED", set true and remove from lore)
 *
 * Entity id remains the book title (normalized). Lookup by bookId is done via MBookColl.getById().
 */
public class MigratorMBook002BookIdAndType extends MigratorRoot
{
	private static MigratorMBook002BookIdAndType i = new MigratorMBook002BookIdAndType();
	public static MigratorMBook002BookIdAndType get() { return i; }

	private MigratorMBook002BookIdAndType()
	{
		super(MBook.class);
	}

	@Override
	public void migrateInner(JsonObject entity)
	{
		entity.addProperty("bookId", UUID.randomUUID().toString());
		entity.addProperty("bookType", "SERVER_BOOK");

		// Move copyrighted from item lore to its own field (for future lore editing; lore no longer stores COPYRIGHTED)
		boolean copyrighted = false;
		JsonElement itemElement = entity.get("item");
		if (itemElement != null && itemElement.isJsonObject())
		{
			JsonObject item = itemElement.getAsJsonObject();
			JsonElement loreElement = item.get("lore");
			if (loreElement != null && loreElement.isJsonArray())
			{
				JsonArray lore = loreElement.getAsJsonArray();
				for (int i = lore.size() - 1; i >= 0; i--)
				{
					JsonElement line = lore.get(i);
					if (line != null && line.isJsonPrimitive() && Const.COPYRIGHTED.equals(line.getAsString()))
					{
						copyrighted = true;
						lore.remove(i);
						break;
					}
				}
				if (lore.size() == 0)
				{
					item.remove("lore");
				}
			}
		}
		entity.addProperty("copyrighted", copyrighted);
	}
}
