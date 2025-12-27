package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.BookUtil;
import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.cmd.type.TypeBookInHand;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.InventoryUtil;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CmdBookAuthor extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookAuthor()
	{
		// Parameters
		this.addParameter(TypeString.get(), "author", true);
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.AUTHOR));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesBookAuthor;
	}
	
	@Override
	public void perform() throws MassiveException
	{
		ItemStack item = TypeBookInHand.getWritten().read(sender);
		
		String target = this.readArg();
		target = IdUtil.getName(target);
		String old = BookUtil.getAuthor(item);

		if (BookUtil.isAuthorEqualsId(item, target))
		{
			message(Lang.getSameAuthor(old));
			return;
		}
		
		if (!BookUtil.isAuthorEquals(item, sender) && !Perm.AUTHOR_OTHER.has(sender, true)) return;
		
		BookUtil.setAuthor(item, target);
		InventoryUtil.setMainHand(me, item);
		
		message(Lang.getAlterAuthor(old, target));
	}
	
}
