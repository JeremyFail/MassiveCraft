package com.massivecraft.factions.cmd.req;

import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementAbstract;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.command.CommandSender;

public class ReqRankIsAtLeast extends RequirementAbstract
{
	// -------------------------------------------- //
	// SERIALIZABLE
	// -------------------------------------------- //
	
	private static final long serialVersionUID = 1L;
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final Rank rank;
	public Rank getRank() { return this.rank; }
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	public static ReqRankIsAtLeast get(Rank rank) { return new ReqRankIsAtLeast(rank); }
	private ReqRankIsAtLeast(Rank rank) { this.rank = rank; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public boolean apply(CommandSender sender, MassiveCommand command)
	{
		if (MUtil.isntSender(sender)) return false;
		
		MPlayer mplayer = MPlayer.get(sender);
		return mplayer.getRank().isAtLeast(this.getRank());
	}
	
	@Override
	public String createErrorMessage(CommandSender sender, MassiveCommand command)
	{
		return Txt.parse("<b>You must be <h>%s <b>or higher to %s.", rank.getName(), getDesc(command));
	}
	
}
