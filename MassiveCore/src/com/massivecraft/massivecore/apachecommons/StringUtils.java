package com.massivecraft.massivecore.apachecommons;

import java.lang.reflect.Array;

public class StringUtils
{
	
	/**
	 * The empty String {@code ""}.
	 * 
	 * @since 2.0
	 */
	public static final String EMPTY = "";
	
	/**
	 * Represents a failed index search.
	 * 
	 * @since 2.1
	 */
	public static final int INDEX_NOT_FOUND = -1;
	
	/**
     * Tests if String contains a search String, handling {@code null}. This method uses {@link indexOf(String)} if possible.
     *
     * <p>
     * A {@code null} String will return {@code false}.
     * </p>
     *
     * <p>
     * Case-sensitive examples
     * </p>
     *
     * <pre>
     * StringUtils.contains(null, *)     = false
     * StringUtils.contains(*, null)     = false
     * StringUtils.contains("", "")      = true
     * StringUtils.contains("abc", "")   = true
     * StringUtils.contains("abc", "a")  = true
     * StringUtils.contains("abc", "z")  = false
     * </pre>
     *
     * @param seq       the String to check, may be null
     * @param searchSeq the String to find, may be null
     * @return true if the String contains the search String, false if not or {@code null} string input
     */
	public static boolean contains(final String str, final String searchStr)
	{
		if (str == null || searchStr == null)
		{
			return false;
		}
		final int len = searchStr.length();
		final int max = str.length() - len;
		for (int i = 0; i <= max; i++)
		{
			if (regionMatches(str, true, i, searchStr, 0, len))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Compares all Strings in an array and returns the initial sequence of
	 * characters that is common to all of them.
	 *
	 * <p>
	 * For example, {@code getCommonPrefix(new String[] {"i am a machine", "i am a
	 * robot"}) -&gt; "i am a "}
	 * </p>
	 *
	 * <pre>
	 * StringUtils.getCommonPrefix(null)                             = ""
	 * StringUtils.getCommonPrefix(new String[] {})                  = ""
	 * StringUtils.getCommonPrefix(new String[] {"abc"})             = "abc"
	 * StringUtils.getCommonPrefix(new String[] {null, null})        = ""
	 * StringUtils.getCommonPrefix(new String[] {"", ""})            = ""
	 * StringUtils.getCommonPrefix(new String[] {"", null})          = ""
	 * StringUtils.getCommonPrefix(new String[] {"abc", null, null}) = ""
	 * StringUtils.getCommonPrefix(new String[] {null, null, "abc"}) = ""
	 * StringUtils.getCommonPrefix(new String[] {"", "abc"})         = ""
	 * StringUtils.getCommonPrefix(new String[] {"abc", ""})         = ""
	 * StringUtils.getCommonPrefix(new String[] {"abc", "abc"})      = "abc"
	 * StringUtils.getCommonPrefix(new String[] {"abc", "a"})        = "a"
	 * StringUtils.getCommonPrefix(new String[] {"ab", "abxyz"})     = "ab"
	 * StringUtils.getCommonPrefix(new String[] {"abcde", "abxyz"})  = "ab"
	 * StringUtils.getCommonPrefix(new String[] {"abcde", "xyz"})    = ""
	 * StringUtils.getCommonPrefix(new String[] {"xyz", "abcde"})    = ""
	 * StringUtils.getCommonPrefix(new String[] {"i am a machine", "i am a robot"}) = "i am a "
	 * </pre>
	 *
	 * @param strs
	 *            array of String objects, entries may be null
	 * @return the initial sequence of characters that are common to all Strings in
	 *         the array; empty String if the array is null, the elements are all
	 *         null or if there is no common prefix.
	 * @since 2.4
	 */
	public static String getCommonPrefix(final String... strs)
	{
		if (strs == null || Array.getLength(strs) == 0)
		{
			return EMPTY;
		}
		final int smallestIndexOfDiff = indexOfDifference(strs);
		if (smallestIndexOfDiff == INDEX_NOT_FOUND)
		{
			// all strings were identical
			if (strs[0] == null)
			{
				return EMPTY;
			}
			return strs[0];
		}
		if (smallestIndexOfDiff == 0)
		{
			// there were no common initial characters
			return EMPTY;
		}
		// we found a common initial character sequence
		return strs[0].substring(0, smallestIndexOfDiff);
	}
	
	/**
	 * Find the Levenshtein distance between two Strings.
	 *
	 * <p>
	 * This is the number of changes needed to change one String into another, where
	 * each change is a single character modification (deletion, insertion or
	 * substitution).
	 * </p>
	 *
	 * <p>
	 * The implementation uses a single-dimensional array of length s.length() + 1.
	 * See <a href=
	 * "https://blog.softwx.net/2014/12/optimizing-levenshtein-algorithm-in-c.html">
	 * https://blog.softwx.net/2014/12/optimizing-levenshtein-algorithm-in-c.html</a>
	 * for details.
	 * </p>
	 *
	 * <pre>
	 * StringUtils.getLevenshteinDistance(null, *)             = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance(*, null)             = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance("", "")              = 0
	 * StringUtils.getLevenshteinDistance("", "a")             = 1
	 * StringUtils.getLevenshteinDistance("aaapppp", "")       = 7
	 * StringUtils.getLevenshteinDistance("frog", "fog")       = 1
	 * StringUtils.getLevenshteinDistance("fly", "ant")        = 3
	 * StringUtils.getLevenshteinDistance("elephant", "hippo") = 7
	 * StringUtils.getLevenshteinDistance("hippo", "elephant") = 7
	 * StringUtils.getLevenshteinDistance("hippo", "zzzzzzzz") = 8
	 * StringUtils.getLevenshteinDistance("hello", "hallo")    = 1
	 * </pre>
	 *
	 * @param s
	 *            the first String, must not be null
	 * @param t
	 *            the second String, must not be null
	 * @return result distance
	 * @throws IllegalArgumentException
	 *             if either String input {@code null}
	 */
	public static int getLevenshteinDistance(String s, String t)
	{
		if (s == null || t == null)
		{
			throw new IllegalArgumentException("Strings must not be null");
		}
		
		int n = s.length();
		int m = t.length();
		
		if (n == 0)
		{
			return m;
		}
		if (m == 0)
		{
			return n;
		}
		
		if (n > m)
		{
			// swap the input strings to consume less memory
			final String tmp = s;
			s = t;
			t = tmp;
			n = m;
			m = t.length();
		}
		
		final int[] p = new int[n + 1];
		// indexes into strings s and t
		int i; // iterates through s
		int j; // iterates through t
		int upperleft;
		int upper;
		
		char jOfT; // jth character of t
		int cost;
		
		for (i = 0; i <= n; i++)
		{
			p[i] = i;
		}
		
		for (j = 1; j <= m; j++)
		{
			upperleft = p[0];
			jOfT = t.charAt(j - 1);
			p[0] = j;
			
			for (i = 1; i <= n; i++)
			{
				upper = p[i];
				cost = s.charAt(i - 1) == jOfT ? 0 : 1;
				// minimum of cell to the left+1, to the top+1, diagonally left and up +cost
				p[i] = Math.min(Math.min(p[i - 1] + 1, p[i] + 1), upperleft + cost);
				upperleft = upper;
			}
		}
		
		return p[n];
	}
	
	public static int indexOf(final String str, final String searchStr, int startPos)
	{
		if (str == null || searchStr == null)
		{
			return INDEX_NOT_FOUND;
		}
		if (startPos < 0)
		{
			startPos = 0;
		}
		final int endLimit = str.length() - searchStr.length() + 1;
		if (startPos > endLimit)
		{
			return INDEX_NOT_FOUND;
		}
		if (searchStr.length() == 0)
		{
			return startPos;
		}
		for (int i = startPos; i < endLimit; i++)
		{
			if (regionMatches(str, true, i, searchStr, 0, searchStr.length()))
			{
				return i;
			}
		}
		return INDEX_NOT_FOUND;
	}
	
	/**
	 * Compares all CharSequences in an array and returns the index at which the
	 * CharSequences begin to differ.
	 *
	 * <p>
	 * For example, {@code indexOfDifference(new String[] {"i am a machine", "i am a
	 * robot"}) -> 7}
	 * </p>
	 *
	 * <pre>
	 * StringUtils.indexOfDifference(null)                             = -1
	 * StringUtils.indexOfDifference(new String[] {})                  = -1
	 * StringUtils.indexOfDifference(new String[] {"abc"})             = -1
	 * StringUtils.indexOfDifference(new String[] {null, null})        = -1
	 * StringUtils.indexOfDifference(new String[] {"", ""})            = -1
	 * StringUtils.indexOfDifference(new String[] {"", null})          = 0
	 * StringUtils.indexOfDifference(new String[] {"abc", null, null}) = 0
	 * StringUtils.indexOfDifference(new String[] {null, null, "abc"}) = 0
	 * StringUtils.indexOfDifference(new String[] {"", "abc"})         = 0
	 * StringUtils.indexOfDifference(new String[] {"abc", ""})         = 0
	 * StringUtils.indexOfDifference(new String[] {"abc", "abc"})      = -1
	 * StringUtils.indexOfDifference(new String[] {"abc", "a"})        = 1
	 * StringUtils.indexOfDifference(new String[] {"ab", "abxyz"})     = 2
	 * StringUtils.indexOfDifference(new String[] {"abcde", "abxyz"})  = 2
	 * StringUtils.indexOfDifference(new String[] {"abcde", "xyz"})    = 0
	 * StringUtils.indexOfDifference(new String[] {"xyz", "abcde"})    = 0
	 * StringUtils.indexOfDifference(new String[] {"i am a machine", "i am a robot"}) = 7
	 * </pre>
	 *
	 * @param css
	 *            array of CharSequences, entries may be null
	 * @return the index where the strings begin to differ; -1 if they are all equal
	 * @since 2.4
	 * @since 3.0 Changed signature from indexOfDifference(String...) to
	 *        indexOfDifference(CharSequence...)
	 */
	public static int indexOfDifference(final String... css)
	{
		if (css == null || Array.getLength(css) <= 1)
		{
			return INDEX_NOT_FOUND;
		}
		boolean anyStringNull = false;
		boolean allStringsNull = true;
		final int arrayLen = css.length;
		int shortestStrLen = Integer.MAX_VALUE;
		int longestStrLen = 0;
		
		// find the min and max string lengths; this avoids checking to make
		// sure we are not exceeding the length of the string each time through
		// the bottom loop.
		for (final CharSequence cs : css)
		{
			if (cs == null)
			{
				anyStringNull = true;
				shortestStrLen = 0;
			}
			else
			{
				allStringsNull = false;
				shortestStrLen = Math.min(cs.length(), shortestStrLen);
				longestStrLen = Math.max(cs.length(), longestStrLen);
			}
		}
		
		// handle lists containing all nulls or all empty strings
		if (allStringsNull || longestStrLen == 0 && !anyStringNull)
		{
			return INDEX_NOT_FOUND;
		}
		
		// handle lists containing some nulls or some empty strings
		if (shortestStrLen == 0)
		{
			return 0;
		}
		
		// find the position with the first difference across all strings
		int firstDiff = -1;
		for (int stringPos = 0; stringPos < shortestStrLen; stringPos++)
		{
			final char comparisonChar = css[0].charAt(stringPos);
			for (int arrayPos = 1; arrayPos < arrayLen; arrayPos++)
			{
				if (css[arrayPos].charAt(stringPos) != comparisonChar)
				{
					firstDiff = stringPos;
					break;
				}
			}
			if (firstDiff != -1)
			{
				break;
			}
		}
		
		if (firstDiff == -1 && shortestStrLen != longestStrLen)
		{
			// we compared all of the characters up to the length of the
			// shortest string and didn't find a match, but the string lengths
			// vary, so return the length of the shortest string.
			return shortestStrLen;
		}
		return firstDiff;
	}
	
	/**
	 * Tests if a String is empty ("") or null.
	 *
	 * <pre>
	 * StringUtils.isEmpty(null)      = true
	 * StringUtils.isEmpty("")        = true
	 * StringUtils.isEmpty(" ")       = false
	 * StringUtils.isEmpty("bob")     = false
	 * StringUtils.isEmpty("  bob  ") = false
	 * </pre>
	 *
	 * <p>
	 * NOTE: This method changed in Lang version 2.0. It no longer trims the String.
	 * That functionality is available in isBlank().
	 * </p>
	 *
	 * @param cs
	 *            the String to check, may be null
	 * @return {@code true} if the String is empty or null
	 */
	public static boolean isEmpty(final String cs)
	{
		return cs == null || cs.length() == 0;
	}
	
	/**
	 * Green implementation of regionMatches.
	 *
	 * @param cs
	 *            the {@link String} to be processed
	 * @param ignoreCase
	 *            whether or not to be case-insensitive
	 * @param thisStart
	 *            the index to start on the {@code cs} String
	 * @param substring
	 *            the {@link String} to be looked for
	 * @param start
	 *            the index to start on the {@code substring} String
	 * @param length
	 *            character length of the region
	 * @return whether the region matched
	 * @see String#regionMatches(boolean, int, String, int, int)
	 */
	public static boolean regionMatches(final String cs, final boolean ignoreCase, final int thisStart,
			final CharSequence substring, final int start, final int length)
	{
		if (cs instanceof String && substring instanceof String)
		{
			return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
		}
		int index1 = thisStart;
		int index2 = start;
		int tmpLen = length;
		
		// Extract these first so we detect NPEs the same as the java.lang.String
		// version
		final int srcLen = cs.length() - thisStart;
		final int otherLen = substring.length() - start;
		
		// Check for invalid parameters
		if (thisStart < 0 || start < 0 || length < 0)
		{
			return false;
		}
		
		// Check that the regions are long enough
		if (srcLen < length || otherLen < length)
		{
			return false;
		}
		
		while (tmpLen-- > 0)
		{
			final char c1 = cs.charAt(index1++);
			final char c2 = substring.charAt(index2++);
			
			if (c1 == c2)
			{
				continue;
			}
			
			if (!ignoreCase)
			{
				return false;
			}
			
			// The real same check as in String#regionMatches(boolean, int, String, int,
			// int):
			final char u1 = Character.toUpperCase(c1);
			final char u2 = Character.toUpperCase(c2);
			if (u1 != u2 && Character.toLowerCase(u1) != Character.toLowerCase(u2))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
     * Case insensitively replaces all occurrences of a String within another String.
     *
     * <p>
     * A {@code null} reference passed to this method is a no-op.
     * </p>
     *
     * <p>
     * Case-sensitive examples
     * </p>
     *
     * <pre>
     * Strings.CS.replace(null, *, *)        = null
     * Strings.CS.replace("", *, *)          = ""
     * Strings.CS.replace("any", null, *)    = "any"
     * Strings.CS.replace("any", *, null)    = "any"
     * Strings.CS.replace("any", "", *)      = "any"
     * Strings.CS.replace("aba", "a", null)  = "aba"
     * Strings.CS.replace("aba", "a", "")    = "b"
     * Strings.CS.replace("aba", "a", "z")   = "zbz"
     * </pre>
     *
     * @see #replace(String text, String searchString, String replacement, int max)
     * @param text         text to search and replace in, may be null
     * @param searchString the String to search for (case-insensitive), may be null
     * @param replacement  the String to replace it with, may be null
     * @return the text with any replacements processed, {@code null} if null String input
     */
    public static String replace(final String text, final String searchString, final String replacement) {
        return replace(text, searchString, replacement, -1);
    }
	
	/**
	 * Replaces a String with another String inside a larger String, for the first
	 * {@code max} values of the search String.
	 *
	 * <p>
	 * A {@code null} reference passed to this method is a no-op.
	 * </p>
	 *
	 * <p>
	 * Case-sensitive examples
	 * </p>
	 *
	 * <pre>
	 * StringUtils.replace(null, *, *, *)         = null
	 * StringUtils.replace("", *, *, *)           = ""
	 * StringUtils.replace("any", null, *, *)     = "any"
	 * StringUtils.replace("any", *, null, *)     = "any"
	 * StringUtils.replace("any", "", *, *)       = "any"
	 * StringUtils.replace("any", *, *, 0)        = "any"
	 * StringUtils.replace("abaa", "a", null, -1) = "abaa"
	 * StringUtils.replace("abaa", "a", "", -1)   = "b"
	 * StringUtils.replace("abaa", "a", "z", 0)   = "abaa"
	 * StringUtils.replace("abaa", "a", "z", 1)   = "zbaa"
	 * StringUtils.replace("abaa", "a", "z", 2)   = "zbza"
	 * StringUtils.replace("abaa", "a", "z", -1)  = "zbzz"
	 * </pre>
	 *
	 * @param text
	 *            text to search and replace in, may be null
	 * @param searchString
	 *            the String to search for (case-insensitive), may be null
	 * @param replacement
	 *            the String to replace it with, may be null
	 * @param max
	 *            maximum number of values to replace, or {@code -1} if no maximum
	 * @return the text with any replacements processed, {@code null} if null String
	 *         input
	 */
	public static String replace(final String text, String searchString, final String replacement, int max)
	{
		if (StringUtils.isEmpty(text) || StringUtils.isEmpty(searchString) || replacement == null || max == 0)
		{
			return text;
		}
		// if (ignoreCase) {
		// searchString = searchString.toLowerCase();
		// }
		int start = 0;
		int end = indexOf(text, searchString, start);
		if (end == INDEX_NOT_FOUND)
		{
			return text;
		}
		final int replLength = searchString.length();
		int increase = Math.max(replacement.length() - replLength, 0);
		increase *= max < 0 ? 16 : Math.min(max, 64);
		final StringBuilder buf = new StringBuilder(text.length() + increase);
		while (end != INDEX_NOT_FOUND)
		{
			buf.append(text, start, end).append(replacement);
			start = end + replLength;
			if (--max == 0)
			{
				break;
			}
			end = indexOf(text, searchString, start);
		}
		buf.append(text, start, text.length());
		return buf.toString();
	}
	
}
