package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeManageTarget;
import com.massivecraft.factions.cmd.type.TypeMPermable;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.factions.util.PermTableUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;

public class CmdFactionsPermViewall extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsPermViewall()
	{
		this.addParameter(TypeManageTarget.get(), "rank/relation/faction/player").setDesc("ranks, relationships, or a specific rank/relation/faction/player");
		this.addParameter(TypeFaction.get(), "faction", "you");
		this.addParameter(Parameter.getPage());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// With 2 args, the second can be a faction or a page (depending on if faction is implied)
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

		String targetArg = this.readArgAt(0);
		if (PermTableUtil.isBulkRanksOrRelationshipsTarget(targetArg))
		{
			PermTableUtil.displayTable(sender, targetArg, faction, page, false, null, this);
			return;
		}

		TypeMPermable permableType = TypeMPermable.get(faction);
		MPermable permable = permableType.read(targetArg, sender);

		if (permable == faction)
		{
			throw new MassiveException().addMsg("<b>A faction can't have perms for itself.");
		}

		PermTableUtil.displayTable(sender, permable, faction, page, false, null, this);
	}
	
}
