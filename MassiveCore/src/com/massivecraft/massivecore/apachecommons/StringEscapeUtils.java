package com.massivecraft.massivecore.apachecommons;

public class StringEscapeUtils
{
	
	private static final String[][] HTML_ESCAPE_CHARS = { 
		{ "\"", "&quot;" }, 
		{ "&", "&amp;" }, 
		{ "<", "&lt;" },
		{ ">", "&gt;" } 
	};
	
	public static String escapeHtml(String input)
	{
		if (input == null)
		{
			return null;
		}
		StringBuilder escaped = new StringBuilder();
		for (char c : input.toCharArray())
		{
			String replacement = getReplacement(c);
			if (replacement != null)
			{
				escaped.append(replacement);
			}
			else
			{
				escaped.append(c);
			}
		}
		return escaped.toString();
	}
	
	private static String getReplacement(char c)
	{
		for (String[] escape : HTML_ESCAPE_CHARS)
		{
			if (escape[0].charAt(0) == c)
			{
				return escape[1];
			}
		}
		return null;
	}
	
}
