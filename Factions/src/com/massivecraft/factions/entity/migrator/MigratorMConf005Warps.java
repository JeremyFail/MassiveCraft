package com.massivecraft.factions.entity.migrator;

import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.store.migrator.MigratorFieldRename;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorMConf005Warps extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorMConf005Warps i = new MigratorMConf005Warps();
	public static MigratorMConf005Warps get() { return i; }
	private MigratorMConf005Warps()
	{
		super(MConf.class);
		this.addInnerMigrator(MigratorFieldRename.get("homesEnabled", "warpsEnabled"));
		this.addInnerMigrator(MigratorFieldRename.get("homesMustBeInClaimedTerritory", "warpsMustBeInClaimedTerritory"));
		this.addInnerMigrator(MigratorFieldRename.get("homesTeleportAllowedFromEnemyTerritory", "warpsTeleportAllowedFromEnemyTerritory"));
		this.addInnerMigrator(MigratorFieldRename.get("homesTeleportAllowedFromDifferentWorld", "warpsTeleportAllowedFromDifferentWorld"));
		this.addInnerMigrator(MigratorFieldRename.get("homesTeleportAllowedEnemyDistance", "warpsTeleportAllowedEnemyDistance"));
		this.addInnerMigrator(MigratorFieldRename.get("homesTeleportIgnoreEnemiesIfInOwnTerritory", "warpsTeleportIgnoreEnemiesIfInOwnTerritory"));
		this.addInnerMigrator(MigratorFieldRename.get("homesTeleportToOnDeathActive", "warpsTeleportToOnDeathActive"));
		this.addInnerMigrator(MigratorFieldRename.get("homesTeleportToOnDeathPriority", "warpsTeleportToOnDeathPriority"));
	}

}
