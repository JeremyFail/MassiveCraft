package com.massivecraft.massivecore.gson;

/**
 * Thrown when Gson encounters an I/O problem while reading or writing JSON during parse/serialize.
 * <p>
 * Corresponds to {@link com.google.gson.JsonIOException} in the underlying Gson implementation.
 * </p>
 */
public class JsonIOException extends JsonParseException
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param msg the detail message (may be {@code null})
	 */
	public JsonIOException(String msg)
	{
		super(msg);
	}

	/**
	 * @param msg   the detail message (may be {@code null})
	 * @param cause the underlying cause (may be {@code null})
	 */
	public JsonIOException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	/**
	 * @param cause the underlying cause (may be {@code null})
	 */
	public JsonIOException(Throwable cause)
	{
		super(cause);
	}
}
