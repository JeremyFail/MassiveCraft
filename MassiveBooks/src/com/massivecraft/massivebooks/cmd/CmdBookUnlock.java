package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.BookUtil;
import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.cmd.type.TypeBookInHand;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
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
		return new MassiveList<>(MConf.get().getAliasesBookUnlock());
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
		
		// Check if we have a stack - book and quills don't stack
		int amount = item.getAmount();
		if (amount > 1)
		{
			// Check if there's enough inventory space to unstack
			int emptySlots = InventoryUtil.countEmptySlots(me.getInventory());
			int slotsNeeded = amount - 1; // -1 because one will stay in the current slot
			
			if (emptySlots < slotsNeeded)
			{
				msg("<b>You need <h>%d<b> empty inventory slots to unlock a stack of <h>%d<b> books.", slotsNeeded, amount);
				return;
			}
		}
		
		ItemStack before = item.clone();
		
		// If it's a stack, we need to split them
		if (amount > 1)
		{
			// Remove the stack from hand
			InventoryUtil.setMainHand(me, null);
			
			// Unlock each book and add individually
			for (int i = 0; i < amount; i++)
			{
				ItemStack single = item.clone();
				single.setAmount(1);
				BookUtil.unlock(single);
				me.getInventory().addItem(single);
			}
		}
		else
		{
			// Single book - unlock in place
			BookUtil.unlock(item);
			InventoryUtil.setMainHand(me, item);
		}
		
		message(Lang.getAlterUnlock(before));
	}

}
