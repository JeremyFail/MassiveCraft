package com.massivecraft.creativegates.cmd.type;

import com.massivecraft.creativegates.gate.GateSetting;
import com.massivecraft.massivecore.command.type.TypeAbstractChoice;

/**
 * Command type for {@link GateSetting} ids (secret, entry, exit, …).
 */
public class TypeGateSetting extends TypeAbstractChoice<GateSetting>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final TypeGateSetting i = new TypeGateSetting();
	public static TypeGateSetting get() { return i; }
	
	public TypeGateSetting()
	{
		super(GateSetting.class);
		this.setAll(GateSetting.values());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String getName()
	{
		return "gate setting";
	}
	
	@Override
	public String getNameInner(GateSetting value)
	{
		return value.getDisplayName();
	}
	
	@Override
	public String getIdInner(GateSetting value)
	{
		return value.getId();
	}
	
}
