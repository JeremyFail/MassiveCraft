package com.massivecraft.massivebooks.entity.migrator;

import com.google.gson.JsonObject;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

import java.util.UUID;

/**
 * Migrates MBook from version 1 to version 2.
 *
 * Adds:
 * - bookId: unique UUID (string) for this saved book, used to link physical items to this MBook
 * - bookType: "SERVER_BOOK" for all existing books
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
	}
}
