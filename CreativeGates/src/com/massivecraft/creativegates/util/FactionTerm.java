package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.massivecore.util.Txt;

/**
 * Configurable display word for FACTION scope messaging (default {@code faction}).
 */
public final class FactionTerm
{
	private FactionTerm()
	{
	}
	
	/**
	 * Configured term in lowercase-ish form as stored (trimmed); never empty.
	 *
	 * @return Display term, e.g. {@code faction}, {@code guild}, {@code nation}.
	 */
	public static String get()
	{
		String term = MConf.get().getFactionTerm();
		if (term == null) return "faction";
		term = term.trim();
		return term.isEmpty() ? "faction" : term;
	}
	
	/**
	 * Capitalized display term ({@link Txt#upperCaseFirst(String)}).
	 *
	 * @return e.g. {@code Faction}.
	 */
	public static String capital()
	{
		return Txt.upperCaseFirst(get());
	}
	
	/**
	 * Possessive form ({@code faction's} / {@code factions'}-style when ending in s).
	 *
	 * @return Possessive display term.
	 */
	public static String possessive()
	{
		String term = get();
		char last = term.charAt(term.length() - 1);
		if (last == 's' || last == 'S') return term + "'";
		return term + "'s";
	}
}
