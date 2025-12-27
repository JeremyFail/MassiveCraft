package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.BookUtil;
import com.massivecraft.massivebooks.Const;
import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.cmd.type.TypeBookInHand;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.primitive.TypeBooleanTrue;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CmdBookCopyrighted extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookCopyrighted()
	{
		// Parameters
		this.addParameter(TypeBooleanTrue.get(), "true/false", "toggle");
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.COPYRIGHTED));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesBookCopyrighted;
	}
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		ItemStack item = TypeBookInHand.getWritten().read(sender);
		
		boolean currentState = BookUtil.containsFlag(item, Const.COPYRIGHTED);
		boolean targetState = this.readArg(!currentState);
		
		// Detect NoChange
		if (targetState == currentState)
		{
			if (targetState)
			{
				message(Lang.COPYRIGHTED_ALREADY_TRUE);
			}
			else
			{
				message(Lang.COPYRIGHTED_ALREADY_FALSE);
			}
			return;
		}
		
		// Check other-perm if another author
		if (!BookUtil.isAuthorEquals(item, sender) && !Perm.COPYRIGHTED_OTHER.has(sender, true)) return;
		
		// Apply & Inform
		if (targetState)
		{
			BookUtil.addFlag(item, Const.COPYRIGHTED);
			message(Lang.COPYRIGHTED_CHANGED_TO_TRUE);
		}
		else
		{
			BookUtil.removeFlag(item, Const.COPYRIGHTED);
			message(Lang.COPYRIGHTED_CHANGED_TO_FALSE);
		}
	}

}
