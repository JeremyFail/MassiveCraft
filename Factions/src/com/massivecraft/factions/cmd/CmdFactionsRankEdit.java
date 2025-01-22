package com.massivecraft.factions.cmd;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;

public class CmdFactionsRankEdit extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	public CmdFactionsRankEditCreate cmdFactionsRankEditCreate = new CmdFactionsRankEditCreate();
	public CmdFactionsRankEditDelete cmdFactionsRankEditDelete = new CmdFactionsRankEditDelete();
	public CmdFactionsRankEditName cmdFactionsRankEditName = new CmdFactionsRankEditName();
	public CmdFactionsRankEditPrefix cmdFactionsRankEditPrefix = new CmdFactionsRankEditPrefix();
	public CmdFactionsRankEditPriority cmdFactionsRankEditPriority = new CmdFactionsRankEditPriority();

	static void ensureAllowed(MPlayer msender, Faction faction, String action) throws MassiveException
	{
		if (msender.isOverriding()) return;

		if (faction != msender.getFaction())
		{
			throw new MassiveException().addMsg("<b>You can't manage ranks outside your own faction.");
		}

		if (!msender.getRank().isLeader())
		{
			throw new MassiveException().addMsg("<b>Only the leader can %s ranks.", action);
		}
	}

}
