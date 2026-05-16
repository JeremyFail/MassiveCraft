package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.skyblockexp.teamsapi.api.TeamsPowerService;

import java.util.Optional;
import java.util.UUID;

/**
 * Exposes MassiveCraft player and faction power aggregates to TeamsAPI without additional event wiring.
 */
public final class FactionsTeamsPowerService implements TeamsPowerService
{
	/**
	 * {@inheritDoc}
	 * <p>
	 * Null player id returns zero; otherwise resolves through {@link TeamsApiFactionFacade#resolvePlayer(UUID)}.
	 */
	@Override
	public double getPlayerPower(UUID playerUUID)
	{
		if (playerUUID == null) return 0;

		final MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);

		return mplayer.getPower();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getPlayerMaxPower(UUID playerUUID)
	{
		if (playerUUID == null) return 0;

		final MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);

		return mplayer.getPowerMax();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Values are clamped through {@link MPlayer#getLimitedPower(double)} consistent with MassiveCraft power rules.
	 */
	@Override
	public boolean setPlayerPower(UUID playerUUID, double power)
	{
		if (playerUUID == null) return false;

		final MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);

		mplayer.setPower(mplayer.getLimitedPower(power));

		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unknown teams return {@code 0}.
	 */
	@Override
	public double getTeamPower(UUID teamId)
	{
		if (teamId == null) return 0;

		final Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);

		return facOpt.map(Faction::getPower).orElse(0D);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getTeamMaxPower(UUID teamId)
	{
		if (teamId == null) return 0;

		final Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);

		return facOpt.map(Faction::getPowerMax).orElse(0D);
	}
}
