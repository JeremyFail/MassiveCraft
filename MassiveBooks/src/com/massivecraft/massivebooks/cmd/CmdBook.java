package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;

import java.util.List;

public class CmdBook extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static final CmdBook i = new CmdBook();
	public static CmdBook get() { return i; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public final CmdBookUnlock cmdBookUnlock = new CmdBookUnlock();
	public final CmdBookLock cmdBookLock = new CmdBookLock();
	public final CmdBookClear cmdBookClear = new CmdBookClear();
	public final CmdBookTitle cmdBookTitle = new CmdBookTitle();
	public final CmdBookAuthor cmdBookAuthor = new CmdBookAuthor();
	public final CmdBookCopy cmdBookCopy = new CmdBookCopy();
	public final CmdBookList cmdBookList = new CmdBookList();
	public final CmdBookLoad cmdBookLoad = new CmdBookLoad();
	public final CmdBookGive cmdBookGive = new CmdBookGive();
	public final CmdBookSave cmdBookSave = new CmdBookSave();
	public final CmdBookDelete cmdBookDelete = new CmdBookDelete();
	public final CmdBookAutoupdate cmdBookAutoupdate = new CmdBookAutoupdate();
	public final CmdBookPowertool cmdBookPowertool = new CmdBookPowertool();
	public final CmdBookCopyrighted cmdBookCopyrighted = new CmdBookCopyrighted();
	public final CmdBookConfig cmdBookConfig = new CmdBookConfig();
	public final CmdBookVersion cmdBookVersion = new CmdBookVersion();
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBook()
	{
		// Children
		this.addChild(this.cmdBookUnlock);
		this.addChild(this.cmdBookLock);
		this.addChild(this.cmdBookClear);
		this.addChild(this.cmdBookTitle);
		this.addChild(this.cmdBookAuthor);
		this.addChild(this.cmdBookCopy);
		this.addChild(this.cmdBookList);
		this.addChild(this.cmdBookLoad);
		this.addChild(this.cmdBookGive);
		this.addChild(this.cmdBookSave);
		this.addChild(this.cmdBookDelete);
		this.addChild(this.cmdBookAutoupdate);
		this.addChild(this.cmdBookPowertool);
		this.addChild(this.cmdBookCopyrighted);
		this.addChild(this.cmdBookConfig);
		this.addChild(this.cmdBookVersion);
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.BOOK));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesBook());
	}

}
