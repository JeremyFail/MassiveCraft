package com.massivecraft.factions.cmd;

import com.massivecraft.massivecore.MassiveException;

public class CmdFactionsDocumentationPerms extends FactionsCommandDocumentation
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsDocumentationPerms()
	{

	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		msgDoc("Permissions decide who can do what in your faction. " +
				   "Permissions can be given to a rank, a player, a relation, " +
				   "everyone in another faction or everyone with a specific rank in another faction.");
		msgDoc("Because perms can be given to all of these groups individually, it allows for extreme degrees of fine tuning.");

		msgDoc("To list all permissions type:");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermList.getTemplate(false, true, sender));

		msgDoc("To see who has a specific perm type:");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermShow.getTemplate(false, true, sender));

		msgDoc("Per default permissions are only granted to ranks within your faction " +
				   "and a few perms are given to allies, but if you have changed it that will be displayed by the command above.");
		msgDoc("When you create a new rank, you will have to set up their perms from scratch.");

		msgDoc("If you want to know what permissions are specifically given to someone do:");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermView.getTemplate(false, true, sender));

		msgDoc("To set perms do: ");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermSet.getTemplate(false, true, sender));
	}
	
}
