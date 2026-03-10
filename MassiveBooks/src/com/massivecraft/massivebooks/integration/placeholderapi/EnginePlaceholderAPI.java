package com.massivecraft.massivebooks.integration.placeholderapi;

import com.massivecraft.massivecore.Engine;

/**
 * Engine for PlaceholderAPI integration.
 * Placeholders are no longer processed when a book is signed; they are stored raw
 * and parsed when the book is loaded/given/updated for a viewer (see PlaceholderAPIProcessor).
 */
public class EnginePlaceholderAPI extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EnginePlaceholderAPI i = new EnginePlaceholderAPI();
	public static EnginePlaceholderAPI get() { return i; }
	private EnginePlaceholderAPI() {}
}
