package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.util.GateChainReorder;
import com.massivecraft.creativegates.util.GateLookUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeInteger;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.command.type.store.TypeEntity;

import java.util.List;

/**
 * Reorders a gate in its network chain (up, down, or absolute position).
 */
public class CmdCgManageReorder extends MassiveCommand
{
	public CmdCgManageReorder()
	{
		this.addParameter(TypeString.get(), "up|down|set");
		this.addParameter(TypeInteger.get(), "position", "0");
		this.addParameter(TypeEntity.get(UGateColl.get()), "gate", "look");
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG_MANAGE));
	}
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgManageReorder());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		String mode = this.readArg();
		int position = this.readArg(0);
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
		
		if (mode.equalsIgnoreCase("up"))
		{
			GateChainReorder.moveUp(this.me, gate, true);
			return;
		}
		
		if (mode.equalsIgnoreCase("down"))
		{
			GateChainReorder.moveDown(this.me, gate, true);
			return;
		}
		
		if (mode.equalsIgnoreCase("set"))
		{
			if (!GateChainReorder.canReorder(this.me, gate))
			{
				GateChainReorder.tellDenied(this.me, gate);
				return;
			}
			gate.setChainIndex(position);
			msg("<g>Gate chain position set to <h>%d<g>.", position);
			return;
		}
		
		msg("<b>Use up, down, or set.");
	}
}
