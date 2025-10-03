package com.massivecraft.creativegates.entity;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.command.editor.annotation.EditorName;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.PermissionUtil;
import org.bukkit.Material;
import org.bukkit.permissions.PermissionDefault;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@EditorName("config")
public class MConf extends Entity<MConf>
{
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	protected static transient MConf i;
	public static MConf get() { return i; }
	
	@Override
	public MConf load(MConf that)
	{
		super.load(that);
		this.updatePerms();
		return this;
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private boolean enabled = true;
	public boolean isEnabled() { return this.enabled; }
	public void setEnabled(boolean enabled)
	{
		this.changed(this.enabled, enabled);
		this.enabled = enabled;
	}

	// Aliases
	private Set<String> aliasesCg = MUtil.set("cg", "creativegates", "creativegate");
	public Set<String> getAliasesCg() { return this.aliasesCg; }
	public void setAliasesCg(Set<String> aliasesCg) { this.aliasesCg = aliasesCg; }
	
	private Set<String> aliasesCgWorld = MUtil.set("world");
	public Set<String> getAliasesCgWorld() { return this.aliasesCgWorld; }
	public void setAliasesCgWorld(Set<String> aliasesCgWorld) { this.aliasesCgWorld = aliasesCgWorld; }

	private Set<String> aliasesCgWorldList = MUtil.set("list");
	public Set<String> getAliasesCgWorldList() { return this.aliasesCgWorldList; }
	public void setAliasesCgWorldList(Set<String> aliasesCgWorldList) { this.aliasesCgWorldList = aliasesCgWorldList; }
	
	private Set<String> aliasesCgWorldDelete = MUtil.set("delete");
	public Set<String> getAliasesCgWorldDelete() { return this.aliasesCgWorldDelete; }
	public void setAliasesCgWorldDelete(Set<String> aliasesCgWorldDelete) { this.aliasesCgWorldDelete = aliasesCgWorldDelete; }
	
	private Set<String> aliasesCgConfig = MUtil.set("config");
	public Set<String> getAliasesCgConfig() { return this.aliasesCgConfig; }
	public void setAliasesCgConfig(Set<String> aliasesCgConfig) { this.aliasesCgConfig = aliasesCgConfig; }

	private Set<String> aliasesCgVersion = MUtil.set("v", "version");
	public Set<String> getAliasesCgVersion() { return this.aliasesCgVersion; }
	public void setAliasesCgVersion(Set<String> aliasesCgVersion) { this.aliasesCgVersion = aliasesCgVersion; }

	public boolean teleportationSoundActive = true;
	public boolean teleportationMessageActive = true;
	
	public PermissionDefault permissionDefaultCreate = PermissionDefault.TRUE;
	public PermissionDefault permissionDefaultUse = PermissionDefault.TRUE;
	
	public boolean verboseCreatePermission = true;
	public boolean verboseUsePermission = true;

	public void updatePerms()
	{
		PermissionUtil.getPermission(false, true, Perm.CREATE.getId(), "create a gate", this.permissionDefaultCreate);
		PermissionUtil.getPermission(false, true, Perm.USE.getId(), "use a gate", this.permissionDefaultUse);
	}

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private boolean usingWater = false;
	public boolean isUsingWater() { return this.usingWater; }
	public void setUsingWater(boolean usingWater)
	{
		this.changed(this.usingWater, usingWater);
		this.usingWater = usingWater;
	}

	private boolean pigmanPortalSpawnAllowed = true;
	public boolean isPigmanPortalSpawnAllowed() { return this.pigmanPortalSpawnAllowed; }
	public void setPigmanPortalSpawnAllowed(boolean pigmanPortalSpawnAllowed)
	{
		this.changed(this.pigmanPortalSpawnAllowed, pigmanPortalSpawnAllowed);
		this.pigmanPortalSpawnAllowed = pigmanPortalSpawnAllowed;
	}

	private int maxarea = 200;
	public int getMaxarea() { return this.maxarea; }
	public void setMaxarea(int maxarea)
	{
		this.changed(this.maxarea, maxarea);
		this.maxarea = maxarea;
	}

	private Map<Material, Integer> blocksrequired = MUtil.map(
		Material.EMERALD_BLOCK, 2
	);
	public Map<Material, Integer> getBlocksrequired() { return new HashMap<>(this.blocksrequired); }
	public void setBlocksrequired(Map<Material, Integer> blocksrequired)
	{
		this.changed(this.blocksrequired, blocksrequired);
		this.blocksrequired = new HashMap<>(blocksrequired);
	}

	private boolean removingCreateToolName = true;
	public boolean isRemovingCreateToolName() { return this.removingCreateToolName; }
	public void setRemovingCreateToolName(boolean removingCreateToolName)
	{
		this.changed(this.removingCreateToolName, removingCreateToolName);
		this.removingCreateToolName = removingCreateToolName;
	}

	private boolean removingCreateToolItem = false;
	public boolean isRemovingCreateToolItem() { return this.removingCreateToolItem; }
	public void setRemovingCreateToolItem(boolean removingCreateToolItem)
	{
		this.changed(this.removingCreateToolItem, removingCreateToolItem);
		this.removingCreateToolItem = removingCreateToolItem;
	}

	private Material materialCreate = Material.CLOCK;
	public Material getMaterialCreate() { return this.materialCreate; }
	public void setMaterialCreate(Material materialCreate)
	{
		this.changed(this.materialCreate, materialCreate);
		this.materialCreate = materialCreate;
	}

	private Material materialInspect = Material.BLAZE_POWDER;
	public Material getMaterialInspect() { return this.materialInspect; }
	public void setMaterialInspect(Material materialInspect)
	{
		this.changed(this.materialInspect, materialInspect);
		this.materialInspect = materialInspect;
	}

	private Material materialSecret = Material.MAGMA_CREAM;
	public Material getMaterialSecret() { return this.materialSecret; }
	public void setMaterialSecret(Material materialSecret)
	{
		this.changed(this.materialSecret, materialSecret);
		this.materialSecret = materialSecret;
	}

	private Material materialMode = Material.BLAZE_ROD;
	public Material getMaterialMode() { return this.materialMode; }
	public void setMaterialMode(Material materialMode)
	{
		this.changed(this.materialMode, materialMode);
		this.materialMode = materialMode;
	}

	// Prevent gate creation in these worlds
	// Can be bypassed with the bypass permission
	// This still allows gate usage in these world if gates exist there
	private Set<String> gateCreationDisabledWorlds = new MassiveSet<>();
	public Set<String> getGateCreationDisabledWorlds() { return new MassiveSet<>(this.gateCreationDisabledWorlds); }
	public void setGateCreationDisabledWorlds(Set<String> gateCreationDisabledWorlds)
	{
		this.changed(this.gateCreationDisabledWorlds, gateCreationDisabledWorlds);
		this.gateCreationDisabledWorlds = new MassiveSet<>(gateCreationDisabledWorlds);
	}
}
