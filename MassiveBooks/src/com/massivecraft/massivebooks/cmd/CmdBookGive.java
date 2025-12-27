package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.cmd.type.TypeBookAmount;
import com.massivecraft.massivebooks.cmd.type.TypeMBook;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.type.container.TypeList;
import com.massivecraft.massivecore.command.type.sender.TypePlayer;
import com.massivecraft.massivecore.mixin.MixinDisplayName;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.InventoryUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Objects;

public class CmdBookGive extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookGive()
	{
		// Parameters
		this.addParameter(TypePlayer.get(), true, "player", "you");
		this.addParameter(1, TypeBookAmount.get(), "amount", "1");
		this.addParameter(TypeList.get(TypeMBook.get()), "title", "*bookandquill*", true);
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.GIVE));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesBookGive;
	}
	
	@Override
	public void perform() throws MassiveException
	{
		Player player = this.readArg(me);
		
		// How many? or perhaps ensure the player has at least one?
		Integer amount = this.readArg();
		boolean ensure = Objects.equals(amount, TypeBookAmount.ENSURE);
		if (ensure) amount = 1;
		
		// What items should we give?
		List<MBook> mbooks = this.readArg(new MassiveList<>());
		List<ItemStack> items = new MassiveList<>();
		if (mbooks.isEmpty())
		{
			items.add(new ItemStack(Material.WRITABLE_BOOK));
		}
		else
		{
			for (MBook mbook : mbooks)
			{
				items.add(mbook.getItem());
			}
		}
		
		// Now for each item ...
		for (ItemStack item : items)
		{
			PlayerInventory inventory = player.getInventory();
			if (ensure && inventory.containsAtLeast(item, 1))
			{
				MixinMessage.get().messageOne(sender, Lang.getAlreadyHave(MixinDisplayName.get().getDisplayName(player, sender), item));
				MixinMessage.get().messageOne(player, Lang.getAlreadyHave("You", item));
				continue;
			}
			
			if (InventoryUtil.roomLeft(inventory, item, amount) < amount)
			{
				MixinMessage.get().messageOne(sender, Lang.getNotEnoughRoomFor(amount, item));
				MixinMessage.get().messageOne(player, Lang.getNotEnoughRoomFor(amount, item));
				continue;
			}
			
			InventoryUtil.addItemTimes(inventory, item, amount);
			
			MixinMessage.get().messageOne(sender, Lang.getGave("You", MixinDisplayName.get().getDisplayName(player, sender), amount, item));
			MixinMessage.get().messageOne(player, Lang.getGave(MixinDisplayName.get().getDisplayName(sender, player), "you", amount, item));
		}
	}

}
