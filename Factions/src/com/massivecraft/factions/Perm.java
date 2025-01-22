package com.massivecraft.factions;

import com.massivecraft.massivecore.Identified;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.util.PermissionUtil;
import org.bukkit.permissions.Permissible;

public enum Perm implements Identified
{
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //

	// All of these are referenced in the code
	ACCESS_GRANT_ONE,
	ACCESS_GRANT_FILL,
	ACCESS_GRANT_SQUARE,
	ACCESS_GRANT_CIRCLE,
	ACCESS_DENY_ONE,
	ACCESS_DENY_FILL,
	ACCESS_DENY_SQUARE,
	ACCESS_DENY_CIRCLE,
	CLAIM_ONE,
	CLAIM_AUTO,
	CLAIM_FILL,
	CLAIM_SQUARE,
	CLAIM_CIRCLE,
	CLAIM_ALL,
	UNCLAIM_ONE,
	UNCLAIM_AUTO,
	UNCLAIM_FILL,
	UNCLAIM_SQUARE,
	UNCLAIM_CIRCLE,
	UNCLAIM_ALL,
	OVERRIDE,
	FLY,
	JOIN_OTHERS,
	INVITE_LIST_OTHER,
	TITLE_COLOR,
	POWERBOOST_SET,
	MONEY_BALANCE_ANY,
	SETPOWER,
	CONFIG,
	VERSION,

	// These are just here to tell the system that it is seechunk rather than see.chunk
	SEECHUNK,
	SEECHUNKOLD,
	
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
		this.id = PermissionUtil.createPermissionId(Factions.get(), this);
	}
	
	// -------------------------------------------- //
	// HAS
	// -------------------------------------------- //
	
	public boolean has(Permissible permissible, boolean verboose)
	{
		return PermissionUtil.hasPermission(permissible, this, verboose);
	}
	
	public boolean has(Permissible permissible)
	{
		return PermissionUtil.hasPermission(permissible, this);
	}

	public void hasOrThrow(Permissible permissible) throws MassiveException
	{
		PermissionUtil.hasPermissionOrThrow(permissible, this);
	}
	
}
