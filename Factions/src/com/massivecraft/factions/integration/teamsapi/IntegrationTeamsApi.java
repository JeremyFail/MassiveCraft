package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.massivecore.Integration;

/**
 * MassiveCraft {@link Integration} that wires Factions as the TeamsAPI provider set.
 * <p>
 * Activation mirrors other optional integrations: runs only while the TeamsAPI plugin is loaded and enabled
 * (via {@link com.massivecraft.massivecore.predicate.PredicateIntegration}).
 * <p>
 * TeamsAPI-facing types live in {@link TeamsApiProviderSession} and are loaded only when integration
 * activates, so {@link com.massivecraft.massivecore.util.ReflectionUtil#getSingletonInstance(Class)}
 * can instantiate this class without the TeamsAPI API on the classpath.
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

	/** Non-null while providers are registered with TeamsAPI; type is {@link TeamsApiProviderSession}. */
	private Object providerSession;

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void setIntegrationActiveInner(final boolean active)
	{
		if (active)
		{
			final Factions factions = (Factions) this.getPlugin();
			try
			{
				final Class<?> sessionClass = Class.forName(
					"com.massivecraft.factions.integration.teamsapi.TeamsApiProviderSession");
				final Object session = sessionClass.getConstructor(Factions.class).newInstance(factions);
				sessionClass.getMethod("register").invoke(session);
				this.providerSession = session;
			}
			catch (final ReflectiveOperationException e)
			{
				this.providerSession = null;
				throw new RuntimeException(e);
			}
		}
		else
		{
			final Object session = this.providerSession;
			this.providerSession = null;
			if (session != null)
			{
				try
				{
					session.getClass().getMethod("unregister").invoke(session);
				}
				catch (final ReflectiveOperationException ignored)
				{

				}
			}
		}
	}
}
