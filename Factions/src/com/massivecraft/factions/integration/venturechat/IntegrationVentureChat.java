package com.massivecraft.factions.integration.venturechat;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.Integration;

public class IntegrationVentureChat extends Integration
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static IntegrationVentureChat i = new IntegrationVentureChat();
	public static IntegrationVentureChat get() { return i; }
	private IntegrationVentureChat()
	{
		this.setPluginName("VentureChat");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Engine getEngine()
	{
		return EngineVentureChat.get();
	}
	
}
