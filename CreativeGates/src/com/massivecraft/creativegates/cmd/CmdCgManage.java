package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.ui.GateManageChat;
import com.massivecraft.creativegates.ui.GateManageUi;
import com.massivecraft.creativegates.util.GateLookUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.store.TypeEntity;
import com.massivecraft.massivecore.dialog.MDialog;

import java.util.List;

public class CmdCgManage extends MassiveCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdCgManageSet cmdCgManageSet = new CmdCgManageSet();
	public CmdCgManageFill cmdCgManageFill = new CmdCgManageFill();
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdCgManage()
	{
		// Children
		this.addChild(this.cmdCgManageSet);
		this.addChild(this.cmdCgManageFill);
		
		// Parameters: optional gate id (chat pagination), optional page
		this.addParameter(TypeEntity.get(UGateColl.get()), "gate", "look");
		this.addParameter(Parameter.getPage());
		
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG_MANAGE));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgManage());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		UGate gate = this.readArg(null);
		int page = this.readArg();
		
		if (gate == null)
		{
			gate = GateLookUtil.getLookedAtGate(this.me);
		}
		if (gate == null)
		{
			msg("<b>You must be looking at a gate to manage it.");
			return;
		}
		
		if (!EngineGateOverride.canManage(this.me, gate))
		{
			msg("<b>Only the gate creator can manage this gate.");
			return;
		}
		
		// Re-entry from chat pagination always uses chat table.
		if (this.argIsSet(0) || !MDialog.isNativeDialogAvailable())
		{
			GateManageChat.display(this.sender, gate, page);
			return;
		}
		
		GateManageUi.open(this.me, gate);
	}
	
}
