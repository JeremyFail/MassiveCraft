package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.util.GateLookUtil;
import com.massivecraft.creativegates.util.GateNetworkRename;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.command.type.store.TypeEntity;

import java.util.List;

/**
 * Changes a single gate's network name (joins another identity under the same scope/owner).
 */
public class CmdCgManageNetwork extends MassiveCommand
{
	public CmdCgManageNetwork()
	{
		this.addParameter(TypeString.get(), "name");
		this.addParameter(TypeEntity.get(UGateColl.get()), "gate", "look");
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG_MANAGE));
	}
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgManageNetwork());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		String name = this.readArg();
		UGate gate = this.readArg(null);
		if (gate == null) gate = GateLookUtil.getLookedAtGate(this.me);
		if (gate == null)
		{
			msg("<b>You must be looking at a gate.");
			return;
		}
		if (!EngineGateOverride.canManage(this.me, gate))
		{
			msg("<b>You may not manage that gate.");
			return;
		}
		
		GateNetworkRename.rename(this.me, gate, name, true);
	}
}
