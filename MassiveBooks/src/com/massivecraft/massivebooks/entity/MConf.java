package com.massivecraft.massivebooks.entity;

import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivecore.command.editor.annotation.EditorName;
import com.massivecraft.massivecore.command.editor.annotation.EditorTypeInner;
import com.massivecraft.massivecore.command.type.TypeStringCommand;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.PermissionUtil;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

@SuppressWarnings("CanBeFinal")
@EditorName("config")
public class MConf extends Entity<MConf>
{
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	protected static transient MConf i;
	public static MConf get() { return i; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	// Command Aliases
	private Set<String> aliasesBook = MUtil.set("book", "books");
	public Set<String> getAliasesBook() { return this.aliasesBook; }
	public void setAliasesBook(Set<String> aliasesBook) { this.aliasesBook = aliasesBook; }

	private Set<String> aliasesBookUnlock = MUtil.set("unlock");
	public Set<String> getAliasesBookUnlock() { return this.aliasesBookUnlock; }
	public void setAliasesBookUnlock(Set<String> aliasesBookUnlock) { this.aliasesBookUnlock = aliasesBookUnlock; }

	private Set<String> aliasesBookLock = MUtil.set("lock");
	public Set<String> getAliasesBookLock() { return this.aliasesBookLock; }
	public void setAliasesBookLock(Set<String> aliasesBookLock) { this.aliasesBookLock = aliasesBookLock; }

	private Set<String> aliasesBookClear = MUtil.set("clear");
	public Set<String> getAliasesBookClear() { return this.aliasesBookClear; }
	public void setAliasesBookClear(Set<String> aliasesBookClear) { this.aliasesBookClear = aliasesBookClear; }

	private Set<String> aliasesBookTitle = MUtil.set("title");
	public Set<String> getAliasesBookTitle() { return this.aliasesBookTitle; }
	public void setAliasesBookTitle(Set<String> aliasesBookTitle) { this.aliasesBookTitle = aliasesBookTitle; }

	private Set<String> aliasesBookAuthor = MUtil.set("author");
	public Set<String> getAliasesBookAuthor() { return this.aliasesBookAuthor; }
	public void setAliasesBookAuthor(Set<String> aliasesBookAuthor) { this.aliasesBookAuthor = aliasesBookAuthor; }

	private Set<String> aliasesBookCopy = MUtil.set("copy");
	public Set<String> getAliasesBookCopy() { return this.aliasesBookCopy; }
	public void setAliasesBookCopy(Set<String> aliasesBookCopy) { this.aliasesBookCopy = aliasesBookCopy; }

	private Set<String> aliasesBookList = MUtil.set("list");
	public Set<String> getAliasesBookList() { return this.aliasesBookList; }
	public void setAliasesBookList(Set<String> aliasesBookList) { this.aliasesBookList = aliasesBookList; }

	private Set<String> aliasesBookLoad = MUtil.set("load");
	public Set<String> getAliasesBookLoad() { return this.aliasesBookLoad; }
	public void setAliasesBookLoad(Set<String> aliasesBookLoad) { this.aliasesBookLoad = aliasesBookLoad; }

	private Set<String> aliasesBookGive = MUtil.set("give");
	public Set<String> getAliasesBookGive() { return this.aliasesBookGive; }
	public void setAliasesBookGive(Set<String> aliasesBookGive) { this.aliasesBookGive = aliasesBookGive; }

	private Set<String> aliasesBookSave = MUtil.set("save");
	public Set<String> getAliasesBookSave() { return this.aliasesBookSave; }
	public void setAliasesBookSave(Set<String> aliasesBookSave) { this.aliasesBookSave = aliasesBookSave; }

	private Set<String> aliasesBookDelete = MUtil.set("delete");
	public Set<String> getAliasesBookDelete() { return this.aliasesBookDelete; }
	public void setAliasesBookDelete(Set<String> aliasesBookDelete) { this.aliasesBookDelete = aliasesBookDelete; }

	private Set<String> aliasesBookAutoupdate = MUtil.set("autoupdate");
	public Set<String> getAliasesBookAutoupdate() { return this.aliasesBookAutoupdate; }
	public void setAliasesBookAutoupdate(Set<String> aliasesBookAutoupdate) { this.aliasesBookAutoupdate = aliasesBookAutoupdate; }

	private Set<String> aliasesBookPowertool = MUtil.set("pt", "powertool");
	public Set<String> getAliasesBookPowertool() { return this.aliasesBookPowertool; }
	public void setAliasesBookPowertool(Set<String> aliasesBookPowertool) { this.aliasesBookPowertool = aliasesBookPowertool; }

	private Set<String> aliasesBookCopyrighted = MUtil.set("cr", "copyrighted");
	public Set<String> getAliasesBookCopyrighted() { return this.aliasesBookCopyrighted; }
	public void setAliasesBookCopyrighted(Set<String> aliasesBookCopyrighted) { this.aliasesBookCopyrighted = aliasesBookCopyrighted; }

	private Set<String> aliasesBookConfig = MUtil.set("config");
	public Set<String> getAliasesBookConfig() { return this.aliasesBookConfig; }
	public void setAliasesBookConfig(Set<String> aliasesBookConfig) { this.aliasesBookConfig = aliasesBookConfig; }

	private Set<String> aliasesBookVersion = MUtil.set("v", "version");
	public Set<String> getAliasesBookVersion() { return this.aliasesBookVersion; }
	public void setAliasesBookVersion(Set<String> aliasesBookVersion) { this.aliasesBookVersion = aliasesBookVersion; }
	
	// New Player Commands
	public boolean usingNewPlayerCommands = true;
	@EditorTypeInner(TypeStringCommand.class)
	public List<String> newPlayerCommands = MUtil.list("book give {p} ensure all");
	public boolean usingNewPlayerCommandsDelayTicks = true;
	public int newPlayerCommandsDelayTicks = 5;
	
	// Copy Cost
	
	public Map<String, Double> permToCopyCost = MUtil.map(
		"massivebooks.copycost.free", 0D,
		"massivebooks.copycost.0", 0D,
		"massivebooks.copycost.0.01", 0.01D,
		"massivebooks.copycost.0.02", 0.02D,
		"massivebooks.copycost.0.03", 0.03D,
		"massivebooks.copycost.0.1", 0.1D,
		"massivebooks.copycost.0.2", 0.2D,
		"massivebooks.copycost.0.3", 0.3D,
		"massivebooks.copycost.1", 1D,
		"massivebooks.copycost.2", 2D,
		"massivebooks.copycost.3", 3D,
		"massivebooks.copycost.10", 10D,
		"massivebooks.copycost.20", 20D,
		"massivebooks.copycost.30", 30D,
		"massivebooks.copycost.default", 0D
	);
	public double getCopyCost(Permissible permissible)
	{
		Double ret = PermissionUtil.pickFirstVal(permissible, this.permToCopyCost);
		if (ret == null) ret = 0D;
		return ret;
	}
	
	// Auto Update
	public boolean autoupdatingServerbooks = true;
	public boolean autoupdatingDisplayNames = true;
	public boolean usingAuthorDisplayName = false;
	
	// ItemFrame Load
	public boolean itemFrameLoadIfSneakTrue = false;
	public boolean itemFrameLoadIfSneakFalse = true;

	// ItemFrame Displayname
	public boolean itemFrameDisplaynameIfSneakTrue = false;
	public boolean itemFrameDisplaynameIfSneakFalse = true;
	
	// ItemFrame Rotate
	public boolean itemFrameRotateIfSneakTrue = true;
	public boolean itemFrameRotateIfSneakFalse = true;
	
	// -------------------------------------------- //
	// UTILS
	// -------------------------------------------- //
	
	public void createUpdatePermissionNodes()
	{
		for (Entry<String, Double> entry : this.permToCopyCost.entrySet())
		{
			final String name = entry.getKey();
			final Double copyCost = entry.getValue();
			String description = String.format(Lang.PERMISSION_DESCRIPTION_COPYCOST_TEMPLATE, copyCost);
			PermissionUtil.getPermission(true, true, name, description, PermissionDefault.FALSE);
		}
	}

}