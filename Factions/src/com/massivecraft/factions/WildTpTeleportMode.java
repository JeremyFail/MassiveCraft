package com.massivecraft.factions;

/**
 * Destination strategy for {@code /f wildtp}.
 * Config stores the mode as a string ({@link #getId()}); parse with {@link #parse(String)}.
 */
public enum WildTpTeleportMode
{
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //

	/** Cache → pure random → claim-edge band. */
	RANDOM,

	/** Operator-defined locations only (resolve before delay). */
	PREDEFINED,

	/** Same tiers as {@link #RANDOM}, then predefined fallback when attempts are exhausted. */
	HYBRID,

	// END OF LIST
	;

	// -------------------------------------------- //
	// PARSE / ID
	// -------------------------------------------- //

	/**
	 * Parses a config/API mode string. Null, blank, or unknown values become {@link #RANDOM}.
	 *
	 * @param raw mode string (case-insensitive); may be null
	 * @return never-null mode
	 */
	public static WildTpTeleportMode parse(String raw)
	{
		if (raw == null) return RANDOM;
		String m = raw.trim().toLowerCase();
		if (m.isEmpty()) return RANDOM;
		if ("predefined".equals(m)) return PREDEFINED;
		if ("hybrid".equals(m)) return HYBRID;
		if ("random".equals(m)) return RANDOM;
		return RANDOM;
	}

	/**
	 * @return lowercase id matching config values ({@code random}, {@code predefined}, {@code hybrid})
	 */
	public String getId()
	{
		return this.name().toLowerCase();
	}

	// -------------------------------------------- //
	// HELPERS
	// -------------------------------------------- //

	/**
	 * @return true when this mode may use predefined locations
	 */
	public boolean usesPredefined()
	{
		return this == PREDEFINED || this == HYBRID;
	}

	/**
	 * @return true when search uses random/claim-edge tiers ({@link #RANDOM} or {@link #HYBRID})
	 */
	public boolean usesRandomSearch()
	{
		return this == RANDOM || this == HYBRID;
	}

}
