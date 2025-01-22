package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqTaxEnabled;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeTaxable;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPlayerColl;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeDouble;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.util.MUtil;

public class CmdFactionsTaxSet extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsTaxSet()
	{
		this.addParameter(TypeDouble.get(), "tax");
		this.addParameter(TypeTaxable.get(), "default|rank|player|all", "default");
		this.addParameter(TypeFaction.get(), "faction", "your");

		this.addRequirements(ReqTaxEnabled.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		Double tax = this.readArg();

		if (MConf.get().taxPlayerMaximum < tax && tax != 0)
		{
			throw new MassiveException().addMsg("<b>Taxes can't be above <h>%s<b>.", Money.format(MConf.get().taxPlayerMaximum));
		}
		if (MConf.get().taxPlayerMinimum > tax && tax != 0)
		{
			throw new MassiveException().addMsg("<b>Taxes can't be below <h>%s<b>.", Money.format(MConf.get().taxPlayerMinimum));
		}

		Faction faction = this.readArgAt(2, msenderFaction);
		TypeTaxable typeTaxable = TypeTaxable.get(faction);
		String taxable = this.argIsSet(1) ? typeTaxable.read(this.argAt(1), sender) : Faction.IDENTIFIER_TAX_BASE;

		if ( ! MPerm.getPermTax().has(msender, faction, true)) return;

		Rank rank = faction.getRank(taxable);
		MPlayer mplayer = MPlayerColl.get().get(taxable, false);

		String name;
		if (Faction.IDENTIFIER_TAX_BASE.equalsIgnoreCase(taxable)) name = "Default";
		else if (rank != null) name = rank.getDisplayName(msender);
		else if (mplayer != null) name = mplayer.getDisplayName(msender);
		else throw new RuntimeException(taxable);

		String taxDesc = Money.format(tax);

		Double previous = faction.setTaxFor(taxable, tax);

		if (MUtil.equalsishNumber(tax, previous))
		{
			throw new MassiveException().addMsg("<b>The tax for <reset>%s <b>is already <h>%s<b>.", name, taxDesc);
		}

		msg("<i>The taxes for <reset>%s <i>is now set to <h>%s<i>.", name, taxDesc);
		if (tax < 0)
		{
			msg("<i>NOTE: A negative tax works like a salary.");
		}

		if (msender != mplayer && mplayer != null)
		{
			mplayer.msg("%s <i>set your tax to <reset>%s<i>.", msender.getDisplayName(mplayer), taxDesc);
		}
		if (rank != null)
		{
			String msg = "<i>Taxes for <reset>%s <i>set to <reset>%s <i>by %s<i>.";
			faction.getMPlayersWhereRank(rank).forEach(mp -> mp.msg(msg, name, taxDesc, msender.getDisplayName(mp)));
		}
	}
	
}
