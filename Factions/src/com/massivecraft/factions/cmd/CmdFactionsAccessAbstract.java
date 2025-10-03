package com.massivecraft.factions.cmd;

import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.factions.util.AsciiMap;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.Txt;

import java.util.Collection;


public abstract class CmdFactionsAccessAbstract extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public PS chunk;
	public TerritoryAccess ta;
	public Faction hostFaction;
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsAccessAbstract()
	{
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void senderFields(boolean set)
	{
		super.senderFields(set);

		if (set)
		{
			chunk = PS.valueOf(me.getLocation()).getChunk(true);
			ta = BoardColl.get().getTerritoryAccessAt(chunk);
			hostFaction = ta.getHostFaction();
		}
		else
		{
			chunk = null;
			ta = null;
			hostFaction = null;
		}
	}

	public void sendAccessInfo()
	{
		String chunkDesc = AsciiMap.getChunkDescWithName(chunk, ta);
		Object title = "Access" + chunkDesc;
		title = Txt.titleize(title);
		message(title);
		
		msg("<k>Host Faction: %s", hostFaction.describeTo(msender, true));
		msg("<k>Host Faction Allowed: %s", ta.isHostFactionAllowed() ? Txt.parse("<lime>TRUE") : Txt.parse("<rose>FALSE"));
		msg("<k>Granted to: %s", CmdFactionsPermShow.permablesToDisplayString(ta.getGranteds(), msender));
	}

	public void setAccess(Collection<PS> chunks, MPermable mpermable, boolean granted)
	{
		chunks.forEach(chunk -> setAccess(chunk, mpermable, granted));
	}

	public void setAccess(PS chunk, MPermable mpermable, boolean granted)
	{
		TerritoryAccess ta = BoardColl.get().getTerritoryAccessAt(chunk);
		Faction faction = ta.getHostFaction();


		String chunkDesc = AsciiMap.getChunkDescWithName(chunk, ta);
		String grantedDenied = granted ? "granted" : "denied";
		String mpermableDesc = mpermable.getDisplayName(msender);

		if ( ! MPerm.getPermAccess().has(msender, faction, false))
		{
			msg("<b>You do not have permission to edit access%s<b>.", chunkDesc);
			return;
		}

		if (ta.isGranted(mpermable) == granted)
		{
			msg("<b>Access%s <b>is already %s to %s<b>.", chunkDesc, grantedDenied, mpermableDesc);
			return;
		}

		ta = ta.withGranted(mpermable, granted);
		BoardColl.get().setTerritoryAccessAt(chunk, ta);

		msg("<i>Access%s<i> is now %s to %s<i>.", chunkDesc, grantedDenied, mpermableDesc);
	}

}
