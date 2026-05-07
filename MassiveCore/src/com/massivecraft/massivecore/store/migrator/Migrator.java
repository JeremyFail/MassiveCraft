package com.massivecraft.massivecore.store.migrator;


import com.massivecraft.massivecore.gson.JsonObject;

public interface Migrator
{
	// -------------------------------------------- //
	// MIGRATION
	// -------------------------------------------- //

	void migrate(JsonObject entity);

}
