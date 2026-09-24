package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.util.GateInspectUtil;
import com.massivecraft.creativegates.util.GateLookUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

import java.util.List;

public class CmdCgInspect extends MassiveCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdCgInspect()
	{
		// page first so clickable titleize can rewrite just the page number
		this.addParameter(Parameter.getPage());
		// optional entity id for pagination without looking at the gate (not advertised to players)
		this.addParameter(null, TypeString.get(), "id", "look");
		
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG_INSPECT));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgInspect());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		int page = this.readArg();
		String gateId = this.readArg();
		
		UGate gate;
		if (gateId != null)
		{
			gate = UGateColl.get().get(gateId);
			if (gate == null)
			{
				msg("<b>No such gate.");
				return;
			}
		}
		else
		{
			gate = GateLookUtil.getLookedAtGate(this.me);
			if (gate == null)
			{
				msg("<b>You must be looking at a gate to inspect it.");
				return;
			}
		}
		
		GateInspectUtil.show(this.sender, gate, page, this);
	}
	
}
