package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPlayer;
import com.massivecraft.factions.cmd.type.TypeRank;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPlayerColl;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.factions.event.EventFactionsMembershipChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange.MembershipChangeReason;
import com.massivecraft.factions.event.EventFactionsRankChange;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.util.Txt;

import java.util.HashSet;
import java.util.Set;

public class CmdFactionsRankSet extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	// These fields are set upon perform() and unset afterwards.
	
	// Target
	private Faction targetFaction = null;
	private MPlayer target = null;
	
	// End faction (the faction they are changed to)
	private Faction endFaction = null;
	private boolean factionChange = false;
	
	// Ranks
	private Rank senderRank = null;
	private Rank targetRank = null;
	private Rank rank = null;
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsRankSet()
	{
		// Parameters
		this.addParameter(TypeMPlayer.get(), "player");
		this.addParameter(TypeRank.get(), "rank");
		this.addParameter(TypeFaction.get(), "faction", "their");

		// Too complicated for that
		this.setSwapping(false);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// This sets target and much other.
		this.registerFields();

		// Is the player allowed or not. Method can be found later down.
		this.ensureAllowed();
		
		if (factionChange)
		{	
			this.changeFaction();
		}
		
		// Does the change make sense.
		this.ensureMakesSense();
		
		// Event
		EventFactionsRankChange event = new EventFactionsRankChange(sender, target, rank);
		event.run();
		if (event.isCancelled()) return;
		rank = event.getNewRank();
		
		// Change the rank.
		this.changeRank();
	}

	// This is always run after performing a MassiveCommand.
	@Override
	public void senderFields(boolean set)
	{
		super.senderFields(set);
		
		if ( ! set)
		{
			this.unregisterFields();			
		}
	}

	// -------------------------------------------- //
	// PRIVATE: REGISTER & UNREGISTER
	// -------------------------------------------- //

	private void registerFields() throws MassiveException
	{
		// Getting the target and faction.
		target = this.readArg();
		targetFaction = target.getFaction();
		
		
		// Ranks
		senderRank = msender.getRank();
		targetRank = target.getRank();

		endFaction = this.readArgAt(2, targetFaction);
		factionChange = (endFaction != targetFaction);
		
		// Rank if any passed.
		TypeRank typeRank = new TypeRank(endFaction, target.getRank());
		rank = typeRank.read(this.argAt(1), sender);

	}
	
	private void unregisterFields()
	{
		targetFaction = null;
		target = null;

		endFaction = null;
		factionChange = false;
		
		senderRank = null;
		targetRank = null;
		rank = null;
	}

	// -------------------------------------------- //
	// PRIVATE: ENSURE
	// -------------------------------------------- //
	
	private void ensureAllowed() throws MassiveException
	{
		// People with permission don't follow the normal rules.
		if (msender.isOverriding()) return;
		
		// If somone gets the leadership of wilderness (Which has happened before).
		// We can at least try to limit their powers.
		if (endFaction.isNone())
		{
			throw new MassiveException().addMsg("%s <b>doesn't use ranks sorry :(", endFaction.getName());
		}
		
		if (target == msender)
		{
			// Don't change your own rank.
			throw new MassiveException().addMsg("<b>The target player mustn't be yourself.");
		}

		if (factionChange)
		{
			// Don't change peoples faction
			throw new MassiveException().addMsg("<b>You can't change %s's <b>faction.", target.describeTo(msender));
		}

		if (!MPerm.getPermRank().has(msender, targetFaction, false))
		{
			throw new MassiveException().addMessage(MPerm.getPermRank().createDeniedMessage(msender, targetFaction));
		}
		
		// The following two if statements could be merged. 
		// But isn't for the sake of nicer error messages.
		if (senderRank == targetRank)
		{
			// You can't change someones rank if it is equal to yours.
			throw new MassiveException().addMsg("<h>%s <b>can't manage eachother.", senderRank.getName()+"s");
		}
		
		if (senderRank.isLessThan(targetRank))
		{
			// You can't change someones rank if it is higher than yours.
			throw new MassiveException().addMsg("<b>You can't manage people of higher rank.");
		}
		
		// The following two if statements could be merged. 
		// But isn't for the sake of nicer error messages.
		if (senderRank == rank && !senderRank.isLeader())
		{
			// You can't set ranks equal to your own. Unless you are the leader.
			throw new MassiveException().addMsg("<b>You can't set ranks equal to your own.");
		}
		
		if (senderRank.isLessThan(rank))
		{
			// You can't set ranks higher than your own.
			throw new MassiveException().addMsg("<b>You can't set ranks higher than your own.");
		}
	}
	
	private void ensureMakesSense() throws MassiveException
	{
		// Don't change their rank to something they already are.
		if (target.getRank() == rank)
		{
			throw new MassiveException().addMsg("%s <b>is already %s %s.", target.describeTo(msender), Txt.aan(rank.getName()), rank.getName());
		}
	}

	// -------------------------------------------- //
	// PRIVATE: CHANGE FACTION
	// -------------------------------------------- //
	
	private void changeFaction() throws MassiveException
	{	
		// Don't change a leader to a new faction.
		if (targetRank.isLeader())
		{
			throw new MassiveException().addMsg("<b>You cannot remove the present leader. Demote them first.");
		}
		
		// Event
		EventFactionsMembershipChange membershipChangeEvent = new EventFactionsMembershipChange(sender, msender, endFaction, MembershipChangeReason.RANK);
		membershipChangeEvent.run();
		if (membershipChangeEvent.isCancelled()) throw new MassiveException();
		
		// Apply
		target.resetFactionData();
		target.setFaction(endFaction);
		
		// No longer invited.
		endFaction.uninvite(target);

		// Create recipients
		Set<MPlayer> recipients = new HashSet<>();
		recipients.addAll(targetFaction.getMPlayersWhereOnline(true));
		recipients.addAll(endFaction.getMPlayersWhereOnline(true));
		recipients.add(msender);
		
		// Send message
		for (MPlayer recipient : recipients)
		{
			recipient.msg("%s <i>was moved from <i>%s to <i>%s<i>.", target.describeTo(recipient), targetFaction.describeTo(recipient), endFaction.describeTo(recipient));
		}
		
		// Derplog
		if (MConf.get().logFactionJoin)
		{
			Factions.get().log(Txt.parse("%s moved %s from %s to %s.", msender.getName(), target.getName(), targetFaction.getName(), endFaction.getName()));
		}
		
		// Now we don't need the old values.
		targetFaction = target.getFaction();
		targetRank = target.getRank();
		senderRank = msender.getRank(); // In case they changed their own rank

	}
	
	// -------------------------------------------- //
	// PRIVATE: CHANGE RANK
	// -------------------------------------------- //
	
	private void changeRank() throws MassiveException
	{
		// In case of leadership change, we do special things not done in other rank changes.
		if (rank.isLeader())
		{
			this.changeRankLeader();
		}
		else
		{
			this.changeRankOther();
		}
	}

	private void changeRankLeader()
	{
		// If there is a current leader. Demote & inform them.
		MPlayer targetFactionCurrentLeader = targetFaction.getLeader();
		if (targetFactionCurrentLeader != null)
		{
			// Inform & demote the old leader.
			targetFactionCurrentLeader.setRank(rank.getRankBelow());
			if (targetFactionCurrentLeader != msender)
			{
				// They kinda know if they fired the command themself.
				targetFactionCurrentLeader.msg("<i>You have been demoted from the position of faction leader by %s<i>.", msender.describeTo(targetFactionCurrentLeader, true));
			}
		}
		
		// Promote the new leader.
		target.setRank(rank);

		// Inform everyone, this includes sender and target.
		for (MPlayer recipient : MPlayerColl.get().getAllOnline())
		{
			String changerName = senderIsConsole ? "A server admin" : msender.describeTo(recipient);
			recipient.msg("%s<i> gave %s<i> the leadership of %s<i>.", changerName, target.describeTo(recipient), targetFaction.describeTo(recipient));
		}
	}
	
	private void changeRankOther() throws MassiveException
	{
		// If the target is currently the leader and faction isn't permanent a new leader should be promoted.
		// Sometimes a bug occurs and multiple leaders exist. Then we should be able to demote without promoting new leader
		if (targetRank.isLeader() && ( ! MConf.get().permanentFactionsDisableLeaderPromotion || ! targetFaction.getFlag(MFlag.ID_PERMANENT)) && targetFaction.getMPlayersWhereRank(targetFaction.getLeaderRank()).size() == 1)
			// This if statement is very long. Should I nest it for readability?
		{
			targetFaction.promoteNewLeader(); // This might disband the faction.
			
			// So if the faction disbanded...
			if (targetFaction.detached())
			{
				// ... we inform the sender.
				target.resetFactionData();
				throw new MassiveException().addMsg("<i>The target was a leader and got demoted. The faction disbanded and no rank was set.");
			}
		}

		// Create recipients
		Set<MPlayer> recipients = new MassiveSet<>(targetFaction.getMPlayers());
		recipients.add(msender);
		
		// Were they demoted or promoted?
		String change = (rank.isLessThan(targetRank) ? "demoted" : "promoted");
		
		// The rank will be set before the msg, so they have the appropriate prefix.
		target.setRank(rank);
		String oldRankName = targetRank.getName().toLowerCase();
		String rankName = rank.getName().toLowerCase();

		// Send message
		for(MPlayer recipient : recipients)
		{
			String targetName = target.describeTo(recipient, true);
			String wasWere = (recipient == target) ? "were" : "was";
			recipient.msg("%s<i> %s %s from %s to <h>%s <i>in %s<i>.", targetName, wasWere, change, oldRankName, rankName, targetFaction.describeTo(msender));
		}
	}
	
}
