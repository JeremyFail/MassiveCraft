package com.massivecraft.massivebooks.entity;

/**
 * Type of book for display and logic. Stored on MBook and on physical items (PDC).
 * More types (e.g. player books, lore books) can be added later.
 */
public enum BookType
{
	SERVER_BOOK("Server Book");

	private final String friendlyName;

	BookType(String friendlyName)
	{
		this.friendlyName = friendlyName;
	}

	public String getFriendlyName()
	{
		return friendlyName;
	}
}
