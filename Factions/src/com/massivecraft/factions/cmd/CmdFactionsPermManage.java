package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeManageTarget;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.factions.util.PermTableUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;

/**
 * Command to open the permission management table for a faction.
 * Displays permissions in a table with YES/NOO cells; click to toggle (when editable).
 * Target can be: ranks, relationships, or a specific rank/relation/faction/player.
 */
public class CmdFactionsPermManage extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	/**
	 * Sets up the command: permission, target argument, optional faction, and page.
	 */
	public CmdFactionsPermManage()
	{
		this.addRequirements(RequirementHasPerm.get(Perm.PERM_MANAGE));
		this.addParameter(TypeManageTarget.get(), "rank/relation/faction/player").setDesc("what to manage: ranks, relationships, or a specific rank/relation/faction/player");
		this.addParameter(TypeFaction.get(), "faction", "you").setDesc("the faction whose perms to manage");
		this.addParameter(Parameter.getPage());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	/**
	 * Runs the permission manage table: resolves target, filters perms, paginates, and sends title, header, and rows.
	 *
	 * @throws MassiveException if target cannot be resolved or page is invalid
	 */
	@Override
	public void perform() throws MassiveException
	{
		// With 2 args, the second can be a faction or a page (depending on if faction is implied)
		String targetArg = this.readArgAt(0);
		Faction faction;
		int page;
		int n = this.getArgs().size();
		if (n >= 3)
		{
			faction = this.readArgAt(1, msenderFaction);
			page = this.readArgAt(2, 1);
		}
		else if (n == 2)
		{
			String arg1 = this.argAt(1);
			try
			{
				page = Math.max(1, Integer.parseInt(arg1));
				faction = msenderFaction;
			}
			catch (NumberFormatException e)
			{
				faction = this.readArgAt(1, msenderFaction);
				page = 1;
			}
		}
		else
		{
			faction = msenderFaction;
			page = 1;
		}
		PermTableUtil.displayTable(sender, targetArg, faction, page, true, null, this);
	}

	/**
	 * Delegates to {@link PermTableUtil#getTargetArgForPermable(MPermable, Faction)} for use by Set, View, Viewall.
	 */
	public static String getTargetArgForPermable(MPermable permable, Faction faction)
	{
		return PermTableUtil.getTargetArgForPermable(permable, faction);
	}

}
