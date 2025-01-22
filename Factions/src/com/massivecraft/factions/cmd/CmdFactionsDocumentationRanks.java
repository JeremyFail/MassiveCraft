package com.massivecraft.factions.cmd;

import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CmdFactionsDocumentationRanks extends FactionsCommandDocumentation
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsDocumentationRanks()
	{

	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		msgDoc("Ranks divide the faction into groups.");

		List<Rank> ranks = msenderFaction.getRanks().getAll(Comparator.comparingInt(Rank::getPriority).reversed());
		List<String> rankDesc = ranks.stream().map(r -> r.getDisplayName(msender)).collect(Collectors.toList());
		msgDoc("Your faction has: <reset>%s", Txt.implodeCommaAndDot(rankDesc, Txt.parse("<i>")));

		msgDoc("Ranks can have a prefix that will be prepended before any player name. Prefixes can be coloured.");
		msgDoc("All ranks have a priority showed in parentheses after the name.");

		Mson msonLeader = mson("The rank with the highest priority is deemed the “leader rank”" +
										 "(can be renamed) and only one person can have that rank")
		.tooltip("For yor faction the leader rank is" + rankDesc.get(0))
		.color(ChatColor.YELLOW);
		messageDoc(msonLeader);
		msgDoc("Whenever a new person joins the faction they will be assigned the rank with the lowest priority.");
		msgDoc("Priorities are important because they determine who can do what." +
				"For example: you can’t kick someone with the same or higher rank than yourself." +
				"So if you have both Officers, and Co-leaders, do not fear officers kicking co-leaders or the co-leaders kicking each other." +
				"They can’t. The same goes for changing ranks, titles and other similar things.");

		msgDoc("To show, set or edit ranks do:");
		message(CmdFactions.get().cmdFactionsRank.getTemplate(false, true, sender));
	}
	
}
