package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.skyblockexp.teamsapi.api.TeamsAPI;

/**
 * Holds TeamsAPI provider instances and registration lifecycle. Loaded only via
 * {@link Class#forName(String)} from {@link IntegrationTeamsApi} after the TeamsAPI plugin is
 * present, so Factions can enable without the TeamsAPI API on the classpath.
 */
public final class TeamsApiProviderSession
{
	private final Factions factions;

	private FactionsTeamsService teamsService;
	private FactionsTeamsInviteService inviteService;
	private FactionsTeamsWarpService warpService;
	private FactionsTeamsClaimService claimService;
	private FactionsTeamsPowerService powerService;

	public TeamsApiProviderSession(final Factions factions)
	{
		this.factions = factions;
	}

	public void register()
	{
		this.unregisterQuietly();

		this.teamsService = new FactionsTeamsService(this.factions);
		this.inviteService = new FactionsTeamsInviteService(this.factions);
		this.warpService = new FactionsTeamsWarpService(this.factions);
		this.claimService = new FactionsTeamsClaimService();
		this.powerService = new FactionsTeamsPowerService();

		TeamsAPI.registerProvider(this.factions, this.teamsService);
		TeamsAPI.registerInviteProvider(this.factions, this.inviteService);
		TeamsAPI.registerWarpProvider(this.factions, this.warpService);
		TeamsAPI.registerClaimProvider(this.factions, this.claimService);
		TeamsAPI.registerPowerProvider(this.factions, this.powerService);
	}

	public void unregister()
	{
		this.unregisterQuietly();
	}

	private void unregisterQuietly()
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
