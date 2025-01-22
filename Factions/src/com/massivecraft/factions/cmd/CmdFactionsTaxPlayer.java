package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqTaxEnabled;
import com.massivecraft.factions.cmd.type.TypeMPlayer;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsTaxPlayer extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsTaxPlayer()
	{
		this.addParameter(TypeMPlayer.get(), "player", "you");

		this.addRequirements(ReqTaxEnabled.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		MPlayer mplayer = this.readArg(msender);

		Mson title = Txt.titleize("Tax for " + mplayer.getDisplayName(msender));
		message(title);

		String factionName = mplayer.getFaction().describeTo(msender);
		msg("<k>Faction: <v>%s", factionName);

		double tax = mplayer.getFaction().getTaxForPlayer(mplayer);
		String taxDesc = Money.format(tax);
		msg("<k>Tax: <v>%s", taxDesc);
	}
	
}
