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
		msgDoc("Permissions are checked in a hierarchy: relation > faction > rank > player. \n " + 
				"Because permissions are additive, once a permission is granted to a higher level of the hierarchy, it " +
				"will be granted to all lower levels of the hierarchy implicitly. Currently we do not support revoking " +
				"permissions at a lower level when it is granted at a higher level. You should not grant permissions " +
				"to a higher level of the hierarchy if you want to have more specific control over who has what permissions.");

		msgDoc("To list all available permissions, type:");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermList.getTemplate(false, true, sender));

		msgDoc("To manage permissions in a table (click YES/NOO to toggle), use manage. " +
				   "You can manage all ranks, all relationships, or a specific rank, relation, faction, or player with:");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermManage.getTemplate(false, true, sender));

		msgDoc("To see what relations, factions, ranks, or players have a specific permission granted for a faction, type:");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermInspect.getTemplate(false, true, sender));

		msgDoc("To view only the permissions directly given to a rank, relation, faction, or player, type:");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermView.getTemplate(false, true, sender));

		msgDoc("To view all permissions given to a rank, relation, faction, or player (including when those permissions are " +
			"granted implicitly by a higher level of the hierarchy), type (same targets as manage):");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermViewall.getTemplate(false, true, sender));

		msgDoc("To set a single permission for a rank, relation, faction, or player, type:");
		message(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermSet.getTemplate(false, true, sender));

		msgDoc("By default, permissions are only granted to ranks within your faction and a few permissions are granted " +
				"to allies, but if you have changed anything, you will see that using the commands above.");
		msgDoc("If/when you create a new rank, you will have to set up their permissions from scratch.");
	}
	
}
