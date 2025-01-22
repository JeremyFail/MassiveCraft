/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.massivecraft.massivecore.apachecommons;

import java.lang.reflect.Array;

/**
 * This class assists in validating arguments. The validation methods are based
 * along the following principles:
 * <ul>
 * <li>An invalid {@code null} argument causes a
 * {@link NullPointerException}.</li>
 * <li>A non-{@code null} argument causes an
 * {@link IllegalArgumentException}.</li>
 * <li>An invalid index into an array/collection/map/string causes an
 * {@link IndexOutOfBoundsException}.</li>
 * </ul>
 *
 * <p>
 * All exceptions messages are <a href=
 * "https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax">format
 * strings</a> as defined by the Java platform. For example:
 *
 * <pre>
 * Validate.isTrue(i &gt; 0, "The value must be greater than zero: %d", i);
 * Validate.notNull(surname, "The surname must not be %s", null);
 * </pre>
 *
 * <p>
 * #ThreadSafe#
 * </p>
 * 
 * @see String#format(String, Object...)
 * @since 2.0
 */
public class Validate
{
	
	private static final String DEFAULT_IS_TRUE_EX_MESSAGE = "The validated expression is false";
	
	/**
	 * Validate that the argument condition is {@code true}; otherwise throwing an
	 * exception. This method is useful when validating according to an arbitrary
	 * boolean expression, such as validating a primitive number or using your own
	 * custom validation expression.
	 *
	 * <pre>
	 * Validate.isTrue(i &gt; 0);
	 * Validate.isTrue(myObject.isOk());
	 * </pre>
	 *
	 * <p>
	 * The message of the exception is &quot;The validated expression is
	 * false&quot;.
	 * </p>
	 *
	 * @param expression
	 *            the boolean expression to check
	 * @throws IllegalArgumentException
	 *             if expression is {@code false}
	 * @see #isTrue(boolean, String, long)
	 * @see #isTrue(boolean, String, double)
	 * @see #isTrue(boolean, String, Object...)
	 */
	public static void isTrue(final boolean expression)
	{
		if (!expression)
		{
			throw new IllegalArgumentException(DEFAULT_IS_TRUE_EX_MESSAGE);
		}
	}
	
	/**
	 * Validate that the argument condition is {@code true}; otherwise throwing an
	 * exception with the specified message. This method is useful when validating
	 * according to an arbitrary boolean expression, such as validating a primitive
	 * number or using your own custom validation expression.
	 *
	 * <pre>
	 * Validate.isTrue(d &gt; 0.0, "The value must be greater than zero: &#37;s", d);
	 * </pre>
	 *
	 * <p>
	 * For performance reasons, the double value is passed as a separate parameter
	 * and appended to the exception message only in the case of an error.
	 * </p>
	 *
	 * @param expression
	 *            the boolean expression to check
	 * @param message
	 *            the {@link String#format(String, Object...)} exception message if
	 *            invalid, not null
	 * @param value
	 *            the value to append to the message when invalid
	 * @throws IllegalArgumentException
	 *             if expression is {@code false}
	 * @see #isTrue(boolean)
	 * @see #isTrue(boolean, String, long)
	 * @see #isTrue(boolean, String, Object...)
	 */
	public static void isTrue(final boolean expression, final String message, final double value)
	{
		if (!expression)
		{
			throw new IllegalArgumentException(String.format(message, Double.valueOf(value)));
		}
	}
	
	/**
	 * Validate that the argument condition is {@code true}; otherwise throwing an
	 * exception with the specified message. This method is useful when validating
	 * according to an arbitrary boolean expression, such as validating a primitive
	 * number or using your own custom validation expression.
	 *
	 * <pre>
	 * Validate.isTrue(i &gt; 0.0, "The value must be greater than zero: &#37;d", i);
	 * </pre>
	 *
	 * <p>
	 * For performance reasons, the long value is passed as a separate parameter and
	 * appended to the exception message only in the case of an error.
	 * </p>
	 *
	 * @param expression
	 *            the boolean expression to check
	 * @param message
	 *            the {@link String#format(String, Object...)} exception message if
	 *            invalid, not null
	 * @param value
	 *            the value to append to the message when invalid
	 * @throws IllegalArgumentException
	 *             if expression is {@code false}
	 * @see #isTrue(boolean)
	 * @see #isTrue(boolean, String, double)
	 * @see #isTrue(boolean, String, Object...)
	 */
	public static void isTrue(final boolean expression, final String message, final long value)
	{
		if (!expression)
		{
			throw new IllegalArgumentException(String.format(message, Long.valueOf(value)));
		}
	}
	
	/**
	 * Validate that the argument condition is {@code true}; otherwise throwing an
	 * exception with the specified message. This method is useful when validating
	 * according to an arbitrary boolean expression, such as validating a primitive
	 * number or using your own custom validation expression.
	 *
	 * <pre>{@code
	 * Validate.isTrue(i >= min &amp;&amp; i <= max, "The value must be between %d and %d", min, max);
	 * }</pre>
	 *
	 * @param expression
	 *            the boolean expression to check
	 * @param message
	 *            the {@link String#format(String, Object...)} exception message if
	 *            invalid, not null
	 * @param values
	 *            the optional values for the formatted exception message, null
	 *            array not recommended
	 * @throws IllegalArgumentException
	 *             if expression is {@code false}
	 * @see #isTrue(boolean)
	 * @see #isTrue(boolean, String, long)
	 * @see #isTrue(boolean, String, double)
	 */
	public static void isTrue(final boolean expression, final String message, final Object... values)
	{
		if (!expression)
		{
			if (values != null && Array.getLength(values) > 0)
			{
				throw new IllegalArgumentException(String.format(message, values));
			}
			else
			{
				throw new IllegalArgumentException(message);
			}
		}
	}
}