package com.massivecraft.factions.entity.migrator;

import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.store.migrator.MigratorFieldRename;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorMPlayer002UsingAdminMode extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorMPlayer002UsingAdminMode i = new MigratorMPlayer002UsingAdminMode();
	public static MigratorMPlayer002UsingAdminMode get() { return i; }
	private MigratorMPlayer002UsingAdminMode()
	{
		super(MPlayer.class);
		this.addInnerMigrator(MigratorFieldRename.get("usingAdminMode", "overriding"));
	}
	
}
