package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.BookUtil;
import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.cmd.type.TypeBookInHand;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivebooks.entity.MBookColl;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CmdBookSave extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookSave()
	{
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.SAVE));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesBookSave());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		ItemStack item = TypeBookInHand.getEither().read(sender);		
		
		String title = BookUtil.getTitle(item);
		if (title == null)
		{
			message(Lang.BOOK_MUST_HAVE_TITLE);
			return;
		}
		
		MBook mbook = MBookColl.get().get(title, true);
		mbook.setItem(item);
		
		message(Lang.getSuccessSave(item));
	}

}
