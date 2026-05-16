package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.skyblockexp.teamsapi.api.TeamsClaimService;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.IdUtil;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implements chunk claim operations through MassiveCraft's {@link MPlayer#tryClaim(Faction, java.util.Collection)} pipeline
 * ({@link BoardColl} lookups, permission-backed validation).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class FactionsTeamsClaimService implements TeamsClaimService
{
	/**
	 * {@inheritDoc}
	 * <p>
	 * Requires an online/sync {@link CommandSender}; offline-only players fail fast with {@code false}.
	 */
	@Override
	public boolean claimChunk(final UUID teamId, final UUID playerUUID, final String worldName, final int chunkX,
		final int chunkZ)
	{
		if (teamId == null || playerUUID == null || worldName == null) return false;

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;

		MPlayer actor = TeamsApiFactionFacade.resolvePlayer(playerUUID);
		if (actor.getSender() == null) return false;

		PS ps = PS.valueOf(worldName, chunkX, chunkZ);

		return actor.tryClaim(facOpt.get(), Collections.singleton(ps));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * No-op unless the faction at the coordinate matches the requesting team - prevents wiping foreign claims.
	 */
	@Override
	public boolean unclaimChunk(final UUID teamId, final UUID playerUUID, final String worldName, final int chunkX,
		final int chunkZ)
	{
		if (teamId == null || playerUUID == null || worldName == null) return false;

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;

		MPlayer actor = TeamsApiFactionFacade.resolvePlayer(playerUUID);
		if (actor.getSender() == null) return false;

		PS ps = PS.valueOf(worldName, chunkX, chunkZ);

		if (BoardColl.get().getFactionAt(ps) != facOpt.get()) return false;

		return actor.tryClaim(FactionColl.get().getNone(), Collections.singleton(ps));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * MassiveClaims path: impersonate console-as-{@link MPlayer} with override briefly so wilderness assignment respects
	 * admin-style permissions without requiring API callers to have player objects.
	 */
	@Override
	public boolean unclaimAll(final UUID teamId)
	{
		if (teamId == null) return false;

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;

		Set<PS> chunks = BoardColl.get().getChunks(facOpt.get());
		if (chunks.isEmpty()) return false;

		MPlayer consoleAsPlayer = MPlayer.get(IdUtil.CONSOLE_ID);
		boolean prevOverride = consoleAsPlayer.isOverriding();

		consoleAsPlayer.setOverriding(true);

		try
		{
			return consoleAsPlayer.tryClaim(FactionColl.get().getNone(), chunks);
		}
		finally
		{
			consoleAsPlayer.setOverriding(prevOverride);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional getClaimAt(final String worldName, final int chunkX, final int chunkZ)
	{
		if (worldName == null) return Optional.empty();

		PS ps = PS.valueOf(worldName, chunkX, chunkZ);
		Faction owner = BoardColl.get().getFactionAt(ps);

		if (!TeamsApiFactionFacade.isEligible(owner)) return Optional.empty();

		UUID tid = TeamsApiFactionFacade.teamUuid(owner);

		return Optional.of(new TeamsApiSnapshotTeamClaim(tid, worldName, chunkX, chunkZ, Instant.EPOCH));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection getTeamClaims(final UUID teamId)
	{
		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return Collections.emptyList();

		UUID tid = TeamsApiFactionFacade.teamUuid(facOpt.get());

		return BoardColl.get().getChunks(facOpt.get()).stream()
			.map(ps ->
			{
				String world = ps.getWorld();
				int cx = ps.getChunkX(false);
				int cz = ps.getChunkZ(false);
				return new TeamsApiSnapshotTeamClaim(tid, world != null ? world : "", cx, cz, Instant.EPOCH);
			})
			.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getClaimCount(final UUID teamId)
	{
		return TeamsApiFactionFacade.resolveNormal(teamId).map(Faction::getLandCount).orElse(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isClaimed(final String worldName, final int chunkX, final int chunkZ)
	{
		if (worldName == null) return false;

		Faction faction = BoardColl.get().getFactionAt(PS.valueOf(worldName, chunkX, chunkZ));
		return faction != null && TeamsApiFactionFacade.isEligible(faction);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isClaimedBy(final UUID teamId, final String worldName, final int chunkX, final int chunkZ)
	{
		if (teamId == null || worldName == null) return false;

		Faction at = BoardColl.get().getFactionAt(PS.valueOf(worldName, chunkX, chunkZ));
		Optional<Faction> mine = TeamsApiFactionFacade.resolveNormal(teamId);

		return mine.filter(f -> f.equals(at)).isPresent();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Infinite-power factions report {@code -1}; otherwise returns rounded power as the practical cap MassiveCraft uses for claim math.
	 */
	@Override
	public int getTeamMaxClaims(final UUID teamId)
	{
		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return 0;

		final Faction faction = facOpt.get();

		if (faction.getFlag(MFlag.getFlagInfpower())) return -1;

		return faction.getPowerRounded();
	}

}
