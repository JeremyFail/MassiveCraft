package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.util.GateLookUtil;
import com.massivecraft.creativegates.util.GateNetworkTransfer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.enumeration.TypeEnum;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.command.type.store.TypeEntity;

import java.util.List;

/**
 * Changes a gate's network scope / owner while keeping the network name.
 */
public class CmdCgManageScope extends MassiveCommand
{
	public CmdCgManageScope()
	{
		this.addParameter(new TypeEnum<>(GateScopeType.class), "scope");
		this.addParameter(TypeString.get(), "owner", "auto");
		this.addParameter(TypeEntity.get(UGateColl.get()), "gate", "look");
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG_MANAGE));
	}
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgManageScope());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		GateScopeType scope = this.readArg();
		String ownerArg = this.readArg(null);
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
		
		String ownerId = ownerArg;
		if (ownerArg != null && (ownerArg.equalsIgnoreCase("auto") || ownerArg.equalsIgnoreCase("me")))
		{
			ownerId = null;
		}
		
		GateNetworkTransfer.transfer(this.me, gate, scope, ownerId, true);
	}
}
