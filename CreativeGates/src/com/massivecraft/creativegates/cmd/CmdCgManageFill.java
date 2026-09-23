package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.ui.GateFillPicker;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.Visibility;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.store.TypeEntity;

import java.util.List;

/**
 * Opens the fill picker for an existing gate (chat manage click target).
 */
public class CmdCgManageFill extends MassiveCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdCgManageFill()
	{
		this.setVisibility(Visibility.INVISIBLE);
		
		this.addParameter(TypeEntity.get(UGateColl.get()), "gate");
		
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CG_MANAGE));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesCgManageFill());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		UGate gate = this.readArg();
		
		if (!GateFillPicker.canChangeFill(this.me, gate))
		{
			msg("<b>You cannot change this gate's fill.");
			return;
		}
		
		GateFillPicker.open(this.me, gate);
	}
	
}
