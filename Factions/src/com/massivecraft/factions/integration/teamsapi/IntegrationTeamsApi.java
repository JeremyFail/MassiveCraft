package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.massivecore.Integration;
import com.skyblockexp.teamsapi.api.TeamsAPI;

/**
 * MassiveCraft {@link Integration} that wires Factions as the TeamsAPI provider set.
 * <p>
 * Activation mirrors other optional integrations: runs only while the TeamsAPI plugin is loaded and enabled
 * (via {@link com.massivecraft.massivecore.predicate.PredicateIntegration}). Each successful activation
 * installs all five TeamsAPI bridges; shutting down TeamsAPI or Factions clears registrations defensively.
 */
public class IntegrationTeamsApi extends Integration
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //

	/** Sole {@link Integration} instance resolved by MassiveCraft through {@link com.massivecraft.massivecore.util.ReflectionUtil#getSingletonInstance(Class)}. */
	private static final IntegrationTeamsApi I = new IntegrationTeamsApi();

	/**
	 * @return singleton used when {@link com.massivecraft.factions.Factions#getClassesActiveIntegrations()} activates this integration
	 */
	public static IntegrationTeamsApi get()
	{
		return I;
	}

	/**
	 * Names the remote plugin whose presence controls this integration (must match TeamsAPI's {@code plugin.yml} name).
	 */
	private IntegrationTeamsApi()
	{
		this.setPluginName("TeamsAPI");
	}

	// -------------------------------------------- //
	// STATE
	// -------------------------------------------- //

	/** Live {@link TeamsAPI} registrations; retained so {@link TeamsAPI}'s unregister methods receive the identical instances. */
	private FactionsTeamsService teamsService;
	private FactionsTeamsInviteService inviteService;
	private FactionsTeamsWarpService warpService;
	private FactionsTeamsClaimService claimService;
	private FactionsTeamsPowerService powerService;

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	/**
	 * {@inheritDoc}
	 * <p>
	 * {@code active == true}: instantiates bridges and forwards them to {@link TeamsAPI}. {@code false}: best-effort
	 * teardown in reverse provider order without surfacing throwable failures to callers.
	 */
	@Override
	public void setIntegrationActiveInner(final boolean active)
	{
		if (active)
		{
			final Factions factions = (Factions) this.getPlugin();
			this.unregisterProvidersQuietly();

			this.teamsService = new FactionsTeamsService(factions);
			this.inviteService = new FactionsTeamsInviteService(factions);
			this.warpService = new FactionsTeamsWarpService(factions);
			this.claimService = new FactionsTeamsClaimService();
			this.powerService = new FactionsTeamsPowerService();

			TeamsAPI.registerProvider(factions, this.teamsService);
			TeamsAPI.registerInviteProvider(factions, this.inviteService);
			TeamsAPI.registerWarpProvider(factions, this.warpService);
			TeamsAPI.registerClaimProvider(factions, this.claimService);
			TeamsAPI.registerPowerProvider(factions, this.powerService);
		}
		else
		{
			this.unregisterProvidersQuietly();
		}
	}

	/**
	 * Invokes TeamsAPI unregister hooks swallowing failures, then clears local references - safe to call repeatedly when already idle.
	 */
	private void unregisterProvidersQuietly()
	{
		try
		{
			if (this.powerService != null) TeamsAPI.unregisterPowerProvider(this.powerService);
			if (this.claimService != null) TeamsAPI.unregisterClaimProvider(this.claimService);
			if (this.warpService != null) TeamsAPI.unregisterWarpProvider(this.warpService);
			if (this.inviteService != null) TeamsAPI.unregisterInviteProvider(this.inviteService);
			if (this.teamsService != null) TeamsAPI.unregisterProvider(this.teamsService);
		}
		catch (final Throwable ignored)
		{

		}
		finally
		{
			this.powerService = null;
			this.claimService = null;
			this.warpService = null;
			this.inviteService = null;
			this.teamsService = null;
		}
	}
}
