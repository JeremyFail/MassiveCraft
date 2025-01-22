package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.type.enumeration.TypeEnum;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;
import com.massivecraft.massivecore.util.TimeDiffUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.List;

public class CmdFactionsTop extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsTop()
	{
		// Parameters
		this.addParameter(new TypeEnum<>(TopCategory.class));
		this.addParameter(Parameter.getPage());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		TopCategory category = this.readArg();
		int page = this.readArg();
		final CommandSender sender = this.sender;
		final MPlayer msender = this.msender;

		if (category == TopCategory.MONEY && !Econ.isEnabled())
		{
			throw new MassiveException().addMsg("<b>Economy is not enabled.");
		}

		Pager<Faction> pager = new Pager<>(this, "Faction top", page);
		pager.setMsonifier((Stringifier<Faction>) (f, i) -> getValue(category, f, msender));

		// NOTE: The faction list is quite slow and mostly thread safe.
		// We run it asynchronously to spare the primary server thread.

		Bukkit.getScheduler().runTaskAsynchronously(Factions.get(), () -> {
			// Pager Items
			List<Faction> factions = FactionColl.get().getAll((f1, f2) -> (int) (getNumber(category, f2) - getNumber(category, f1)));
			pager.setItems(factions);

			// Pager Message
			pager.message();
		});
	}

	private static double getNumber(TopCategory category, Faction faction)
	{
		switch(category)
		{
			case MONEY: return Econ.getMoney(faction);
			case MEMBERS: return faction.getMPlayers().size();
			case TERRITORY: return faction.getLandCount();
			case AGE: return faction.getAge();
		}
		throw new RuntimeException();
	}

	private static String getValue(TopCategory category, Faction faction, MPlayer mplayer)
	{
		String ret = Txt.parse("%s<i>: ", faction.getName(mplayer));
		switch(category)
		{
			case MONEY: ret += Money.format(Econ.getMoney(faction), true); break;
			case MEMBERS: ret += faction.getMPlayers().size() + " members"; break;
			case TERRITORY: ret += faction.getLandCount() + " chunks"; break;
			case AGE:
				long ageMillis = faction.getAge();
				LinkedHashMap<TimeUnit, Long> ageUnitcounts = TimeDiffUtil.limit(TimeDiffUtil.unitcounts(ageMillis, TimeUnit.getAllButMillis()), 3);
				ret += TimeDiffUtil.formatedVerboose(ageUnitcounts);
				break;
		}
		return ret;
	}

	public enum TopCategory
	{
		MONEY,
		MEMBERS,
		TERRITORY,
		AGE,
	}
	
}
