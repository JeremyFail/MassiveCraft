package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.massivecore.Integration;

/**
 * MassiveCraft {@link Integration} that wires Factions as the TeamsAPI provider set (teams, invites, warps,
 * claims, power, and directional relations).
 * <p>
 * Activation mirrors other optional integrations (e.g. PlaceholderAPI): runs only while the TeamsAPI plugin
 * is loaded and enabled ({@link com.massivecraft.massivecore.predicate.PredicateIntegration}).
 * {@link TeamsApiProviderSession} is used only from {@link #setIntegrationActiveInner(boolean)}; version checks
 * run inside {@link TeamsApiProviderSession#register()}.
 */
public class IntegrationTeamsApi extends Integration
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //

	private static final IntegrationTeamsApi I = new IntegrationTeamsApi();

	public static IntegrationTeamsApi get()
	{
		return I;
	}

	private IntegrationTeamsApi()
	{
		this.setPluginName("TeamsAPI");
	}

	// -------------------------------------------- //
	// STATE
	// -------------------------------------------- //

	private TeamsApiProviderSession providerSession;

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void setIntegrationActiveInner(final boolean active)
	{
		if (active)
		{
			final Factions factions = (Factions) this.getPlugin();
			final TeamsApiProviderSession session = new TeamsApiProviderSession(factions);
			if (session.register())
			{
				this.providerSession = session;
			}
		}
		else
		{
			final TeamsApiProviderSession session = this.providerSession;
			this.providerSession = null;
			if (session != null)
			{
				session.unregister();
			}
		}
	}
}
