package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqFactionWarpsEnabled;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.massivecore.MassiveException;

import java.util.Set;

public class CmdFactionsDocumentationWarps extends FactionsCommandDocumentation
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsDocumentationWarps()
	{
		this.addRequirements(ReqFactionWarpsEnabled.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		msgDoc("A faction can have warps which allows it's members to easily go to important places within the faction.");

		if (MConf.get().warpsMax < 0)
		{
			msgDoc("There is <h>no limit <i>to how many warps a faction can have.");
		}
		else
		{
			msgDoc("A faction can only have <h>%d <i>warps.", MConf.get().warpsMax);
		}

		if (MConf.get().warpsMustBeInClaimedTerritory)
		{
			msgDoc("Warps must be within claimed territory.");
		}

		if (MConf.get().warpsTeleportToOnDeathActive)
		{
			msgDoc("If your faction has a warp with the name <h>%s <i>you will teleport there after death.", MConf.get().warpsTeleportToOnDeathName);
		}

		if (!MConf.get().warpsTeleportAllowedFromEnemyTerritory)
		{
			msgDoc("You can't use faction warps while in enemy territory.");
		}

		if (!MConf.get().warpsTeleportAllowedFromDifferentWorld)
		{
			msgDoc("You can't teleporty to a warp from another world.");
		}

		if (MConf.get().warpsTeleportAllowedEnemyDistance > 0)
		{
			String str = String.format("You can't teleport home if there is an enemy within <h>%.1f <i>blocks of you", MConf.get().warpsTeleportAllowedEnemyDistance);
			if (MConf.get().warpsTeleportIgnoreEnemiesIfInOwnTerritory) str += " unless you are in your own territory.";
			else str += ".";
			msgDoc(str);
		}

		if (msenderFaction.isNormal())
		{
			Set<MPermable> set = msenderFaction.getPermittedPermables(MPerm.getPermWarp());
			String permables = CmdFactionsPermShow.permablesToDisplayString(set, msender);
			msgDoc("In your faction warps can be used by: %s<i>.", permables);
		}
	}
	
}
