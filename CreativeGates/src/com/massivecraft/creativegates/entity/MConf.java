package com.massivecraft.creativegates.entity;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.gate.GateOrientation;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.creativegates.gate.fill.GateTypeResolve;
import com.massivecraft.creativegates.gate.fill.SupportedGateType;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.command.editor.annotation.EditorName;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.PermissionUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
		this.allowedGateTypes = sanitizeGateTypeIds(this.allowedGateTypes, false);
		this.allowedHorizontalGateTypes = sanitizeGateTypeIds(this.allowedHorizontalGateTypes, true);
		this.updatePerms();
		return this;
	}
	
	// -------------------------------------------- //
	// VERSION
	// -------------------------------------------- //
	
	public int version = 1;
	
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
	public String teleportationSound = "ENTITY_GHAST_SHOOT";
	public float teleportationSoundVolume = 1.0f;
	public float teleportationSoundPitch = 1.0f;
	public boolean teleportationMessageActive = true;
	
	public Sound resolveTeleportationSound()
	{
		if (this.teleportationSound == null || this.teleportationSound.isEmpty())
		{
			return Sound.ENTITY_GHAST_SHOOT;
		}
		
		try
		{
			return Sound.valueOf(this.teleportationSound.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex)
		{
			return Sound.ENTITY_GHAST_SHOOT;
		}
	}
	
	public PermissionDefault permissionDefaultCreate = PermissionDefault.TRUE;
	public PermissionDefault permissionDefaultSetGateFill = PermissionDefault.TRUE;
	public PermissionDefault permissionDefaultUse = PermissionDefault.TRUE;
	
	public boolean verboseCreatePermission = true;
	public boolean verboseSetGateFillPermission = false;
	public boolean verboseUsePermission = true;

	public void updatePerms()
	{
		PermissionUtil.getPermission(false, true, Perm.CREATE.getId(), "create a gate", this.permissionDefaultCreate);
		PermissionUtil.getPermission(false, true, Perm.SET_GATE_FILL.getId(), "choose gate fill material when creating", this.permissionDefaultSetGateFill);
		PermissionUtil.getPermission(false, true, Perm.USE.getId(), "use a gate", this.permissionDefaultUse);
	}

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	/**
	 * Vertical gate fill allow-list as config ids ({@link SupportedGateType} names or material names).
	 * Resolved via {@link GateTypeResolve}.
	 */
	private Set<String> allowedGateTypes = MUtil.set(
		SupportedGateType.NETHER_PORTAL.name(),
		SupportedGateType.WATER.name(),
		SupportedGateType.LAVA.name(),
		SupportedGateType.END_GATEWAY.name(),
		SupportedGateType.POWDER_SNOW.name(),
		SupportedGateType.ICE.name(),
		SupportedGateType.PACKED_ICE.name(),
		SupportedGateType.BLUE_ICE.name(),
		SupportedGateType.FROSTED_ICE.name()
	);
	public Set<String> getAllowedGateTypes() { return new LinkedHashSet<>(this.allowedGateTypes); }
	public void setAllowedGateTypes(Set<String> allowedGateTypes)
	{
		Set<String> sanitized = sanitizeGateTypeIds(allowedGateTypes, false);
		this.changed(this.allowedGateTypes, sanitized);
		this.allowedGateTypes = sanitized;
	}

	/**
	 * Horizontal gate fill allow-list as config ids. Never includes {@link SupportedGateType#NETHER_PORTAL}.
	 */
	private Set<String> allowedHorizontalGateTypes = MUtil.set(
		SupportedGateType.WATER.name(),
		SupportedGateType.LAVA.name(),
		SupportedGateType.POWDER_SNOW.name(),
		SupportedGateType.ICE.name(),
		SupportedGateType.PACKED_ICE.name(),
		SupportedGateType.BLUE_ICE.name(),
		SupportedGateType.FROSTED_ICE.name()
	);
	public Set<String> getAllowedHorizontalGateTypes() { return new LinkedHashSet<>(this.allowedHorizontalGateTypes); }
	public void setAllowedHorizontalGateTypes(Set<String> allowedHorizontalGateTypes)
	{
		Set<String> sanitized = sanitizeGateTypeIds(allowedHorizontalGateTypes, true);
		this.changed(this.allowedHorizontalGateTypes, sanitized);
		this.allowedHorizontalGateTypes = sanitized;
	}

	/**
	 * Returns whether a fill type is allowed for the orientation.
	 *
	 * @param gateType Type to check.
	 * @param orientation Gate orientation; null treats as vertical.
	 * @return True if its config id is listed and compatible.
	 */
	public boolean isGateTypeAllowed(GateType gateType, GateOrientation orientation)
	{
		if (gateType == null) return false;
		if (!gateType.isCompatibleWith(orientation)) return false;
		String id = gateType.getConfigId();
		if (orientation != null && orientation.isHorizontal())
		{
			return this.allowedHorizontalGateTypes.contains(id);
		}
		return this.allowedGateTypes.contains(id);
	}

	/**
	 * Resolved selectable fills for UI / creation for the given orientation.
	 *
	 * @param orientation Gate orientation; null treats as vertical.
	 * @return Ordered list of resolved types (invalid ids omitted).
	 */
	public List<GateType> getSelectableGateTypes(GateOrientation orientation)
	{
		boolean horizontal = orientation != null && orientation.isHorizontal();
		Set<String> ids = horizontal ? this.allowedHorizontalGateTypes : this.allowedGateTypes;
		List<GateType> ret = new ArrayList<>();
		for (String id : ids)
		{
			GateType type = GateTypeResolve.parse(id);
			if (type == null) continue;
			if (!type.isCompatibleWith(orientation)) continue;
			ret.add(type);
		}
		return ret;
	}

	/**
	 * When true, {@link SupportedGateType#WATER} gates place lava in the nether instead of water.
	 * Does not require {@link SupportedGateType#LAVA} to be in the allow-list; that type is separate
	 * for explicitly selectable lava gates (including overworld).
	 */
	private boolean replaceWaterWithLavaInNether = true;
	public boolean isReplaceWaterWithLavaInNether() { return this.replaceWaterWithLavaInNether; }
	public void setReplaceWaterWithLavaInNether(boolean replaceWaterWithLavaInNether)
	{
		this.changed(this.replaceWaterWithLavaInNether, replaceWaterWithLavaInNether);
		this.replaceWaterWithLavaInNether = replaceWaterWithLavaInNether;
	}

	// Floor/ceiling portals (Portal-style). Nether portal blocks are not compatible.
	private boolean horizontalGatesEnabled = true;
	public boolean isHorizontalGatesEnabled() { return this.horizontalGatesEnabled; }
	public void setHorizontalGatesEnabled(boolean horizontalGatesEnabled)
	{
		this.changed(this.horizontalGatesEnabled, horizontalGatesEnabled);
		this.horizontalGatesEnabled = horizontalGatesEnabled;
	}

	// Keep player momentum when passing through horizontal gates.
	private boolean horizontalGatesPreserveVelocity = true;
	public boolean isHorizontalGatesPreserveVelocity() { return this.horizontalGatesPreserveVelocity; }
	public void setHorizontalGatesPreserveVelocity(boolean horizontalGatesPreserveVelocity)
	{
		this.changed(this.horizontalGatesPreserveVelocity, horizontalGatesPreserveVelocity);
		this.horizontalGatesPreserveVelocity = horizontalGatesPreserveVelocity;
	}

	/**
	 * Configured default fill for vertical gates. Blank / invalid falls back to the first
	 * entry in {@link #getAllowedGateTypes()}.
	 */
	private String defaultGateType = "";
	public String getDefaultGateType() { return this.defaultGateType; }
	public void setDefaultGateType(String defaultGateType)
	{
		String sanitized = defaultGateType == null ? "" : defaultGateType.trim().toUpperCase();
		this.changed(this.defaultGateType, sanitized);
		this.defaultGateType = sanitized;
	}

	/**
	 * Configured default fill for horizontal gates. Blank / invalid falls back to the first
	 * entry in {@link #getAllowedHorizontalGateTypes()}.
	 */
	private String defaultHorizontalGateType = "";
	public String getDefaultHorizontalGateType() { return this.defaultHorizontalGateType; }
	public void setDefaultHorizontalGateType(String defaultHorizontalGateType)
	{
		String sanitized = defaultHorizontalGateType == null ? "" : defaultHorizontalGateType.trim().toUpperCase();
		this.changed(this.defaultHorizontalGateType, sanitized);
		this.defaultHorizontalGateType = sanitized;
	}

	/**
	 * Picks the default fill for newly created gates (when the player cannot or need not pick).
	 *
	 * @param orientation Gate orientation.
	 * @param world World the gate is created in (unused; reserved for future preference).
	 * @return An allowed compatible type, or null if none are configured.
	 */
	public GateType resolveDefaultGateType(GateOrientation orientation, World world)
	{
		List<GateType> selectable = this.getSelectableGateTypes(orientation);
		if (selectable.isEmpty()) return null;

		boolean horizontal = orientation != null && orientation.isHorizontal();
		String configuredId = horizontal ? this.defaultHorizontalGateType : this.defaultGateType;
		if (configuredId != null && !configuredId.isEmpty())
		{
			for (GateType type : selectable)
			{
				if (type.getConfigId().equals(configuredId)) return type;
			}
		}

		return selectable.get(0);
	}

	/**
	 * Normalizes allow-list ids: uppercase, resolvable only, strip NETHER_PORTAL when horizontal.
	 *
	 * @param ids Input ids; null becomes empty.
	 * @param horizontal When true, removes nether portal.
	 * @return Sanitized mutable set of config ids.
	 */
	private static Set<String> sanitizeGateTypeIds(Set<String> ids, boolean horizontal)
	{
		Set<String> sanitized = new LinkedHashSet<>();
		if (ids == null) return sanitized;
		for (String raw : ids)
		{
			if (raw == null) continue;
			String id = raw.trim().toUpperCase();
			if (id.isEmpty()) continue;
			if (horizontal && SupportedGateType.NETHER_PORTAL.name().equals(id)) continue;
			GateType type = GateTypeResolve.parse(id);
			if (type == null) continue;
			if (horizontal && !type.isCompatibleWith(GateOrientation.HORIZONTAL)) continue;
			sanitized.add(type.getConfigId());
		}
		return sanitized;
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
