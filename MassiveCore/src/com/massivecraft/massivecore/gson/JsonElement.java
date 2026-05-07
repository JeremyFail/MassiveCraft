package com.massivecraft.massivecore.gson;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Abstract node in a JSON tree (object, array, primitive, or null).
 * <p>
 * This type (and subclasses) are the stable public API for MassiveCore and dependent plugins.
 * Internally each instance delegates to a {@link com.google.gson.JsonElement}; at runtime (after
 * shading) that type lives under {@code com.massivecraft.massivecore.lib.gson}. Use
 * {@link #unwrapNative()} only when bridging to code that still uses Gson types directly (e.g.
 * {@code com.google.gson.JsonSerializer} in the {@code adapter} package).
 * </p>
 */
public abstract class JsonElement
{
	/** Backing Gson node; package-visible for subclasses in this package only. */
	final com.google.gson.JsonElement nativeElement;

	/**
	 * Subclasses pass the Gson element to wrap (must not be {@code null}).
	 *
	 * @param nativeElement Gson node to delegate to
	 * @throws NullPointerException if {@code nativeElement} is {@code null}
	 */
	JsonElement(com.google.gson.JsonElement nativeElement)
	{
		if (nativeElement == null) throw new NullPointerException("nativeElement");
		this.nativeElement = nativeElement;
	}

	/**
	 * Exposes the underlying Gson element for native adapters and {@link Gson} internals.
	 *
	 * @return the delegate {@link com.google.gson.JsonElement} (never {@code null} for wrapper instances)
	 */
	public com.google.gson.JsonElement unwrapNative()
	{
		return nativeElement;
	}

	/**
	 * Wraps a Gson element as the appropriate MassiveCore subtype, or returns {@code null}.
	 *
	 * @param element Gson node, or {@code null}
	 * @return {@link JsonNull}, {@link JsonObject}, {@link JsonArray}, or {@link JsonPrimitive} wrapper, or {@code null}
	 * @throws IllegalStateException if Gson introduces an unexpected element type
	 */
	public static JsonElement wrap(com.google.gson.JsonElement element)
	{
		if (element == null) return null;
		if (element.isJsonNull()) return JsonNull.wrap((com.google.gson.JsonNull) element);
		if (element.isJsonObject()) return new JsonObject((com.google.gson.JsonObject) element);
		if (element.isJsonArray()) return new JsonArray((com.google.gson.JsonArray) element);
		if (element.isJsonPrimitive()) return new JsonPrimitive((com.google.gson.JsonPrimitive) element);
		throw new IllegalStateException("Unknown JsonElement: " + element.getClass());
	}

	/**
	 * @return this value as a JSON object (new wrapper around the same Gson instance for non-{@link JsonObject} elements)
	 * @throws IllegalStateException if this element is not a JSON object
	 * @see com.google.gson.JsonElement#getAsJsonObject()
	 */
	public JsonObject getAsJsonObject()
	{
		return new JsonObject(nativeElement.getAsJsonObject());
	}

	/**
	 * @return this value as a JSON array
	 * @throws IllegalStateException if this element is not a JSON array
	 * @see com.google.gson.JsonElement#getAsJsonArray()
	 */
	public JsonArray getAsJsonArray()
	{
		return new JsonArray(nativeElement.getAsJsonArray());
	}

	/**
	 * @return this value as a JSON primitive
	 * @throws IllegalStateException if this element is not a primitive
	 * @see com.google.gson.JsonElement#getAsJsonPrimitive()
	 */
	public JsonPrimitive getAsJsonPrimitive()
	{
		return new JsonPrimitive(nativeElement.getAsJsonPrimitive());
	}

	/**
	 * @return {@code true} if this node is a JSON object
	 */
	public boolean isJsonObject()
	{
		return nativeElement.isJsonObject();
	}

	/**
	 * @return {@code true} if this node is a JSON array
	 */
	public boolean isJsonArray()
	{
		return nativeElement.isJsonArray();
	}

	/**
	 * @return {@code true} if this node is a string, number, or boolean primitive
	 */
	public boolean isJsonPrimitive()
	{
		return nativeElement.isJsonPrimitive();
	}

