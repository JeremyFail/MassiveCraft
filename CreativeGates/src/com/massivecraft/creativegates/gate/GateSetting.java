package com.massivecraft.creativegates.gate;

import com.massivecraft.creativegates.entity.UGate;

/**
 * Configurable boolean settings exposed by inspect/manage UIs.
 */
public enum GateSetting
{
	SECRET("secret", "Secret", "Only the creator can read full gate inscriptions."),
	ENTRY("entry", "Entry", "Whether entities may enter this gate."),
	EXIT("exit", "Exit", "Whether this gate may be used as a destination."),
	PLAYERS("players", "Players", "Whether players may travel through this gate."),
	MOBS("mobs", "Mobs", "Whether mobs may travel through this gate."),
	VEHICLES("vehicles", "Vehicles", "Whether vehicles may travel through this gate."),
	;
	
	private final String id;
	private final String displayName;
	private final String description;
	
	/**
	 * Creates a new gate setting.
	 * 
	 * @param id Setting id.
	 * @param displayName Setting display name.
	 * @param description Setting description.
	 */
	GateSetting(String id, String displayName, String description)
	{
		this.id = id;
		this.displayName = displayName;
		this.description = description;
	}
	
	public String getId() { return this.id; }
	public String getDisplayName() { return this.displayName; }
	public String getDescription() { return this.description; }
	
	/**
	 * Resolves a setting by id (case-insensitive).
	 *
	 * @param id Setting id.
	 * @return Setting or null.
	 */
	public static GateSetting get(String id)
	{
		if (id == null) return null;
		for (GateSetting setting : values())
		{
			if (setting.id.equalsIgnoreCase(id) || setting.name().equalsIgnoreCase(id))
			{
				return setting;
			}
		}
		return null;
	}
	
	/**
	 * Reads the current effective value from the gate.
	 * 
	 * @param gate The gate to read the value from.
	 * @return The current effective value.
	 */
	public boolean get(UGate gate)
	{
		switch (this)
		{
			case SECRET: return gate.isRestricted();
			case ENTRY: return gate.isEnterEnabled();
			case EXIT: return gate.isExitEnabled();
			case PLAYERS: return gate.isAllowPlayers();
			case MOBS: return gate.isAllowMobs();
			case VEHICLES: return gate.isAllowVehicles();
			default: return false;
		}
	}
	
	/**
	 * Writes the value onto the gate.
	 * 
	 * @param gate The gate to write the value to.
	 * @param value The value to write.
	 */
	public void set(UGate gate, boolean value)
	{
		switch (this)
		{
			case SECRET:
				gate.setRestricted(value);
				break;
			case ENTRY:
				gate.setEnterEnabled(value);
				break;
			case EXIT:
				gate.setExitEnabled(value);
				break;
			case PLAYERS:
				gate.setAllowPlayers(value);
				break;
			case MOBS:
				gate.setAllowMobs(value);
				break;
			case VEHICLES:
				gate.setAllowVehicles(value);
				break;
			default:
				break;
		}
	}
	
}
