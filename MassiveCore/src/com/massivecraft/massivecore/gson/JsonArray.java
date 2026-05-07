package com.massivecraft.massivecore.gson;

import java.util.Iterator;

/**
 * A JSON array: ordered list of {@link JsonElement} values.
 * <p>
 * Implements {@link Iterable} for enhanced for-loops. All mutating operations affect the same
 * underlying Gson array as {@link #unwrapNative()}.
 * </p>
 */
public final class JsonArray extends JsonElement implements Iterable<JsonElement>
{
	/**
	 * Creates an empty JSON array.
	 */
	public JsonArray()
	{
		super(new com.google.gson.JsonArray());
	}

	/**
	 * Wraps an existing Gson array (used by {@link JsonElement#wrap} and internal bridges).
	 *
	 * @param nativeArray the Gson array to delegate to (must not be {@code null})
	 */
	JsonArray(com.google.gson.JsonArray nativeArray)
	{
		super(nativeArray);
	}

	// Underlying Gson array (same reference as JsonElement.nativeElement).
	private com.google.gson.JsonArray nativeArray()
	{
		return (com.google.gson.JsonArray) nativeElement;
	}

	@Override
	public JsonArray getAsJsonArray()
	{
		return this;
	}

	/**
	 * Appends a JSON boolean (boxed) to the end of the array.
	 *
	 * @param bool the value to append (must not be {@code null})
	 */
	public void add(Boolean bool)
	{
		nativeArray().add(bool);
	}

	/**
	 * Appends a character as a JSON string primitive of length one.
	 *
	 * @param character the value to append (must not be {@code null})
	 */
	public void add(Character character)
	{
		nativeArray().add(character);
	}

	/**
	 * Appends a JSON number to the end of the array.
	 *
	 * @param number the value to append (must not be {@code null})
	 */
	public void add(Number number)
	{
		nativeArray().add(number);
	}

	/**
	 * Appends a JSON string to the end of the array.
	 *
	 * @param string the value to append
	 */
	public void add(String string)
	{
		nativeArray().add(string);
	}

	/**
	 * Appends an arbitrary JSON element.
	 *
	 * @param element the subtree to append; {@code null} is forwarded to Gson (may become JSON null)
	 */
	public void add(JsonElement element)
	{
		if (element == null) nativeArray().add((com.google.gson.JsonElement) null);
		else nativeArray().add(element.unwrapNative());
	}

	/**
	 * Appends all elements of {@code array} to this array (same as Gson's {@code addAll}).
	 *
	 * @param array the source array (must not be {@code null})
	 */
	public void addAll(JsonArray array)
	{
		nativeArray().addAll(array.nativeArray());
	}

	/**
	 * Replaces the element at {@code index}.
	 *
	 * @param index   zero-based index
	 * @param element new value; {@code null} is passed through to Gson
	 * @return the previous element at {@code index}, wrapped, or {@code null}
	 */
	public JsonElement set(int index, JsonElement element)
	{
		return JsonElement.wrap(nativeArray().set(index, element == null ? null : element.unwrapNative()));
	}

	/**
	 * Removes the element at {@code index} (shifts subsequent elements).
	 *
	 * @param index zero-based index
	 */
	public void remove(int index)
	{
		nativeArray().remove(index);
	}

	/**
	 * @param i zero-based index
	 * @return the element at {@code i}, wrapped; {@code null} if Gson stores null (unusual)
	 */
	public JsonElement get(int i)
	{
		return JsonElement.wrap(nativeArray().get(i));
	}

	/**
	 * @return number of elements in the array
	 */
	public int size()
	{
		return nativeArray().size();
	}

	/**
	 * Iterates over array elements in order (each {@link #next()} is a fresh wrapper view).
	 *
	 * @return iterator over wrapped elements
	 */
	@Override
	public Iterator<JsonElement> iterator()
	{
		final Iterator<com.google.gson.JsonElement> it = nativeArray().iterator();
		return new Iterator<JsonElement>()
		{
			@Override
			public boolean hasNext()
			{
				return it.hasNext();
			}

			@Override
			public JsonElement next()
			{
				// Wrap each Gson node as our public JsonElement subtype.
				return JsonElement.wrap(it.next());
			}
		};
	}
}
