package com.massivecraft.massivecore.store.accessor;

public interface FieldAccessor
{
	Object get(Object entity);
	void set(Object entity, Object val);

}
