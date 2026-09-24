package com.massivecraft.creativegates.entity;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.gate.GateOrientation;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.creativegates.gate.fill.GateTypeResolve;
import com.massivecraft.creativegates.gate.fill.ParticleGateType;
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
		this.allowedGateParticleTypes = ParticleGateType.sanitizeIds(this.allowedGateParticleTypes);
		this.allowedHorizontalGateParticleTypes = ParticleGateType.sanitizeIds(this.allowedHorizontalGateParticleTypes);
		this.normalizeParticleAmountConfig();
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
	
	private Set<String> aliasesCgInspect = MUtil.set("inspect", "view", "show");
	public Set<String> getAliasesCgInspect() { return this.aliasesCgInspect; }
	public void setAliasesCgInspect(Set<String> aliasesCgInspect) { this.aliasesCgInspect = aliasesCgInspect; }
	
	private Set<String> aliasesCgManage = MUtil.set("manage");
	public Set<String> getAliasesCgManage() { return this.aliasesCgManage; }
	public void setAliasesCgManage(Set<String> aliasesCgManage) { this.aliasesCgManage = aliasesCgManage; }
	
	private Set<String> aliasesCgManageSet = MUtil.set("set");
	public Set<String> getAliasesCgManageSet() { return this.aliasesCgManageSet; }
	public void setAliasesCgManageSet(Set<String> aliasesCgManageSet) { this.aliasesCgManageSet = aliasesCgManageSet; }
	
	private Set<String> aliasesCgManageFill = MUtil.set("fill");
	public Set<String> getAliasesCgManageFill() { return this.aliasesCgManageFill; }
	public void setAliasesCgManageFill(Set<String> aliasesCgManageFill) { this.aliasesCgManageFill = aliasesCgManageFill; }
	
	private Set<String> aliasesCgOverride = MUtil.set("override", "admin");
	public Set<String> getAliasesCgOverride() { return this.aliasesCgOverride; }
	public void setAliasesCgOverride(Set<String> aliasesCgOverride) { this.aliasesCgOverride = aliasesCgOverride; }
	
	private Set<String> aliasesCgTool = MUtil.set("tool", "tooltoggle", "toggletool");
	public Set<String> getAliasesCgTool() { return this.aliasesCgTool; }
	public void setAliasesCgTool(Set<String> aliasesCgTool) { this.aliasesCgTool = aliasesCgTool; }

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
	public PermissionDefault permissionDefaultSetFillParticleCount = PermissionDefault.TRUE;
	public PermissionDefault permissionDefaultUse = PermissionDefault.TRUE;
	
	public boolean verboseCreatePermission = true;
	public boolean verboseSetGateFillPermission = false;
	public boolean verboseSetFillParticleCountPermission = false;
	public boolean verboseUsePermission = true;

	public void updatePerms()
	{
		PermissionUtil.getPermission(false, true, Perm.CREATE.getId(), "create a gate", this.permissionDefaultCreate);
		PermissionUtil.getPermission(false, true, Perm.SET_GATE_FILL.getId(), "choose gate fill when creating or managing", this.permissionDefaultSetGateFill);
		PermissionUtil.getPermission(false, true, Perm.SET_FILL_PARTICLE_COUNT.getId(), "choose particle fill amount when creating or managing", this.permissionDefaultSetFillParticleCount);
		PermissionUtil.getPermission(false, true, Perm.USE.getId(), "use a gate", this.permissionDefaultUse);
	}

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	/**
	 * Vertical gate <em>block</em> fill allow-list as config ids ({@link SupportedGateType} names
	 * or material names). Particle fills use {@link #getAllowedGateParticleTypes()} instead.
	 * Resolved via {@link GateTypeResolve}.
	 */
	private Set<String> allowedGateTypes = MUtil.set(
		SupportedGateType.NETHER_PORTAL.name(),
		SupportedGateType.END_GATEWAY.name(),
		SupportedGateType.WATER.name(),
		SupportedGateType.LAVA.name(),
		Material.POWDER_SNOW.name(),
		Material.ICE.name(),
		Material.SCULK_VEIN.name(),
		Material.SCULK.name()
	);
	public Set<String> getAllowedGateTypes() { return new LinkedHashSet<>(this.allowedGateTypes); }
	public void setAllowedGateTypes(Set<String> allowedGateTypes)
	{
		Set<String> sanitized = sanitizeGateTypeIds(allowedGateTypes, false);
		this.changed(this.allowedGateTypes, sanitized);
		this.allowedGateTypes = sanitized;
	}

	/**
	 * Horizontal gate <em>block</em> fill allow-list as config ids (includes nether portal via
	 * BlockDisplay rotation; animation can freeze at some camera pitches). Particle fills use
	 * {@link #getAllowedHorizontalGateParticleTypes()}.
	 */
	private Set<String> allowedHorizontalGateTypes = MUtil.set(
		SupportedGateType.NETHER_PORTAL.name(),
		SupportedGateType.END_GATEWAY.name(),
		SupportedGateType.WATER.name(),
		SupportedGateType.LAVA.name(),
		Material.POWDER_SNOW.name(),
		Material.ICE.name(),
		Material.SCULK_VEIN.name(),
		Material.SCULK.name()
	);
	public Set<String> getAllowedHorizontalGateTypes() { return new LinkedHashSet<>(this.allowedHorizontalGateTypes); }
	public void setAllowedHorizontalGateTypes(Set<String> allowedHorizontalGateTypes)
	{
		Set<String> sanitized = sanitizeGateTypeIds(allowedHorizontalGateTypes, true);
		this.changed(this.allowedHorizontalGateTypes, sanitized);
		this.allowedHorizontalGateTypes = sanitized;
	}

	/**
	 * Vertical gate particle fill allow-list as {@code PARTICLE_*} (or bare enum) ids.
	 */
	private Set<String> allowedGateParticleTypes = ParticleGateType.defaultConfigIds();
	public Set<String> getAllowedGateParticleTypes() { return new LinkedHashSet<>(this.allowedGateParticleTypes); }
	public void setAllowedGateParticleTypes(Set<String> allowedGateParticleTypes)
	{
		Set<String> sanitized = ParticleGateType.sanitizeIds(allowedGateParticleTypes);
		this.changed(this.allowedGateParticleTypes, sanitized);
		this.allowedGateParticleTypes = sanitized;
	}

	/**
	 * Horizontal gate particle fill allow-list as {@code PARTICLE_*} (or bare enum) ids.
	 */
	private Set<String> allowedHorizontalGateParticleTypes = ParticleGateType.defaultConfigIds();
	public Set<String> getAllowedHorizontalGateParticleTypes() { return new LinkedHashSet<>(this.allowedHorizontalGateParticleTypes); }
	public void setAllowedHorizontalGateParticleTypes(Set<String> allowedHorizontalGateParticleTypes)
	{
		Set<String> sanitized = ParticleGateType.sanitizeIds(allowedHorizontalGateParticleTypes);
		this.changed(this.allowedHorizontalGateParticleTypes, sanitized);
		this.allowedHorizontalGateParticleTypes = sanitized;
	}

	/**
	 * Minimum particle-fill spawn count (slider floor).
	 */
	private int gateFillParticleAmountMin = 16;
	public int getGateFillParticleAmountMin() { return this.gateFillParticleAmountMin; }
	public void setGateFillParticleAmountMin(int gateFillParticleAmountMin)
	{
		this.gateFillParticleAmountMin = gateFillParticleAmountMin;
		this.normalizeParticleAmountConfig();
		this.changed();
	}
	
	/**
	 * Maximum particle-fill spawn count (slider ceiling).
	 */
	private int gateFillParticleAmountMax = 32;
	public int getGateFillParticleAmountMax() { return this.gateFillParticleAmountMax; }
	public void setGateFillParticleAmountMax(int gateFillParticleAmountMax)
	{
		this.gateFillParticleAmountMax = gateFillParticleAmountMax;
		this.normalizeParticleAmountConfig();
		this.changed();
	}
	
	/**
	 * Default particle-fill spawn count for new gates / players who cannot set amount.
	 */
	private int gateFillParticleAmountDefault = 16;
	public int getGateFillParticleAmountDefault() { return this.gateFillParticleAmountDefault; }
	public void setGateFillParticleAmountDefault(int gateFillParticleAmountDefault)
	{
		this.gateFillParticleAmountDefault = gateFillParticleAmountDefault;
		this.normalizeParticleAmountConfig();
		this.changed();
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
		boolean horizontal = orientation != null && orientation.isHorizontal();
		if (gateType.isParticleFill())
		{
			return horizontal
				? this.allowedHorizontalGateParticleTypes.contains(id)
				: this.allowedGateParticleTypes.contains(id);
		}
		return horizontal
			? this.allowedHorizontalGateTypes.contains(id)
			: this.allowedGateTypes.contains(id);
	}

	/**
	 * Resolved selectable <em>block</em> fills for UI / creation for the given orientation.
	 *
	 * @param orientation Gate orientation; null treats as vertical.
	 * @return Ordered list of resolved block types (invalid ids omitted).
	 */
	public List<GateType> getSelectableBlockGateTypes(GateOrientation orientation)
	{
		boolean horizontal = orientation != null && orientation.isHorizontal();
		Set<String> ids = horizontal ? this.allowedHorizontalGateTypes : this.allowedGateTypes;
		List<GateType> ret = new ArrayList<>();
		for (String id : ids)
		{
			GateType type = GateTypeResolve.parse(id);
			if (type == null || type.isParticleFill()) continue;
			if (!type.isCompatibleWith(orientation)) continue;
			ret.add(type);
		}
		return ret;
	}

	/**
	 * Resolved selectable <em>particle</em> fills for UI / creation for the given orientation.
	 *
	 * @param orientation Gate orientation; null treats as vertical.
	 * @return Ordered list of resolved particle types (invalid ids omitted).
	 */
	public List<GateType> getSelectableParticleGateTypes(GateOrientation orientation)
	{
		boolean horizontal = orientation != null && orientation.isHorizontal();
		Set<String> ids = horizontal ? this.allowedHorizontalGateParticleTypes : this.allowedGateParticleTypes;
		List<GateType> ret = new ArrayList<>();
		for (String id : ids)
		{
			ParticleGateType type = ParticleGateType.parse(id);
			if (type == null) continue;
			ret.add(type);
		}
		return ret;
	}

	/**
	 * Resolved selectable fills (blocks then particles) for UI / creation for the given orientation.
	 *
	 * @param orientation Gate orientation; null treats as vertical.
	 * @return Ordered list of resolved types (invalid ids omitted).
	 */
	public List<GateType> getSelectableGateTypes(GateOrientation orientation)
	{
		List<GateType> ret = new ArrayList<>();
		ret.addAll(this.getSelectableBlockGateTypes(orientation));
		ret.addAll(this.getSelectableParticleGateTypes(orientation));
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

	// Floor/ceiling portals (Portal-style). Nether portal fill uses rotated BlockDisplays
	// (client animation can freeze at some camera pitches - BlockDisplay limitation).
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
	 * When true, living mobs may use gates (wandering, on leads, and as living mounts).
	 * Individual gates may further disable via {@link UGate#isAllowMobs()}.
	 */
	private boolean gatesAllowMobs = true;
	public boolean isGatesAllowMobs() { return this.gatesAllowMobs; }
	public void setGatesAllowMobs(boolean gatesAllowMobs)
	{
		this.changed(this.gatesAllowMobs, gatesAllowMobs);
		this.gatesAllowMobs = gatesAllowMobs;
	}

	/**
	 * When true, non-living vehicles (boats, minecarts, etc.) may use gates.
	 * Individual gates may further disable via {@link UGate#isAllowVehicles()}.
	 * Living mounts are controlled by {@link #isGatesAllowMobs()} instead.
	 */
	private boolean gatesAllowVehicles = true;
	public boolean isGatesAllowVehicles() { return this.gatesAllowVehicles; }
	public void setGatesAllowVehicles(boolean gatesAllowVehicles)
	{
		this.changed(this.gatesAllowVehicles, gatesAllowVehicles);
		this.gatesAllowVehicles = gatesAllowVehicles;
	}

	/**
	 * Spigot backend only: how often to scan loaded-chunk gates for wandering mobs.
	 * Ignored on Paper ({@code EntityMoveEvent} backend). 20 ticks = 1 second. Default 10.
	 */
	private int gatesAllowMobsScanTicks = 10;
	public int getGatesAllowMobsScanTicks() { return this.gatesAllowMobsScanTicks; }
	public void setGatesAllowMobsScanTicks(int gatesAllowMobsScanTicks)
	{
		int sanitized = Math.max(1, gatesAllowMobsScanTicks);
		this.changed(this.gatesAllowMobsScanTicks, sanitized);
		this.gatesAllowMobsScanTicks = sanitized;
	}

	/**
	 * Configured default fill for vertical gates. Blank / invalid falls back to the first
	 * entry in {@link #getSelectableGateTypes(GateOrientation)}.
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
	 * entry in {@link #getSelectableGateTypes(GateOrientation)}.
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
	 * Normalizes allow-list ids: uppercase, resolvable only.
	 *
	 * @param ids Input ids; null becomes empty.
	 * @param horizontal When true, skips types incompatible with horizontal orientation.
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
			GateType type = GateTypeResolve.parse(id);
			if (type == null || type.isParticleFill()) continue;
			if (horizontal && !type.isCompatibleWith(GateOrientation.HORIZONTAL)) continue;
			sanitized.add(type.getConfigId());
		}
		return sanitized;
	}

	/**
	 * Clamps a raw particle amount into the absolute safe range {@code [1, 128]}.
	 *
	 * @param amount Raw value.
	 * @return Clamped value.
	 */
	private static int sanitizeParticleAmountAbsolute(int amount)
	{
		return Math.max(1, Math.min(128, amount));
	}
	
	/**
	 * Ensures min ≤ default ≤ max within the absolute safe range.
	 */
	private void normalizeParticleAmountConfig()
	{
		int min = sanitizeParticleAmountAbsolute(this.gateFillParticleAmountMin);
		int max = sanitizeParticleAmountAbsolute(this.gateFillParticleAmountMax);
		if (max < min) max = min;
		int def = sanitizeParticleAmountAbsolute(this.gateFillParticleAmountDefault);
		def = Math.max(min, Math.min(max, def));
		this.gateFillParticleAmountMin = min;
		this.gateFillParticleAmountMax = max;
		this.gateFillParticleAmountDefault = def;
	}
	
	/**
	 * Clamps a per-gate amount into the configured min/max window.
	 *
	 * @param amount Raw amount.
	 * @return Value in {@code [min, max]}.
	 */
	public int clampParticleAmount(int amount)
	{
		return Math.max(this.gateFillParticleAmountMin, Math.min(this.gateFillParticleAmountMax, amount));
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

	private Material materialManage = Material.BLAZE_ROD;
	public Material getMaterialManage() { return this.materialManage; }
	public void setMaterialManage(Material materialManage)
	{
		this.changed(this.materialManage, materialManage);
		this.materialManage = materialManage;
	}

	// Prevent gate creation in these worlds (by Bukkit world name).
	// Bypassed by creativegates.create.bypassdisabled, override mode, or cg.override.bypass.
	// Existing gates in these worlds can still be used.
	private Set<String> gateCreationDisabledWorlds = new MassiveSet<>();
	public Set<String> getGateCreationDisabledWorlds() { return new MassiveSet<>(this.gateCreationDisabledWorlds); }
	public void setGateCreationDisabledWorlds(Set<String> gateCreationDisabledWorlds)
	{
		this.changed(this.gateCreationDisabledWorlds, gateCreationDisabledWorlds);
		this.gateCreationDisabledWorlds = new MassiveSet<>(gateCreationDisabledWorlds);
	}
	
	/**
	 * True if gate creation is disabled in the named world (unless the player bypasses).
	 *
	 * @param worldName Bukkit world name.
	 * @return True if creation is disabled there.
	 */
	public boolean isGateCreationDisabledIn(String worldName)
	{
		if (worldName == null) return false;
		return this.gateCreationDisabledWorlds.contains(worldName);
	}
}
