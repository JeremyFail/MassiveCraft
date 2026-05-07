package com.massivecraft.massivecore.gson;

/**
 * Thrown when JSON input is not valid syntactically (malformed JSON, wrong token, etc.).
 * <p>
 * Corresponds to {@link com.google.gson.JsonSyntaxException}. This is the typical exception from
 * {@link Gson#fromJson(String, Class)} and related methods when input cannot be parsed.
 * </p>
 */
public class JsonSyntaxException extends JsonIOException
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param msg the detail message (may be {@code null})
	 */
	public JsonSyntaxException(String msg)
	{
		super(msg);
	}

	/**
	 * @param msg   the detail message (may be {@code null})
	 * @param cause the underlying cause (may be {@code null})
	 */
	public JsonSyntaxException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	/**
	 * @param cause the underlying cause (may be {@code null})
	 */
	public JsonSyntaxException(Throwable cause)
	{
		super(cause);
	}
}
