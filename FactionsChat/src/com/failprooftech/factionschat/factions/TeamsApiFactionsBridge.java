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

	private TeamRelation relationBetweenPlayers(final Player from, final Player toward)
	{
		final Optional<Team> fromTeam = teamOf(from);
		final Optional<Team> towardTeam = teamOf(toward);
		if (fromTeam.isEmpty() || towardTeam.isEmpty())
		{
			return TeamRelation.NEUTRAL;
		}

		if (this.relationService != null)
		{
			return this.relationService.getRelation(fromTeam.get().getId(), towardTeam.get().getId());
		}

		return fromTeam.get().getId().equals(towardTeam.get().getId())
				? TeamRelation.MEMBER
				: TeamRelation.NEUTRAL;
	}

	/**
	 * Converts provider {@code #RRGGBB} from {@link TeamsRelationService#getRelationColor(TeamRelation)} into
	 * FactionsChat's supported RGB legacy form ({@code §#RRGGBB}, same family as {@code &#RRGGBB}).
	 * {@code §x§R§R§G§G§B§B} is not used here — the chat parser expects {@code &#} / {@code §#} hex.
	 */
	private static String hexToChatRgbCode(final String hexFromProvider)
	{
		if (hexFromProvider == null)
		{
			return null;
		}
		String hex = hexFromProvider.trim();
		if (!hex.startsWith("#"))
		{
			return null;
		}
		hex = hex.substring(1);
		if (hex.length() == 3)
		{
			hex = "" + hex.charAt(0) + hex.charAt(0)
					+ hex.charAt(1) + hex.charAt(1)
					+ hex.charAt(2) + hex.charAt(2);
		}
		if (hex.length() != 6 || !hex.matches("[A-Fa-f0-9]{6}"))
		{
			return null;
		}
		return "§#" + hex;
	}

	private String legacyColorForRelation(final TeamRelation relation)
	{
		if (this.relationService != null)
		{
			final String rgb = hexToChatRgbCode(this.relationService.getRelationColor(relation));
			if (rgb != null)
			{
				return rgb;
			}
		}
		return "§" + relation.getLegacyColorCode();
	}

	@Override
	public String getRelationColor(final Player sender, final Player recipient)
	{
		return legacyColorForRelation(relationBetweenPlayers(sender, recipient));
	}

	@Override
	public String getRelationName(final Player sender, final Player recipient)
	{
		return relationBetweenPlayers(sender, recipient).name();
	}

	@Override
	public String getRelationNameLowercase(final Player sender, final Player recipient)
	{
		return getRelationName(sender, recipient).toLowerCase();
	}

	@Override
	public boolean shouldExcludeByFactionRelation(final ChatMode chatMode, final Player sender, final Player recipient)
	{
		final TeamRelation rel = relationBetweenPlayers(sender, recipient);

		return switch (chatMode)
		{
			case FACTION -> rel != TeamRelation.MEMBER;

			case ALLY -> rel != TeamRelation.MEMBER && rel != TeamRelation.ALLY;

			case TRUCE -> rel != TeamRelation.MEMBER && rel != TeamRelation.ALLY && rel != TeamRelation.TRUCE;

			case ENEMY -> rel != TeamRelation.ENEMY;

			default -> false;
		};
	}

	@Override
	public String getDefaultAllyColor()
	{
		return legacyColorForRelation(TeamRelation.ALLY);
	}

	@Override
	public String getDefaultTruceColor()
	{
		return legacyColorForRelation(TeamRelation.TRUCE);
	}

	@Override
	public String getDefaultMemberColor()
	{
		return legacyColorForRelation(TeamRelation.MEMBER);
	}

	@Override
	public String getDefaultEnemyColor()
	{
		return legacyColorForRelation(TeamRelation.ENEMY);
	}

	@Override
	public String getDefaultNeutralColor()
	{
		return legacyColorForRelation(TeamRelation.NEUTRAL);
	}
}
