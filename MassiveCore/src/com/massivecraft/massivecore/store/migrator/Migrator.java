package com.massivecraft.massivecore.store.migrator;


import com.google.gson.JsonObject;

public interface Migrator
{
	// -------------------------------------------- //
	// MIGRATION
	// -------------------------------------------- //

	void migrate(JsonObject entity);

}
