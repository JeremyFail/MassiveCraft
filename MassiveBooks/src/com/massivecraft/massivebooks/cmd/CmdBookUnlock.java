package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.BookUtil;
import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.cmd.type.TypeBookInHand;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.util.InventoryUtil;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CmdBookUnlock extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookUnlock()
	{
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.UNLOCK));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesBookUnlock;
	}
	
	@Override
	public void perform() throws MassiveException
	{
		ItemStack item = TypeBookInHand.getEither().read(sender);
		
		if (BookUtil.isUnlocked(item))
		{
			message(Lang.getSameUnlock(item));
			return;
		}
		
		if (!BookUtil.isAuthorEquals(item, sender) && !Perm.UNLOCK_OTHER.has(sender, true)) return;
		
		ItemStack before = item.clone();
		BookUtil.unlock(item);
		InventoryUtil.setMainHand(me, item);
		
		message(Lang.getAlterUnlock(before));
	}

}
