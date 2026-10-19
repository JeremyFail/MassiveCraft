package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.MPlayer;
import com.massivecraft.creativegates.util.GateScopeUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.enumeration.TypeEnum;

import java.util.List;

/**
 * Cycles or sets the player's preferred create-network scope.
 */
public class CmdCgScope extends MassiveCommand
{
	public CmdCgScope()
	{
		this.addParameter(new TypeEnum<>(GateScopeType.class), "scope", "cycle");
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG));
	}
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgScope());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		GateScopeType requested = this.readArg(null);
		GateScopeType selected;
		if (requested == null)
		{
			selected = GateScopeUtil.cycleCreateScope(this.me);
		}
		else
		{
			if (!GateScopeUtil.isScopeOffered(this.me, requested, true)) return;
			if (requested == GateScopeType.FACTION)
			{
				String owner = GateScopeUtil.resolveOwnerIdForCreate(this.me, requested, true);
				if (owner != null && owner.isEmpty()) return;
			}
			MPlayer.get(this.me).setCreateScope(requested);
			selected = requested;
		}
		
		if (selected == null)
		{
			msg("<b>No create scopes are available to you.");
			return;
		}
		GateScopeUtil.tellCreateScope(this.me, selected);
	}
}
