package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivebooks.entity.MBookColl;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;

import java.util.Collection;
import java.util.List;

public class CmdBookList extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookList()
	{
		// Parameters
		this.addParameter(Parameter.getPage());
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.LIST));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesBookList;
	}
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		int page = this.readArg();
		
		// Pager Create
		Collection<MBook> mbooks = MBookColl.get().getAll();
		String title = mbooks.size() +" Saved Books";
		Pager<MBook> pager = new Pager<>(this, title, page, mbooks, (Stringifier<MBook>) (mbook, index) -> Lang.descDisplayName(mbook.getItem()));
		
		// Pager Message
		pager.message();
	}

}
