package com.massivecraft.factions.integration.teamsapi;


import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.Invitation;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsInvitedChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange.MembershipChangeReason;
import com.skyblockexp.teamsapi.api.TeamsInviteService;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.UUID;

/**
 * Bridges TeamsAPI invitation flows onto MassiveCraft {@link Invitation} storage and faction join semantics.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class FactionsTeamsInviteService implements TeamsInviteService
{
	private final Factions factionsPlugin;

	/**
	 * @param factionsPlugin owning plugin - used only for snapshot display context ({@link TeamsApiSnapshotTeam#of}) and fallback console sender access
	 */
	public FactionsTeamsInviteService(final Factions factionsPlugin)
	{
		this.factionsPlugin = factionsPlugin;
	}

	/** @return server's console actor for integrations lacking an online sender */
	private CommandSender cmdConsole()
	{
		return this.factionsPlugin.getServer().getConsoleSender();
	}

	/**
	 * @param playerUuid player invoking the mutation; resolves to {@link MPlayer#getSender()} when logged in else console fallback
	 */
	private CommandSender actorFor(final UUID playerUuid)
	{
		if (playerUuid == null) return cmdConsole();
		MPlayer mp = TeamsApiFactionFacade.resolvePlayer(playerUuid);
		CommandSender sender = mp.getSender();
		return sender != null ? sender : cmdConsole();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Rejected when invitee shares the faction already, occupies another faction, duplicate invite exists, or
	 * {@link EventFactionsInvitedChange} cancels.
	 */
	@Override
	public boolean invitePlayer(final UUID teamId, final UUID inviterUUID, final UUID inviteeUUID)
	{
		if (teamId == null || inviterUUID == null || inviteeUUID == null) return false;

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;
		Faction faction = facOpt.get();

		MPlayer invitee = TeamsApiFactionFacade.resolvePlayer(inviteeUUID);
		if (invitee.getFaction().equals(faction)) return false;
		if (TeamsApiFactionFacade.isEligible(invitee.getFaction())) return false;

		if (faction.isInvited(invitee)) return false;

		CommandSender actor = actorFor(inviterUUID);

		EventFactionsInvitedChange event = new EventFactionsInvitedChange(actor, invitee, faction, false);
		event.run();
		if (event.isCancelled()) return false;

		MPlayer inviter = TeamsApiFactionFacade.resolvePlayer(inviterUUID);
		Invitation invitation = new Invitation(inviter.getId(), System.currentTimeMillis());
		faction.invite(invitee.getId(), invitation);
		faction.changed();
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Join behaves like {@code /f join}: lowest rank placement, honoring negative-power config and cancellable membership event.
	 */
	@Override
	public Optional acceptInvite(final UUID teamId, final UUID playerUUID)
	{
		if (teamId == null || playerUUID == null) return Optional.empty();

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return Optional.empty();
		Faction faction = facOpt.get();

		MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);
		if (!faction.isInvited(mplayer)) return Optional.empty();

		if (!MConf.get().canLeaveWithNegativePower && mplayer.getPower() < 0)
		{
			return Optional.empty();
		}

		CommandSender actor = actorFor(playerUUID);

		EventFactionsMembershipChange joinEvent = new EventFactionsMembershipChange(actor, mplayer, faction, MembershipChangeReason.JOIN);
		joinEvent.run();
		if (joinEvent.isCancelled()) return Optional.empty();

		mplayer.resetFactionData();
		mplayer.setFaction(faction);
		mplayer.setRank(faction.getLowestRank());
		faction.uninvite(mplayer);

		return Optional.of(TeamsApiSnapshotTeam.of(faction, this.factionsPlugin));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean declineInvite(final UUID teamId, final UUID playerUUID)
	{
		if (teamId == null || playerUUID == null) return false;

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;
		Faction faction = facOpt.get();

		MPlayer mplayer = TeamsApiFactionFacade.resolvePlayer(playerUUID);
		if (!faction.isInvited(mplayer)) return false;

		boolean ok = faction.uninvite(mplayer);
		if (ok) faction.changed();
		return ok;
	}

}
