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
import com.massivecraft.massivecore.command.type.primitive.TypeInteger;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.util.InventoryUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CmdBookCopy extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookCopy()
	{
		// Parameters
		this.addParameter(1, TypeInteger.get(), "times", "1");
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.COPY));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesBookCopy());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		// Get item arg
		ItemStack item = TypeBookInHand.getWritten().read(sender);
		BookUtil.updateBook(item);
		
		item = item.clone();
		item.setAmount(1);
		
		// Get times arg
		int times = this.readArg();
		if (times <= 0) throw new MassiveException().addMessage(Lang.TIMES_MUST_BE_POSITIVE);

		// Has right to copy?
		if (!BookUtil.hasCopyPerm(item, sender, true)) return;
		
		// What do we require?
		double moneyRequired = times * MConf.get().getCopyCost(me);
		int booksRequired = times;
		int inksacsRequired = times;
		int feathersRequired = times;
		
		if (me.getGameMode() == GameMode.CREATIVE)
		{
			moneyRequired = 0;
			booksRequired = 0;
			inksacsRequired = 0;
			feathersRequired = 0;
			// roomRequired = 0; // Nope, since the room always is required. It's not really a resource cost.
		}
		
		// The inventory in question
		Inventory inventory = me.getInventory();
		
		// Check ...
		
		// ... money (this is only a preliminary check)
		if (Money.enabled())
		{
			if (!Money.has(me, moneyRequired))
			{
				double moneyPossessed = Money.get(me);
				double moneyMissing = moneyRequired - moneyPossessed;
				this.sendCheckFailMessage(Lang.RESOURCE_MONEY, moneyRequired, moneyPossessed, moneyMissing);
				return;
			}
		}
		
		// ... books (the actual check)
		int booksPossessed = InventoryUtil.countSimilar(inventory, new ItemStack(Material.BOOK));
		if (!this.checkResource(Lang.RESOURCE_BOOKS, booksRequired, booksPossessed)) return;
		
		// ... inksacs (the actual check)
		int inksacsPossessed = InventoryUtil.countSimilar(inventory, new ItemStack(Material.INK_SAC));
		if (!this.checkResource(Lang.RESOURCE_INKSACS, inksacsRequired, inksacsPossessed)) return;
		
		// ... feathers (the actual check)
		int feathersPossessed = InventoryUtil.countSimilar(inventory, new ItemStack(Material.FEATHER));
		if (!this.checkResource(Lang.RESOURCE_FEATHERS, feathersRequired, feathersPossessed)) return;
		
		// ... room (the actual check)
		int roomPossessed = InventoryUtil.roomLeft(me.getInventory(), item, times);
		if (!this.checkResource(Lang.RESOURCE_ROOM, times, roomPossessed)) return;
		
		// Remove ...
		
		// ... money (real check here)
		if (Money.enabled() && moneyRequired != 0)
		{
			if (!Money.despawn(me, me, moneyRequired, "MassiveBooks"))
			{
				message(String.format(Lang.FAILED_TO_REMOVE_X, Lang.RESOURCE_MONEY));
				return;
			}
		}
		
		// ... books (assumed to succeed)
		inventory.removeItem(new ItemStack(Material.BOOK, booksRequired));
		
		// ... inksacs (assumed to succeed)
		inventory.removeItem(new ItemStack(Material.INK_SAC, inksacsRequired));
		
		// ... feathers (assumed to succeed)
		inventory.removeItem(new ItemStack(Material.FEATHER, feathersRequired));
		
		// ... room (assumed to succeed)
		// (add book copies)
		InventoryUtil.addItemTimes(me.getInventory(), item, times);
		
		// Inform		
		message(Lang.getSuccessCopyCopies(times));
		message(Lang.getSuccessCopyResources(moneyRequired, booksRequired, inksacsRequired, feathersRequired));
	}
	
	// -------------------------------------------- //
	// CHECK UTILITIES
	// -------------------------------------------- //
	
	public boolean checkResource(String resourceName, Integer required, Integer possessed)
	{
		int missing = required - possessed;
		if (missing <= 0) return true;
		this.sendCheckFailMessage(resourceName, required, possessed, missing);
		return false;
	}
	
	public void sendCheckFailMessage(String resourceName, Object required, Object possessed, Object missing)
	{
		String notEnoughMessage = String.format(Lang.NOT_ENOUGH_X, resourceName); 
		this.message(notEnoughMessage);
		String reportMessage = String.format(Lang.REQUIRED_X_POSSESSED_Y_MISSING_Z, required, possessed, missing);
		this.message(reportMessage);
	}
	
}
