package com.massivecraft.massivehat.cmd;

import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivehat.Perm;
import com.massivecraft.massivehat.entity.MConf;
import com.massivecraft.massivehat.predicate.PredicateIsHat;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CmdHatUse extends MassiveCommand
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static final CmdHatUse i = new CmdHatUse() { @Override public List<String> getAliases() { return new MassiveList<>(MConf.get().getAliasesHatUseOuter()); }};
	public static CmdHatUse get() { return i; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdHatUse()
	{
		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
		this.addRequirements(RequirementHasPerm.get(Perm.USE));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform()
	{
		final ItemStack mainHand = InventoryUtil.getMainHand(me);
		final ItemStack helmet = InventoryUtil.getHelmet(me);
		
		if (!PredicateIsHat.getHeadwear().apply(mainHand))
		{
			msg("<b>You are not holding a hat in your hand.");
			return;
		}
		
		if (helmet != null && helmet.containsEnchantment(Enchantment.BINDING_CURSE)){
			msg("<b>You cannot change your hat as your helmet is cursed with binding!");
			return;
		}
		
		InventoryUtil.setHelmet(me, mainHand);
		InventoryUtil.setMainHand(me, helmet);
		
		// This command is especially useful in creative mode since the normal inventory equip hack cannot be implemented there.
		// The client does not the relevant packets to the server in creative mode inventory.
		if (me.getGameMode() == GameMode.CREATIVE) return;
		msg("<h>NOTE: <i>You can equip in your inventory <h>like a regular helmet<i>.");
	}
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesHatUseInner());
	}
	
}
