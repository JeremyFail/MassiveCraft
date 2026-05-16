package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsCreate;
import com.massivecraft.factions.event.EventFactionsDisband;
import com.massivecraft.factions.event.EventFactionsMembershipChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange.MembershipChangeReason;
import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.model.TeamRole;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.store.MStore;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TeamsAPI {@link TeamsService} backed by persisted {@link Faction} entities: team lifecycle delegates to MassiveCraft
 * events ({@link EventFactionsCreate}, {@link EventFactionsDisband}, {@link EventFactionsMembershipChange}) mirroring gameplay rules.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class FactionsTeamsService implements TeamsService
{
	private final Factions factionsPlugin;

	/** @param factionsPlugin owner used for snapshots and fallback console attribution */
	public FactionsTeamsService(final Factions factionsPlugin)
	{
		this.factionsPlugin = factionsPlugin;
	}

	/** @return canonical console {@link CommandSender} for integrations without Player actors */
	private CommandSender cmdConsole()
	{
		return this.factionsPlugin.getServer().getConsoleSender();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Rejects blank names, non-UUID owners, collisions with existing factions, or players already occupying a faction.
	 */
	@Override
	public Optional createTeam(final String name, final UUID ownerUUID)
	{
		if (name == null || ownerUUID == null || name.isBlank()) return Optional.empty();

		MPlayer owner = TeamsApiFactionFacade.resolvePlayer(ownerUUID);
		if (owner.hasFaction()) return Optional.empty();

		if (FactionColl.get().getByName(name) != null) return Optional.empty();

		String factionId = MStore.createId();

		CommandSender console = cmdConsole();

		EventFactionsCreate createEvent = new EventFactionsCreate(console, factionId, name);
		createEvent.run();
		if (createEvent.isCancelled()) return Optional.empty();

		Faction faction = FactionColl.get().create(factionId);
		faction.setName(name.strip());

		owner.resetFactionData();
		owner.setFaction(faction);
		owner.setRank(faction.getLeaderRank());

		EventFactionsMembershipChange joinEvent = new EventFactionsMembershipChange(console, owner, faction,
			MembershipChangeReason.CREATE);
		joinEvent.run();

		return Optional.of(TeamsApiSnapshotTeam.of(faction, this.factionsPlugin));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Permanent factions cannot dissolve through this bridge; emits disband-oriented membership churn per member first.
	 */
	@Override
	public boolean deleteTeam(final UUID teamId)
	{
		Optional<Faction> opt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (opt.isEmpty()) return false;
		Faction faction = opt.get();

		if (faction.getFlag(MFlag.getFlagPermanent())) return false;

		CommandSender console = cmdConsole();

		EventFactionsDisband disband = new EventFactionsDisband(console, faction);
		disband.run();
		if (disband.isCancelled()) return false;

		for (MPlayer mplayer : new ArrayList<>(faction.getMPlayers()))
		{
			EventFactionsMembershipChange event = new EventFactionsMembershipChange(console, mplayer, FactionColl.get().getNone(),
				MembershipChangeReason.DISBAND);
			event.run();
		}

		faction.detach();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional getTeam(final UUID teamId)
	{
		return TeamsApiFactionFacade.resolveNormal(teamId).map(f -> TeamsApiSnapshotTeam.of(f, this.factionsPlugin));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional getTeamByName(final String name)
	{
		if (name == null) return Optional.empty();
		Faction faction = FactionColl.get().getByName(name);
		if (!TeamsApiFactionFacade.isEligible(faction)) return Optional.empty();
		return Optional.of(TeamsApiSnapshotTeam.of(faction, this.factionsPlugin));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional getPlayerTeam(final UUID playerUUID)
	{
		if (playerUUID == null) return Optional.empty();
		MPlayer mp = TeamsApiFactionFacade.resolvePlayer(playerUUID);
		Faction faction = mp.getFaction();
		if (!TeamsApiFactionFacade.isEligible(faction)) return Optional.empty();
		return Optional.of(TeamsApiSnapshotTeam.of(faction, this.factionsPlugin));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection getAllTeams()
	{
		return FactionColl.get().getAll().stream()
			.filter(TeamsApiFactionFacade::isEligible)
			.map(f -> TeamsApiSnapshotTeam.of(f, this.factionsPlugin))
			.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getTeamCount()
	{
		return (int) FactionColl.get().getAll().stream().filter(TeamsApiFactionFacade::isEligible).count();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Direct OWNER promotion is blocked here - use {@link #setMemberRole(UUID, UUID, TeamRole)}. Join obeys member caps and
	 * negative-power policy equivalent to slash commands.
	 */
	@Override
	public boolean addMember(final UUID teamId, final UUID playerUUID, final TeamRole role)
	{
		if (teamId == null || playerUUID == null || role == null || role == TeamRole.OWNER) return false;

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;
		Faction faction = facOpt.get();

		MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);
		Faction theirs = mplayer.getFaction();

		if (TeamsApiFactionFacade.isEligible(theirs))
		{
			if (theirs.equals(faction)) return false;
			return false;
		}

		int lim = MConf.get().factionMemberLimit;
		if (lim > 0 && faction.getMPlayers().size() >= lim) return false;

		if (!MConf.get().canLeaveWithNegativePower && mplayer.getPower() < 0)
		{
			return false;
		}

		CommandSender console = cmdConsole();
		EventFactionsMembershipChange event = new EventFactionsMembershipChange(console, mplayer, faction, MembershipChangeReason.JOIN);
		event.run();
		if (event.isCancelled()) return false;

		mplayer.resetFactionData();
		mplayer.setFaction(faction);
		mplayer.setRank(TeamsApiFactionFacade.roleToRank(faction, role));

		faction.uninvite(mplayer);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Leaders resist removal via this API mirroring faction command protections; empties factions may auto-disband following kick rules.
	 */
	@Override
	public boolean removeMember(final UUID teamId, final UUID playerUUID)
	{
		if (teamId == null || playerUUID == null) return false;
		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;

		Faction faction = facOpt.get();
		MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);

		if (!mplayer.getFaction().equals(faction)) return false;
		if (mplayer.getRank().isLeader()) return false;

		boolean permanent = faction.getFlag(MFlag.getFlagPermanent());

		if (!MConf.get().canLeaveWithNegativePower && mplayer.getPower() < 0) return false;

		CommandSender console = cmdConsole();
		EventFactionsMembershipChange event = new EventFactionsMembershipChange(console, mplayer, FactionColl.get().getNone(),
			MembershipChangeReason.KICK);
		event.run();
		if (event.isCancelled()) return false;

		mplayer.resetFactionData();

		if (faction.isNormal() && !permanent && faction.getMPlayers().isEmpty())
		{
			EventFactionsDisband disband = new EventFactionsDisband(console, faction);
			disband.run();
			if (!disband.isCancelled())
			{
				faction.detach();
			}
		}
		return true;
	}

	/**
	 * Demotes incumbent leader gracefully when assigning any non-owner role, then applies target rank mappings.
	 *
	 * @return {@code false} unless both players remain in the faction after rotations
	 */
	private static boolean transferOwnership(final Faction faction, final MPlayer newLeader)
	{
		if (faction == null || newLeader == null) return false;
		if (!newLeader.getFaction().equals(faction)) return false;

		if (newLeader.getRank().isLeader())
		{
			return true;
		}

		MPlayer old = faction.getLeader();
		Rank leaderRank = faction.getLeaderRank();
		if (old != null && old != newLeader && old.getFaction().equals(faction))
		{
			Rank below = leaderRank.getRankBelow();
			if (below != null) old.setRank(below);
			else old.setRank(faction.getLowestRank());
		}

		newLeader.setRank(leaderRank);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Promoting OWNER uses {@link #transferOwnership}; demoting today's leader triggers {@link Faction#promoteNewLeader()} when other
	 * leaders would be absent afterward to stay consistent with MassiveCraft invariants.
	 */
	@Override
	public boolean setMemberRole(final UUID teamId, final UUID playerUUID, final TeamRole newRole)
	{
		if (teamId == null || playerUUID == null || newRole == null) return false;

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;
		Faction faction = facOpt.get();

		MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);
		if (!mplayer.getFaction().equals(faction)) return false;

		if (newRole == TeamRole.OWNER)
		{
			return transferOwnership(faction, mplayer);
		}

		if (mplayer.getRank().isLeader() && newRole != TeamRole.OWNER)
		{
			if (faction.getMPlayersWhereRank(faction.getLeaderRank()).size() <= 1) return false;
			faction.promoteNewLeader();
			if (faction.detached())
			{
				return false;
			}

			Optional<Faction> refreshed = TeamsApiFactionFacade.resolveNormal(teamId);
			if (refreshed.isEmpty()) return false;
			faction = refreshed.get();

			MPlayer refreshedPlayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);
			if (!refreshedPlayer.getFaction().equals(faction))
			{
				return false;
			}

			Rank desired = TeamsApiFactionFacade.roleToRank(faction, newRole);
			refreshedPlayer.setRank(desired);
			return true;
		}

		mplayer.setRank(TeamsApiFactionFacade.roleToRank(faction, newRole));
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional getMemberRole(final UUID teamId, final UUID playerUUID)
	{
		if (teamId == null || playerUUID == null) return Optional.empty();

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return Optional.empty();
		Faction faction = facOpt.get();

		MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);
		if (!mplayer.getFaction().equals(faction)) return Optional.empty();

		return Optional.of(TeamsApiFactionFacade.rankToRole(faction, mplayer.getRank()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional getMemberInfo(final UUID teamId, final UUID playerUUID)
	{
		if (teamId == null || playerUUID == null) return Optional.empty();

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return Optional.empty();
		Faction faction = facOpt.get();

		MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);
		if (!mplayer.getFaction().equals(faction)) return Optional.empty();

		return Optional.of(TeamsApiFactionFacade.memberOf(faction, mplayer));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasTeam(final UUID playerUUID)
	{
		if (playerUUID == null) return false;
		return TeamsApiFactionFacade.resolvePlayer(playerUUID).hasFaction();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean teamExists(final String name)
	{
		if (name == null) return false;
		Faction f = FactionColl.get().getByName(name);
		return TeamsApiFactionFacade.isEligible(f);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMember(final UUID teamId, final UUID playerUUID)
	{
		if (teamId == null || playerUUID == null) return false;
		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;
		return TeamsApiFactionFacade.resolvePlayer(playerUUID).getFaction().equals(facOpt.get());
	}
}
