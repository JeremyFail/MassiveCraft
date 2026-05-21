package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.collections.MassiveMapDef;
import com.skyblockexp.teamsapi.api.TeamsRelationService;
import com.skyblockexp.teamsapi.event.TeamRelationChangeEvent;
import com.skyblockexp.teamsapi.model.TeamRelation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Maps MassiveCraft directional relation wishes to TeamsAPI {@link TeamRelation} semantics.
 *
 * <p>For distinct factions, {@link #getRelation(UUID, UUID)} reflects {@link Faction#getRelationWish(Faction)} —
 * the relation {@code fromTeamId} declares toward {@code toTeamId}. Clearing to
 * {@link TeamRelation#NEUTRAL} removes the stored wish, matching
 * {@link Faction#setRelationWish(Faction, Rel)} with {@link Rel#NEUTRAL}.
 * When both team UUIDs are equal, returns {@link TeamRelation#MEMBER} (same faction).</p>
 *
 * <p>Declaring {@link TeamRelation#ENEMY} mirrors {@code /f relation set}: the target faction's
 * wish toward the source is also set to {@link Rel#ENEMY}.</p>
 *
 * <p>{@link TeamRelation#MEMBER} is returned only from {@link #getRelation(UUID, UUID)} when both ids match;
 * it is not persisted as a directional wish toward another team ({@link #setRelation} rejects it).</p>
 *
 * <p>{@link #getRelationColor(TeamRelation)} maps {@link MConf} ally/truce/neutral/enemy/member chat colours to
 * {@code #RRGGBB} so consumers match MassiveCraft relation colours instead of {@link TeamRelation} enum defaults.</p>
 */
public final class FactionsTeamsRelationService implements TeamsRelationService
{
	private final Factions factionsPlugin;

	public FactionsTeamsRelationService(final Factions factionsPlugin)
	{
		this.factionsPlugin = factionsPlugin;
	}

	@Override
	public boolean setRelation(final UUID fromTeamId, final UUID toTeamId, final TeamRelation relation,
		final UUID initiatorUUID)
	{
		if (fromTeamId == null || toTeamId == null || relation == null || initiatorUUID == null) return false;
		if (fromTeamId.equals(toTeamId)) return false;

		final Optional<Faction> fromOpt = TeamsApiFactionFacade.resolveNormal(fromTeamId);
		final Optional<Faction> toOpt = TeamsApiFactionFacade.resolveNormal(toTeamId);
		if (fromOpt.isEmpty() || toOpt.isEmpty()) return false;

		final Faction from = fromOpt.get();
		final Faction to = toOpt.get();

		final TeamRelation oldRelation = relToTeamRelation(from.getRelationWish(to));

		final TeamRelationChangeEvent event = new TeamRelationChangeEvent(
			TeamsApiSnapshotTeam.of(from, this.factionsPlugin),
			TeamsApiSnapshotTeam.of(to, this.factionsPlugin),
			initiatorUUID,
			oldRelation,
			relation);

		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return false;

		final TeamRelation applied = event.getNewRelation();
		final Rel relWish = teamRelationToRel(applied);

		from.setRelationWish(to, relWish);

		if (applied == TeamRelation.ENEMY)
		{
			to.setRelationWish(from, Rel.ENEMY);
			to.changed();
		}

		from.changed();
		return true;
	}

	@Override
	public TeamRelation getRelation(final UUID fromTeamId, final UUID toTeamId)
	{
		if (fromTeamId == null || toTeamId == null) return TeamRelation.NEUTRAL;

		final Optional<Faction> fromOpt = TeamsApiFactionFacade.resolveNormal(fromTeamId);
		final Optional<Faction> toOpt = TeamsApiFactionFacade.resolveNormal(toTeamId);
		if (fromOpt.isEmpty() || toOpt.isEmpty()) return TeamRelation.NEUTRAL;

		if (fromTeamId.equals(toTeamId))
		{
			return TeamRelation.MEMBER;
		}

		return relToTeamRelation(fromOpt.get().getRelationWish(toOpt.get()));
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Map getRelations(final UUID teamId)
	{
		if (teamId == null) return Collections.emptyMap();

		final Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return Collections.emptyMap();

		final Map<UUID, TeamRelation> out = new HashMap<>();
		for (final Map.Entry<String, Rel> entry : facOpt.get().getRelationWishes().entrySet())
		{
			final Faction target = FactionColl.get().get(entry.getKey());
			if (!TeamsApiFactionFacade.isEligible(target)) continue;

			final TeamRelation tr = relToTeamRelation(entry.getValue());
			if (tr == TeamRelation.NEUTRAL || tr == TeamRelation.MEMBER) continue;

			out.put(TeamsApiFactionFacade.teamUuid(target), tr);
		}
		return Collections.unmodifiableMap(out);
	}

	@Override
	public boolean clearRelations(final UUID teamId)
	{
		if (teamId == null) return false;

		final String idKey = teamId.toString();
		boolean removedAny = false;

		final Optional<Faction> selfOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (selfOpt.isPresent())
		{
			final Faction self = selfOpt.get();
			if (!self.getRelationWishes().isEmpty())
			{
				self.setRelationWishes(new MassiveMapDef<>());
				removedAny = true;
			}
		}

		for (final Faction other : FactionColl.get().getAll())
		{
			if (other.getRelationWishes().containsKey(idKey))
			{
				other.setRelationWish(idKey, Rel.NEUTRAL);
				removedAny = true;
			}
		}

		return removedAny;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Uses {@link MConf} relation colours when conf is loadable; otherwise the interface default
	 * ({@link TeamRelation#getDefaultHexColor()}).
	 */
	@Override
	public String getRelationColor(final TeamRelation relation)
	{
		try
		{
			final MConf conf = MConf.get();
			final ChatColor cc = switch (relation)
			{
				case MEMBER -> conf.colorMember;
				case ALLY -> conf.colorAlly;
				case TRUCE -> conf.colorTruce;
				case NEUTRAL -> conf.colorNeutral;
				case ENEMY -> conf.colorEnemy;
			};
			final String hex = legacyNamedChatColorToHex(cc);
			return hex != null ? hex : relation.getDefaultHexColor();
		}
		catch (final Throwable ignored)
		{
			return TeamsRelationService.super.getRelationColor(relation);
		}
	}

	/**
	 * Classic dye-style RGB for legacy {@link ChatColor} constants (not formatting codes).
	 *
	 * @return {@code #RRGGBB}, or {@code null} for non-colour codes / unknown
	 */
	private static String legacyNamedChatColorToHex(final ChatColor color)
	{
		if (color == null) return null;

		return switch (color)
		{
			case BLACK -> "#000000";
			case DARK_BLUE -> "#0000AA";
			case DARK_GREEN -> "#00AA00";
			case DARK_AQUA -> "#00AAAA";
			case DARK_RED -> "#AA0000";
			case DARK_PURPLE -> "#AA00AA";
			case GOLD -> "#FFAA00";
			case GRAY -> "#AAAAAA";
			case DARK_GRAY -> "#555555";
			case BLUE -> "#5555FF";
			case GREEN -> "#55FF55";
			case AQUA -> "#55FFFF";
			case RED -> "#FF5555";
			case LIGHT_PURPLE -> "#FF55FF";
			case YELLOW -> "#FFFF55";
			case WHITE -> "#FFFFFF";
			default -> null;
		};
	}

	private static Rel teamRelationToRel(final TeamRelation relation)
	{
		return switch (relation)
		{
			case MEMBER -> Rel.FACTION;
			case ALLY -> Rel.ALLY;
			case TRUCE -> Rel.TRUCE;
			case ENEMY -> Rel.ENEMY;
			case NEUTRAL -> Rel.NEUTRAL;
		};
	}

	private static TeamRelation relToTeamRelation(final Rel rel)
	{
		if (rel == null || rel == Rel.NEUTRAL) return TeamRelation.NEUTRAL;
		if (rel == Rel.FACTION) return TeamRelation.MEMBER;

		return switch (rel)
		{
			case ALLY -> TeamRelation.ALLY;
			case TRUCE -> TeamRelation.TRUCE;
			case ENEMY -> TeamRelation.ENEMY;
			default -> TeamRelation.NEUTRAL;
		};
	}
}
