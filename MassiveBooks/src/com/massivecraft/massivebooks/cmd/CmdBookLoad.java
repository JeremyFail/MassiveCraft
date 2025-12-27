package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.cmd.type.TypeBookInHand;
import com.massivecraft.massivebooks.cmd.type.TypeMBook;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.util.InventoryUtil;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CmdBookLoad extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookLoad()
	{
		// Parameters
		this.addParameter(TypeMBook.get(), "title", true);
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.LOAD));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesBookLoad;
	}
	
	@Override
	public void perform() throws MassiveException
	{
		MBook mbook = this.readArg();
		ItemStack old = TypeBookInHand.getEither().read(sender);
		
		ItemStack target = mbook.getItem();
		target.setAmount(old.getAmount());
		
		InventoryUtil.setMainHand(me, target);
		
		message(Lang.getSuccessLoad(target));
	}

}
