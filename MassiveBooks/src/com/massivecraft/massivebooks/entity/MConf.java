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
	public List<String> aliasesBook = MUtil.list("book", "books");
	public List<String> aliasesBookUnlock = MUtil.list("unlock");
	public List<String> aliasesBookLock = MUtil.list("lock");
	public List<String> aliasesBookClear = MUtil.list("clear");
	public List<String> aliasesBookTitle = MUtil.list("title");
	public List<String> aliasesBookAuthor = MUtil.list("author");
	public List<String> aliasesBookCopy = MUtil.list("copy");
	public List<String> aliasesBookList = MUtil.list("list");
	public List<String> aliasesBookLoad = MUtil.list("load");
	public List<String> aliasesBookGive = MUtil.list("give");
	public List<String> aliasesBookSave = MUtil.list("save");
	public List<String> aliasesBookDelete = MUtil.list("delete");
	public List<String> aliasesBookAutoupdate = MUtil.list("autoupdate");
	public List<String> aliasesBookPowertool = MUtil.list("pt", "powertool");
	public List<String> aliasesBookCopyrighted = MUtil.list("cr", "copyrighted");
	public List<String> aliasesBookConfig = MUtil.list("config");
	public List<String> aliasesBookVersion = MUtil.list("v", "version");
	
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