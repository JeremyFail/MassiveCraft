package com.massivecraft.massivebooks.integration.placeholderapi;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.Integration;

/**
 * Integration with PlaceholderAPI.
 * Placeholders are stored raw in books and parsed when a book is loaded/given/updated for a viewer
 * (see {@link PlaceholderAPIProcessor}). We do not register any placeholders.
 */
public class IntegrationPlaceholderAPI extends Integration
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static IntegrationPlaceholderAPI i = new IntegrationPlaceholderAPI();
	public static IntegrationPlaceholderAPI get() { return i; }
	private IntegrationPlaceholderAPI()
	{
		this.setPluginName("PlaceholderAPI");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Engine getEngine()
	{
		return EnginePlaceholderAPI.get();
	}

	@Override
	public void setIntegrationActiveInner(boolean active)
	{
		// No placeholders to register or unregister; we only consume the API when signing books.
	}
}
