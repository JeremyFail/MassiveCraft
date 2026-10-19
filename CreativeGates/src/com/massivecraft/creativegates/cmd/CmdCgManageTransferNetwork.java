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
 * Admin whole-network transfer: moves all gates on the looked-at identity.
 */
public class CmdCgManageTransferNetwork extends MassiveCommand
{
	public CmdCgManageTransferNetwork()
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
		return new MassiveList<>(MConf.get().getAliasesCgManageTransferNetwork());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		if (!EngineGateOverride.canTransferWholeNetwork(this.me))
		{
			msg("<b>You do not have permission to transfer an entire network.");
			return;
		}
		
		GateScopeType scope = this.readArg();
		String ownerArg = this.readArg(null);
		UGate gate = this.readArg(null);
		if (gate == null) gate = GateLookUtil.getLookedAtGate(this.me);
		if (gate == null)
		{
			msg("<b>You must be looking at a gate.");
			return;
		}
		
		String ownerId = ownerArg;
		if (ownerArg != null && (ownerArg.equalsIgnoreCase("auto") || ownerArg.equalsIgnoreCase("me")))
		{
			ownerId = null;
		}
		
		GateNetworkTransfer.transferWholeNetwork(this.me, gate, scope, ownerId, true);
	}
}
