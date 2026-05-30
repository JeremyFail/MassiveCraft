package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.massivecore.Integration;

/**
 * MassiveCraft {@link Integration} that wires Factions as the TeamsAPI provider set (teams, invites, warps,
 * claims, power, and directional relations).
 * <p>
 * Activation mirrors other optional integrations (e.g. PlaceholderAPI): the TeamsAPI plugin must be loaded and
 * enabled ({@link com.massivecraft.massivecore.predicate.PredicateIntegration}), and {@link TeamsApiVersion} must
 * accept the plugin's runtime API (see {@link #setIntegrationActive(Boolean)}). Providers register only when both
 * checks pass; otherwise no "integration activated" wiring or TeamsAPI provider registration occurs.
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
	public void setIntegrationActive(final Boolean integrationActive)
	{
		Boolean active = integrationActive;
		if (active == null)
		{
			active = this.getPredicate().test(this);
		}
		if (active && !TeamsApiVersion.isRuntimeSupported())
		{
			TeamsApiVersion.logAndCheckRuntimeSupported(((Factions) this.getPlugin()).getLogger());
			if (this.isIntegrationActive())
			{
				super.setIntegrationActive(false);
			}
			return;
		}
		super.setIntegrationActive(active);
	}

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
