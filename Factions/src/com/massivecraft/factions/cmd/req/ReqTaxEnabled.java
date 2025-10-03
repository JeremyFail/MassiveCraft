package com.massivecraft.factions.cmd.req;

import com.massivecraft.factions.task.TaskTax;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementAbstract;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.command.CommandSender;

public class ReqTaxEnabled extends RequirementAbstract
{
	// -------------------------------------------- //
	// SERIALIZABLE
	// -------------------------------------------- //
	
	private static final long serialVersionUID = 1L;
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static ReqTaxEnabled i = new ReqTaxEnabled();
	public static ReqTaxEnabled get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public boolean apply(CommandSender sender, MassiveCommand command)
	{
		return TaskTax.get().areConditionsMet();
	}
	
	@Override
	public String createErrorMessage(CommandSender sender, MassiveCommand command)
	{
		return Txt.parse("<b>Tax is not enabled on this server.");
	}
	
}
