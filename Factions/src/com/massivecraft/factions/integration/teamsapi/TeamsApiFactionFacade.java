package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPlayerColl;
import com.massivecraft.factions.entity.Rank;
import com.skyblockexp.teamsapi.model.TeamRole;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Stateless helpers shared by TeamsAPI bridging code: faction resolution keyed by TeamsAPI UUIDs, and mapping between
 * MassiveCraft {@link Rank} ladders and coarse TeamsAPI {@link TeamRole} values (OWNER / ADMIN / MEMBER).
 * <p>
 * String-based role codes stay useful where we must avoid touching {@link TeamRole}, while {@link #roleToRank},
 * {@link #rankToRole}, and {@link #memberOf} adapt directly for snapshot DTOs.
 */
final class TeamsApiFactionFacade
{
	/** TeamsAPI {@link TeamRole#name()} for the faction leader rank. */
	static final String TEAMS_ROLE_OWNER = "OWNER";
	/** TeamsAPI {@link TeamRole#name()} for elevated non-leader ranks (officer / second-in-command slot when present). */
	static final String TEAMS_ROLE_ADMIN = "ADMIN";
	/** TeamsAPI {@link TeamRole#name()} for ordinary members. */
	static final String TEAMS_ROLE_MEMBER = "MEMBER";

	/** Declared to block subclassing of this utility holder. */
	private TeamsApiFactionFacade()
	{

	}

	/**
	 * @return {@code true} only for live normal player factions (excludes wilderness, safezone and warzone).
	 */
	static boolean isEligible(final Faction faction)
	{
		return faction != null && faction.isLive() && faction.isNormal();
	}

	/**
	 * Resolves a playable faction backed by persistent storage keyed by TeamsAPI team UUID string.
	 *
	 * @param teamId canonical id previously produced for that faction; {@code null} yields empty
	 */
	static Optional<Faction> resolveNormal(final UUID teamId)
	{
		if (teamId == null) return Optional.empty();

		final Faction faction = FactionColl.get().get(teamId.toString());
		if (!isEligible(faction)) return Optional.empty();

		return Optional.of(faction);
	}

	/**
	 * Canonical UUID presented to TeamsAPI callers. Persisted faction ids are stored as UUID strings.
	 *
	 * @throws NullPointerException if {@code faction} is null.
	 */
	static UUID teamUuid(final Faction faction)
	{
		if (faction == null) throw new NullPointerException("faction");

		try
		{
			return UUID.fromString(faction.getId());
		}
		catch (final IllegalArgumentException ex)
		{
			// Legacy or non-UUID ids: stable synthetic UUID so external APIs still key off a single value.
			return UUID.nameUUIDFromBytes(("faction:" + faction.getId()).getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * @param playerUuid bukkit player id as stored in {@link MPlayerColl}
	 * @return never {@code null} - missing players still materialize a bare {@link MPlayer} shell
	 * @throws NullPointerException if {@code playerUuid} is null
	 */
	static MPlayer resolvePlayer(final UUID playerUuid)
	{
		if (playerUuid == null) throw new NullPointerException("playerUuid");

		return MPlayerColl.get().get(playerUuid.toString());
	}

	/**
	 * Maps a player's rank inside a faction to a TeamsAPI role enum constant name.
	 *
	 * @see #factionRankForTeamsApiRoleCode(Faction, String)
	 */
	static String teamsApiRoleCodeForFactionRank(final Faction faction, final Rank rank)
	{
		if (faction == null || rank == null) return TEAMS_ROLE_MEMBER;

		final Rank leader = faction.getLeaderRank();

		if (rank.equals(leader)) return TEAMS_ROLE_OWNER;

		if (rank.equals(faction.getLowestRank())) return TEAMS_ROLE_MEMBER;

		final Rank belowLeader = leader.getRankBelow();

		if (belowLeader != null && rank.equals(belowLeader))
		{
			return TEAMS_ROLE_ADMIN;
		}

		// Ranks strictly between recruit and leader are treated as admin-equivalent unless they match lowest.
		final boolean aboveLowest = rank.isMoreThan(faction.getLowestRank()) && rank.isLessThan(leader);

		return aboveLowest ? TEAMS_ROLE_ADMIN : TEAMS_ROLE_MEMBER;
	}

	/**
	 * Resolves MassiveCraft {@link Rank} from a TeamsAPI role constant name (<code>OWNER</code>/<code>ADMIN</code>/<code>MEMBER</code>).
	 *
	 * @param roleCodeName {@link TeamRole#name()} or equivalent; unknown strings fall back to lowest rank
	 */
	static Rank factionRankForTeamsApiRoleCode(final Faction faction, final String roleCodeName)
	{
		if (faction == null) return null;
		if (roleCodeName == null) return faction.getLowestRank();

		if (TEAMS_ROLE_OWNER.equals(roleCodeName))
		{
			return faction.getLeaderRank();
		}

		if (TEAMS_ROLE_ADMIN.equals(roleCodeName))
		{
			final Rank rb = faction.getLeaderRank().getRankBelow();

			return rb != null ? rb : faction.getLowestRank();
		}

		return faction.getLowestRank();
	}

	/**
	 * Convenience wrapper translating {@link TeamRole} enumeration values through {@link #factionRankForTeamsApiRoleCode(Faction, String)}.
	 */
	static Rank roleToRank(final Faction faction, final TeamRole role)
	{
		if (faction == null) return null;
		if (role == null) return faction.getLowestRank();

		return factionRankForTeamsApiRoleCode(faction, role.name());
	}

	/**
	 * Builds an immutable TeamsAPI-facing member snapshot; join timestamps are not tracked in faction data ({@link Instant#EPOCH}).
	 */
	static TeamsApiSnapshotTeamMember memberOf(final Faction faction, final MPlayer mp)
	{
		return new TeamsApiSnapshotTeamMember(UUID.fromString(mp.getId()), rankToRole(faction, mp.getRank()), Instant.EPOCH);
	}

	/**
	 * Inverse of {@link #roleToRank(Faction, TeamRole)} for read paths.
	 */
	static TeamRole rankToRole(final Faction faction, final Rank rank)
	{
		return TeamRole.valueOf(teamsApiRoleCodeForFactionRank(faction, rank));
	}
}