	/**
	 * @return {@code true} if this node is JSON null
	 */
	public boolean isJsonNull()
	{
		return nativeElement.isJsonNull();
	}

	/**
	 * @return a deep copy of this subtree (new Gson nodes and new wrappers)
	 * @see com.google.gson.JsonElement#deepCopy()
	 */
	public JsonElement deepCopy()
	{
		return wrap(nativeElement.deepCopy());
	}

	/**
	 * @return string form of a primitive, or conversion per Gson rules
	 * @throws UnsupportedOperationException if this element cannot be coerced to string
	 * @throws IllegalStateException         if this element is a JSON null
	 * @see com.google.gson.JsonElement#getAsString()
	 */
	public String getAsString()
	{
		return nativeElement.getAsString();
	}

	/**
	 * @return numeric value as {@code double}
	 * @throws UnsupportedOperationException if not a primitive or not a number
	 * @see com.google.gson.JsonElement#getAsDouble()
	 */
	public double getAsDouble()
	{
		return nativeElement.getAsDouble();
	}

	/**
	 * @return numeric value as {@code float}
	 * @throws UnsupportedOperationException if not a primitive or not a number
	 * @see com.google.gson.JsonElement#getAsFloat()
	 */
	public float getAsFloat()
	{
		return nativeElement.getAsFloat();
	}

	/**
	 * @return numeric value as {@code long}
	 * @throws UnsupportedOperationException if not a primitive or not a number
	 * @see com.google.gson.JsonElement#getAsLong()
	 */
	public long getAsLong()
	{
		return nativeElement.getAsLong();
	}

	/**
	 * @return numeric value as {@code int} (narrowing may lose range)
	 * @throws UnsupportedOperationException if not a primitive or not a number
	 * @see com.google.gson.JsonElement#getAsInt()
	 */
	public int getAsInt()
	{
		return nativeElement.getAsInt();
	}

	/**
	 * @return numeric value as {@code byte}
	 * @throws UnsupportedOperationException if not a primitive or not a number
	 * @see com.google.gson.JsonElement#getAsByte()
	 */
	public byte getAsByte()
	{
		return nativeElement.getAsByte();
	}

	/**
	 * @return first character of string primitive
	 * @throws UnsupportedOperationException if not a string primitive
	 * @see com.google.gson.JsonElement#getAsCharacter()
	 */
	public char getAsCharacter()
	{
		return nativeElement.getAsCharacter();
	}

	/**
	 * @return numeric value as {@code short}
	 * @throws UnsupportedOperationException if not a primitive or not a number
	 * @see com.google.gson.JsonElement#getAsShort()
	 */
	public short getAsShort()
	{
		return nativeElement.getAsShort();
	}

	/**
	 * @return decimal value
	 * @throws UnsupportedOperationException if not a number primitive
	 * @see com.google.gson.JsonElement#getAsBigDecimal()
	 */
	public BigDecimal getAsBigDecimal()
	{
		return nativeElement.getAsBigDecimal();
	}

	/**
	 * @return integer value
	 * @throws UnsupportedOperationException if not a number primitive
	 * @see com.google.gson.JsonElement#getAsBigInteger()
	 */
	public BigInteger getAsBigInteger()
	{
		return nativeElement.getAsBigInteger();
	}

	/**
	 * @return lazy number representation from Gson
	 * @see com.google.gson.JsonElement#getAsNumber()
	 */
	public Number getAsNumber()
	{
		return nativeElement.getAsNumber();
	}

	/**
	 * @return boolean value of a boolean primitive
	 * @throws UnsupportedOperationException if not a boolean primitive
	 * @see com.google.gson.JsonElement#getAsBoolean()
	 */
	public boolean getAsBoolean()
	{
		return nativeElement.getAsBoolean();
	}

	/**
	 * @return compact JSON text for this subtree
	 */
	@Override
	public String toString()
	{
		return nativeElement.toString();
	}

	/**
	 * @param o another object
	 * @return {@code true} if {@code o} is the same wrapper type and delegates to an equal Gson element
	 */
	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JsonElement that = (JsonElement) o;
		return nativeElement.equals(that.nativeElement);
	}

	/**
	 * @return hash code delegated to the Gson element
	 */
	@Override
	public int hashCode()
	{
		return nativeElement.hashCode();
	}
}
