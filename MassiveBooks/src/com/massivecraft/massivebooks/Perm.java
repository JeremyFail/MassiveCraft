package com.massivecraft.massivebooks;

import com.massivecraft.massivecore.Identified;
import com.massivecraft.massivecore.util.PermissionUtil;
import org.bukkit.permissions.Permissible;

public enum Perm implements Identified
{
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //
	
	BOOK,

	UNLOCK,
	UNLOCK_OTHER,
	
	LOCK,
	LOCK_OTHER,
	
	CLEAR,
	CLEAR_OTHER,
	
	TITLE,
	TITLE_OTHER,
	TITLE_COLOR,
	
	AUTHOR,
	AUTHOR_OTHER,
	
	COPY,
	COPY_OTHER,
	COPY_COPYRIGHTED,
	
	LIST,
	LOAD,
	GIVE,
	SAVE,
	DELETE,
	
	AUTOUPDATE,
	
	POWERTOOL,
	POWERTOOL_OTHER,
	
	COPYRIGHTED,
	COPYRIGHTED_OTHER,
	
	VERSION,
	
	CONFIG,
	
	// END OF LIST
	;
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final String id;
	@Override public String getId() { return this.id; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	Perm()
	{
		this.id = PermissionUtil.createPermissionId(MassiveBooks.get(), this);
	}
	
	// -------------------------------------------- //
	// HAS
	// -------------------------------------------- //
	
	public boolean has(Permissible permissible, boolean verbose)
	{
		return PermissionUtil.hasPermission(permissible, this, verbose);
	}
	
	public boolean has(Permissible permissible)
	{
		return PermissionUtil.hasPermission(permissible, this);
	}
	
}
