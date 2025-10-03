package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqTaxEnabled;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;

import java.util.Map.Entry;

public class CmdFactionsTaxFaction extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsTaxFaction()
	{
		this.addParameter(TypeFaction.get(), "faction", "your");

		this.addRequirements(ReqTaxEnabled.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		Faction faction = this.readArg(msenderFaction);

		if (faction.isNone())
		{
			throw new MassiveException().addMsg("<b>Taxes are not in place for %s<b>.", faction.describeTo(msender));
		}

		Mson title = Txt.titleize("Tax for " + faction.getDisplayName(msender));
		message(title);

		MFlag flag = MFlag.getFlagTaxKick();
		String flagDesc = flag.getStateDesc(faction.getFlag(flag), true, false, false, true, true);
		msg("<k>Player Kick: <v>%s", flagDesc);

		boolean anyTax = false;
		for (Entry<String, Double> entry : faction.getTax().entrySet())
		{
			String id = entry.getKey();
			Double tax = entry.getValue();

			Rank rank = faction.getRank(id);
			MPlayer mplayer = MPlayer.get(id);

			String name;
			if (Faction.IDENTIFIER_TAX_BASE.equals(id)) name = "Default";
			else if (rank != null) name = rank.getName();
			else if (mplayer != null) name = mplayer.getDisplayName(msender);
			else continue;
			anyTax = true;
			msg("<k>%s: <v>%.2f", name, tax);
		}
		if (!anyTax) msg("<i>No players in this faction pays taxes.");
	}
	
}
