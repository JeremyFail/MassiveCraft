package com.massivecraft.massivehat;

import com.massivecraft.massivecore.Identified;
import com.massivecraft.massivecore.util.PermissionUtil;
import org.bukkit.permissions.Permissible;

public enum Perm implements Identified
{
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //
	
	BASECOMMAND,
	
	USE,
	
	CONFIG,
	
	VERSION,
	
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
		this.id = PermissionUtil.createPermissionId(MassiveHat.get(), this);
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
