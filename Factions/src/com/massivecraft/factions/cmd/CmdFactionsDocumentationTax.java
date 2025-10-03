package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqTaxEnabled;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.util.TimeDiffUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import com.massivecraft.massivecore.util.Txt;

import java.util.LinkedHashMap;

public class CmdFactionsDocumentationTax extends FactionsCommandDocumentation
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsDocumentationTax()
	{
		this.addRequirements(ReqTaxEnabled.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		LinkedHashMap<TimeUnit, Long> timeUnitcounts = TimeDiffUtil.limit(TimeDiffUtil.unitcounts(MConf.get().taxTaskPeriodMillis, TimeUnit.getAll()), 3);
		String periodString = TimeDiffUtil.formatedVerboose(timeUnitcounts);

		msgDoc("<key>Taxation Period: <i>every %s<i>.", periodString);

		long nextTaxationTime = MConf.get().taxTaskPeriodMillis + MConf.get().taxTaskPeriodMillis;

		msgDoc("<key>Next Taxation: %s", Txt.getTimeDeltaDescriptionRelNow(nextTaxationTime));

		String minTax = Money.format(MConf.get().taxPlayerMinimum);
		String maxTax = Money.format(MConf.get().taxPlayerMaximum);
		msgDoc("<i>Taxes for players can be set between <reset>%s <i>and <reset>%s<i>.", minTax, maxTax);

		double tax = msenderFaction.getTaxForPlayer(msender);
		if (tax > 0)
		{
			msgDoc("<i>You pay <reset>%s <i>in taxes.", Money.format(tax));
		}
		else if (tax < 0)
		{
			msgDoc("<i>Instead of taxes you faction pays you <reset>%s <i>.", Money.format(tax * -1));
		}
		else
		{
			msgDoc("<i>You don't pay taxes.");
		}
	}
	
}
