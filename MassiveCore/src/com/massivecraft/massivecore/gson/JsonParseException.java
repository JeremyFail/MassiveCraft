package com.massivecraft.massivecore.gson;

/**
 * Base runtime exception for JSON parse failures in the MassiveCore Gson API.
 * <p>
 * Mirrors the role of {@link com.google.gson.JsonParseException} so dependent plugins never need to
 * reference shaded {@code com.google.gson} (or {@code lib.gson}) types.
 * </p>
 */
public class JsonParseException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an exception with a detail message.
	 *
	 * @param msg the detail message (may be {@code null})
	 */
	public JsonParseException(String msg)
	{
		super(msg);
	}

	/**
	 * Constructs an exception with a detail message and cause.
	 *
	 * @param msg   the detail message (may be {@code null})
	 * @param cause the underlying cause (may be {@code null})
	 */
	public JsonParseException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	/**
	 * Constructs an exception wrapping another throwable.
	 *
	 * @param cause the underlying cause (may be {@code null})
	 */
	public JsonParseException(Throwable cause)
	{
		super(cause);
	}
}
