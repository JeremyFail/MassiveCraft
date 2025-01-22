package com.massivecraft.massivecore.predicate;

@Deprecated
public interface Predicate<T> extends java.util.function.Predicate<T>
{
	boolean apply(T type);

	@Override
	default boolean test(T type)
	{
		return this.apply(type);
	}
}
