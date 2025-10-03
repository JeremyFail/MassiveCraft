package com.massivecraft.factions.predicate;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.command.CommandSender;

import java.io.Serializable;
import java.util.function.Predicate;

public class PredicateCommandSenderFaction implements Predicate<CommandSender>, Serializable
{
	private static final long serialVersionUID = 1L;
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final String factionId;
	public String getFactionId() { return this.factionId; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public PredicateCommandSenderFaction(Faction faction)
	{
		this.factionId = faction.getId();
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public boolean test(CommandSender sender)
	{
		if (MUtil.isntSender(sender)) return false;
		
		MPlayer mplayer = MPlayer.get(sender);
		return this.factionId.equals(mplayer.getFaction().getId());
	}

}
