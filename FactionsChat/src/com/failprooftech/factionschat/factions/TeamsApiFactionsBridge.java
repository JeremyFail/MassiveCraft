package com.failprooftech.factionschat.factions;

import com.failprooftech.factionschat.ChatMode;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsRelationService;
import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamRelation;
import com.skyblockexp.teamsapi.model.TeamRole;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * {@link FactionsBridge} backed by the Teams API ({@link TeamsService} + optional {@link TeamsRelationService}).
 *
 * <p>FactionsChat prefers this bridge whenever a teams plugin has registered {@link TeamsAPI} services,
 * regardless of whether that plugin ships Teams API as an optional dependency (for example PvPIndex Factions
 * may run without it). If no provider is registered, callers fall back to direct MassiveCraft / PvPIndex bridges.</p>
 */
public final class TeamsApiFactionsBridge implements FactionsBridge
{
	private final TeamsService teamsService;
	private final TeamsRelationService relationService;

	/**
	 * Creates a new TeamsApiFactionsBridge instance.
	 * 
	 * @param teamsService The Teams service instance.
	 * @param relationService The Teams relation service instance.
	 */
	private TeamsApiFactionsBridge(final TeamsService teamsService, final TeamsRelationService relationService)
	{
		this.teamsService = teamsService;
		this.relationService = relationService;
	}

	/**
	 * Tries to create a new TeamsApiFactionsBridge instance.
	 * 
	 * @return The TeamsApiFactionsBridge instance, or empty if the Teams API is not available.
	 */
	public static Optional<TeamsApiFactionsBridge> tryCreate()
	{
		if (!TeamsAPI.isAvailable()) return Optional.empty();

		final TeamsService svc = TeamsAPI.getService();
		if (svc == null) return Optional.empty();

		final TeamsRelationService rel = TeamsAPI.isRelationAvailable() ? TeamsAPI.getRelationService() : null;

		return Optional.of(new TeamsApiFactionsBridge(svc, rel));
	}

	/**
	 * Gets the team of a player.
	 * 
	 * @param player The player to get the team of.
	 * @return The team of the player, or empty if the player is not in a team.
	 */
	private Optional<Team> teamOf(final Player player)
	{
		return svcGetPlayerTeam(player.getUniqueId());
	}
	
	/**
	 * Gets the team of a player from the Teams service.
	 * 
	 * @param playerUuid The UUID of the player to get the team of.
	 * @return The team of the player, or empty if the player is not in a team.
	 */
	private Optional<Team> svcGetPlayerTeam(final UUID playerUuid)
	{
		return this.teamsService.getPlayerTeam(playerUuid);
	}
	
	/**
	 * Gets the role of a player.
	 * 
	 * @param playerUuid The UUID of the player to get the role of.
	 * @return The role of the player, or empty if the player is not in a team.
	 */
	private Optional<TeamRole> roleOf(final UUID playerUuid)
	{
		return svcGetPlayerTeam(playerUuid).flatMap(team ->
			this.teamsService.getMemberRole(team.getId(), playerUuid));
	}

	// --------------------------------------------------------------------- //
	// Membership / names
	// --------------------------------------------------------------------- //

	@Override
	public boolean isInFaction(final Player player)
	{
		return this.teamsService.hasTeam(player.getUniqueId());
	}

	@Override
	public String getFactionName(final Player player)
	{
		return teamOf(player).map(Team::getName).orElse("");
	}

	@Override
	public String getFactionNameForce(final Player player)
	{
		return teamOf(player).map(Team::getName).orElse("Wilderness");
	}

	// --------------------------------------------------------------------- //
	// Rank / title
	// --------------------------------------------------------------------- //

	@Override
	public String getPlayerRank(final Player player)
	{
		if (!isInFaction(player)) return "";
		return roleOf(player.getUniqueId()).map(Enum::name).orElse("");
	}

	@Override
	public String getPlayerRankPrefix(final Player player)
	{
		return "";
	}

	@Override
	public String getPlayerRankForce(final Player player)
	{
		return roleOf(player.getUniqueId()).map(Enum::name).orElse(TeamRole.MEMBER.name());
	}

	@Override
	public String getPlayerRankPrefixForce(final Player player)
	{
		return "";
	}

	@Override
	public String getPlayerTitle(final Player player)
	{
		return "";
	}

	// --------------------------------------------------------------------- //
	// Relations
	// --------------------------------------------------------------------- //

	private boolean sameTeam(final Player a, final Player b)
	{
		final Optional<Team> ta = teamOf(a);
		final Optional<Team> tb = teamOf(b);
		return ta.isPresent() && tb.isPresent() && ta.get().getId().equals(tb.get().getId());
	}

	private TeamRelation declaredRelation(final Player from, final Player toward)
	{
		if (this.relationService == null) return TeamRelation.NEUTRAL;

		final Optional<Team> fromTeam = teamOf(from);
		final Optional<Team> towardTeam = teamOf(toward);
		if (fromTeam.isEmpty() || towardTeam.isEmpty()) return TeamRelation.NEUTRAL;

		return this.relationService.getRelation(fromTeam.get().getId(), towardTeam.get().getId());
	}

	@Override
	public String getRelationColor(final Player sender, final Player recipient)
	{
		if (sameTeam(sender, recipient)) return getDefaultMemberColor();

		final TeamRelation rel = declaredRelation(sender, recipient);
		return "§" + rel.getLegacyColorCode();
	}

	@Override
	public String getRelationName(final Player sender, final Player recipient)
	{
		if (sameTeam(sender, recipient)) return "MEMBER";

		return declaredRelation(sender, recipient).name();
	}

	@Override
	public String getRelationNameLowercase(final Player sender, final Player recipient)
	{
		return getRelationName(sender, recipient).toLowerCase();
	}

	@Override
	public boolean shouldExcludeByFactionRelation(final ChatMode chatMode, final Player sender, final Player recipient)
	{
		if (sameTeam(sender, recipient))
		{
			return switch (chatMode)
			{
				case ENEMY -> true;
				default -> false;
			};
		}

		final TeamRelation rel = declaredRelation(sender, recipient);

		return switch (chatMode)
		{
			case FACTION -> true;

			case ALLY -> rel != TeamRelation.ALLY;

			case TRUCE -> rel != TeamRelation.ALLY && rel != TeamRelation.TRUCE;

			case ENEMY -> rel != TeamRelation.ENEMY;

			default -> false;
		};
	}

	@Override
	public String getDefaultAllyColor()
	{
		return "§" + TeamRelation.ALLY.getLegacyColorCode();
	}

	@Override
	public String getDefaultTruceColor()
	{
		return "§" + TeamRelation.TRUCE.getLegacyColorCode();
	}

	@Override
	public String getDefaultMemberColor()
	{
		return "§a";
	}

	@Override
	public String getDefaultEnemyColor()
	{
		return "§" + TeamRelation.ENEMY.getLegacyColorCode();
	}

	@Override
	public String getDefaultNeutralColor()
	{
		return "§" + TeamRelation.NEUTRAL.getLegacyColorCode();
	}
}
