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
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CmdBookTitle extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookTitle()
	{
		// Parameters
		this.addParameter(TypeString.get(), "title", true);
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.TITLE));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesBookTitle());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		ItemStack item = TypeBookInHand.getWritten().read(sender);
		
		String target = this.readArg();
		String targetParsed = Txt.parse(target);
		boolean usingColor = (!ChatColor.stripColor(targetParsed).equals(targetParsed));
		
		if (usingColor)
		{
			if (!Perm.TITLE_COLOR.has(sender, true)) return;
			target = targetParsed;
		}
		
		String old = BookUtil.getTitle(item);
		
		if (BookUtil.isTitleEquals(item, target))
		{
			message(Lang.getSameTitle(old));
			return;
		}
		
		if (!BookUtil.isAuthorEquals(item, sender) && !Perm.TITLE_OTHER.has(sender, true)) return;
		
		BookUtil.setTitle(item, target);
		InventoryUtil.setMainHand(me, item);
		
		message(Lang.getAlterTitle(old, target));
	}

}
