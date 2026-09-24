package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.cmd.type.TypeGateSetting;
import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.gate.GateSetting;
import com.massivecraft.creativegates.ui.GateManageChat;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.Visibility;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeBooleanYes;
import com.massivecraft.massivecore.command.type.store.TypeEntity;
import com.massivecraft.massivecore.util.Txt;

import java.util.List;

/**
 * Click-to-toggle setter used by {@link com.massivecraft.creativegates.ui.GateManageChat}.
 */
public class CmdCgManageSet extends MassiveCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdCgManageSet()
	{
		this.setVisibility(Visibility.INVISIBLE);
		
		// Parameters: gate, setting, yes/no, [page]
		this.addParameter(TypeEntity.get(UGateColl.get()), "gate");
		this.addParameter(TypeGateSetting.get(), "setting");
		this.addParameter(TypeBooleanYes.get(), "yes/no");
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
		return new MassiveList<>(MConf.get().getAliasesCgManageSet());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		UGate gate = this.readArg();
		GateSetting setting = this.readArg();
		boolean value = this.readArg();
		int page = this.readArg();
		
		if (!EngineGateOverride.canManage(this.me, gate))
		{
			msg("<b>Only the gate creator can manage this gate.");
			return;
		}
		
		setting.set(gate, value);
		
		String yesNo = Txt.parse(value ? "<g>YES" : "<b>NOO");
		msg("<i>Set <h>%s <i>to %s<i>.", setting.getDisplayName(), yesNo);
		
		GateManageChat.display(this.sender, gate, page);
	}
	
}
