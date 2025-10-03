package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import org.bukkit.ChatColor;

public class CmdFactionsDocumentation extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsDocumentation()
	{
		String web = Factions.get().getDescription().getWebsite();
		this.setHelp(mson("More help can be found at ", mson(web).link(web).color(ChatColor.AQUA)));
	}

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdFactionsDocumentationPower cmdFactionsDocumentationPower = new CmdFactionsDocumentationPower();
	public CmdFactionsDocumentationRanks cmdFactionsDocumentationRanks = new CmdFactionsDocumentationRanks();
	public CmdFactionsDocumentationWarps cmdFactionsDocumentationWarps = new CmdFactionsDocumentationWarps();
	public CmdFactionsDocumentationFlags cmdFactionsDocumentationFlags = new CmdFactionsDocumentationFlags();
	public CmdFactionsDocumentationPerms cmdFactionsDocumentationPerms = new CmdFactionsDocumentationPerms();

}
