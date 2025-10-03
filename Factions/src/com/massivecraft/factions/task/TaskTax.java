package com.massivecraft.factions.task;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPlayerColl;
import com.massivecraft.factions.event.EventFactionsMembershipChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange.MembershipChangeReason;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.massivecore.Couple;
import com.massivecraft.massivecore.Task;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.TimeUnit;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskTax extends Task
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //

	private static TaskTax i = new TaskTax();
	public static TaskTax get() { return i; }
	public TaskTax()
	{
		// Just check once a minute
		this.setPeriod(60L * 20L);

		this.setMustBeTaskServer(true);
		this.setLoggingTimeSpent(true);

		this.addCondition(Econ::isEnabled);
		this.addCondition(() -> MConf.get().taxEnabled);
	}

	// -------------------------------------------- //
	// OVERRIDE: TASK
	// -------------------------------------------- //

	@Override
	public long getPreviousMillis()
	{
		return MConf.get().taxTaskLastMillis;
	}
	@Override
	public void setPreviousMillis(long millis)
	{
		MConf.get().taxTaskLastMillis = millis;
		MConf.get().changed();
	}

	@Override
	public long getPeriodMillis()
	{
		return MConf.get().taxTaskPeriodMillis;
	}
	@Override
	public long getOffsetMillis()
	{
		return MConf.get().taxTaskInvocationOffsetMillis;
	}

	// -------------------------------------------- //
	// OVERRIDE: RUNNABLE
	// -------------------------------------------- //

	@Override
	public void invoke(long now)
	{
		taxPlayers(now);
		taxFactions(now);
	}

	public void taxPlayers(long now)
	{
		MixinMessage.get().msgAll("<i>Taxation of players starting.");
		long start = System.nanoTime();

		// Tax players and track how many players are taxed and how much
		Map<Faction, Couple<Integer, Double>> faction2tax = new MassiveMap<>();

		List<Couple<MPlayer, Double>> taxes = MPlayerColl.get().getAll().stream()
			.filter(mp -> shouldBeTaxed(now, mp))
			.map(mp -> new Couple<>(mp, getTax(mp)))
			.filter(e -> e.getValue() != 0D)
			.collect(Collectors.toList());

		/*String debug = taxes.stream()
			.map(c -> c.getFirst().getName() + ": " + c.getSecond())
			.reduce((s1, s2) -> s1 + "\n" + s2).orElse("No players pay tax.");
		MixinMessage.get().messageAll(debug);*/

		// Pay the highest taxes first.
		// That way taxes are collected before wages are given.
		Comparator<Couple<MPlayer, Double>> comparator = Comparator.comparingDouble(Couple::getSecond);
		comparator = comparator.reversed();
		taxes.sort(comparator);

		for (Couple<MPlayer, Double> couple : taxes)
		{
			double tax = doTaxPlayer(couple);
			if (tax == 0D) continue;

			// Log data
			Faction faction = couple.getFirst().getFaction();
			Couple<Integer, Double> newCouple = new Couple<>(1, tax);
			faction2tax.merge(faction, newCouple,
				(c1, c2) -> new Couple<>(c1.getFirst() + c2.getFirst(), c1.getSecond() + c2.getSecond()));
		}

		// Inform factions
		faction2tax.forEach(this::informFactionOfPlayerTax);

		// Inform of taxation complete
		int count = faction2tax.values().stream().mapToInt(Couple::getFirst).sum();
		MixinMessage.get().msgAll("<i>Taxation of players complete. <h>%d <i>players were taxed.", count);

		long end = System.nanoTime();
		double elapsedSeconds = (end - start) / 1000_000_000D;
		MixinMessage.get().msgAll("<i>Took <h>%.2f <i>seconds.", elapsedSeconds);
	}

	private double getTax(MPlayer mplayer)
	{
		return mplayer.getFaction().getTaxForPlayer(mplayer);
	}

	private double doTaxPlayer(Couple<MPlayer, Double> couple)
	{
		return doTaxPlayer(couple.getFirst(), couple.getSecond());
	}

	private double doTaxPlayer(MPlayer mplayer, double tax)
	{
		Faction faction = mplayer.getFaction();
		boolean success = Econ.moveMoney(mplayer, faction, null, tax, "Factions Tax");
		if (success)
		{
			// Inform player
			if (mplayer.isOnline())
			{
				if (tax > 0) mplayer.msg("<i>You were just taxed <reset>%s <i> by your faction.", Money.format(tax)); // Tax
				else mplayer.msg("<i>You were just paid <reset>%s <i> by your faction.", Money.format(-tax)); // Salary
			}

			return tax;
		}
		else if (tax > 0) // If a tax
		{
			faction.msg("%s<i> couldn't afford tax!", mplayer.describeTo(faction));
			boolean kicked = tryKickPlayer(mplayer);
			if (!kicked) faction.msg("%s <i>could not afford tax.", mplayer.describeTo(faction));
			return 0D;
		}
		else // If a salary
		{
			faction.msg("<i>Your faction couldn't afford to pay <reset>%s <i>to %s<i>.", Money.format(-tax), mplayer.describeTo(faction));
			return 0D;
		}
	}

	private boolean shouldBeTaxed(long now, MPlayer mplayer)
	{
		// Must have faction
		if ( ! mplayer.hasFaction()) return false;

		// Must have been online recently
		long offlinePeriod;
		if (mplayer.isOnline()) offlinePeriod = 0;
		else offlinePeriod = now - mplayer.getLastActivityMillis();

		int inactiveDays = MConf.get().taxInactiveDays;
		if (inactiveDays > 0 && offlinePeriod > inactiveDays * TimeUnit.MILLIS_PER_DAY) return false;

		return true;
	}

	private boolean tryKickPlayer(MPlayer mplayer)
	{
		Faction faction = mplayer.getFaction();
		if (mplayer.getRank().isLeader()) return false;
		if ( ! faction.getFlag(MFlag.getFlagTaxKick())) return false;

		EventFactionsMembershipChange event = new EventFactionsMembershipChange(null, mplayer, FactionColl.get().getNone(), MembershipChangeReason.KICK);
		event.run();
		if (event.isCancelled()) return false;

		faction.msg("%s <i>could not afford tax and was kicked from your faction.", mplayer.describeTo(faction));

		if (MConf.get().logFactionKick)
		{
			MPlayer console = MPlayer.get(IdUtil.CONSOLE_ID);
			Factions.get().log("%s <i>could not afford tax and was kicked from <reset>%s<i>.", mplayer.describeTo(console), faction.describeTo(console));
		}

		// Apply
		faction.uninvite(mplayer);
		mplayer.resetFactionData();

		return true;
	}

	private void informFactionOfPlayerTax(Faction faction, Couple<Integer, Double> couple)
	{
		faction.msg("<i>A total of <h>%d <i>players in your faction were taxed for a total of <reset>%s<i>.", couple.getFirst(), Money.format(couple.getSecond()));
	}

	public void taxFactions(long now)
	{
		// TODO
		String msg = "<i>For the time being factions themselves cannot be taxed. This feature will be added at a later date.";
		MixinMessage.get().msgOne(IdUtil.CONSOLE_ID, msg);
	}

}
