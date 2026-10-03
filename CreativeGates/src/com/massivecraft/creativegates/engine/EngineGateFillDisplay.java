package com.massivecraft.creativegates.engine;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.gate.GateOrientation;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.creativegates.gate.fill.SupportedGateType;
import com.massivecraft.creativegates.util.GateFillDisplayUtil;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.util.ReflectionUtil;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spawns and heals BlockDisplay / ItemDisplay fill visuals for creative gates.
 * <p>
 * END_GATEWAY on Minecraft versions before 26.1 cannot render as a BlockDisplay
 * (shader block). Those versions use a temporary {@link Player#sendBlockChange} fallback.
 * TODO: Remove {@link #usesEndGatewayBlockChangeFallback()} and the fallback path when
 * the plugin minimum Minecraft version is ≥ 26.1.
 * </p>
 * <p>
 * Fire, soul fire, and <em>vertical</em> nether portal use {@link Player#sendBlockChange}
 * over the whole interior (server {@link Material#LIGHT}). That gives correct client
 * animation without real portal/fire blocks. Horizontal nether portal stays BlockDisplay
 * (vanilla portal models cannot lie flat).
 * </p>
 * <p>
 * Face-attached fills listed in {@link GateFillDisplayUtil#MATERIALS_ITEM_DISPLAY_FILL}
 * (sculk vein, resin clump, vines, etc.) use {@link ItemDisplay} with the item model
 * instead of a squashed BlockDisplay.
 * </p>
 */
public class EngineGateFillDisplay extends Engine
{
	private static final EngineGateFillDisplay i = new EngineGateFillDisplay();
	
	/**
	 * @return Singleton engine instance.
	 */
	public static EngineGateFillDisplay get() { return i; }
	
	/** Max distance (blocks) from gate center at which overlays / display sync run for a player. */
	private static final int VIEW_DISTANCE_BLOCKS = 64;
	
	/** Minimum time between move-triggered {@link #syncPlayer(Player)} calls per player. */
	private static final long MOVE_SYNC_INTERVAL_MS = 750L;
	
	/** Z/X/Y scale used when squashing a BlockDisplay into a thin gate pane. */
	private static final float PANE_THICKNESS = 0.125f;
	
	private final Map<UUID, Long> lastMoveSyncMillis = new HashMap<>();
	
	private NamespacedKey keyGateId;
	private NamespacedKey keyFillDisplay;
	
	/**
	 * Registers persistent-data keys used to tag gate-owned display entities when the engine activates.
	 *
	 * @param active True when this engine is being enabled.
	 */
	@Override
	public void setActiveInner(boolean active)
	{
		if (active)
		{
			CreativeGates plugin = CreativeGates.get();
			this.keyGateId = new NamespacedKey(plugin, "gate_id");
			this.keyFillDisplay = new NamespacedKey(plugin, "fill_display");
		}
	}
	
	/**
	 * Whether END_GATEWAY fills must use {@link Player#sendBlockChange} instead of BlockDisplay.
	 * TODO: Delete when minimum MC version ≥ 26.1 - END_GATEWAY BlockDisplays render correctly then.
	 *
	 * @return True on Minecraft versions before 26.1.
	 */
	public static boolean usesEndGatewayBlockChangeFallback()
	{
		return !ReflectionUtil.isAtLeastMinecraft(26, 1, 0);
	}
	
	/**
	 * Clears previous visuals and spawns the correct look for this gate
	 * (client overlay, BlockDisplays, or ItemDisplays).
	 *
	 * @param gate Gate to refresh; null is ignored.
	 */
	public void syncGate(UGate gate)
	{
		if (gate == null) return;
		this.clearGate(gate);
		
		if (!gate.usesBlockDisplayFill()) return;
		
		GateType type = gate.getFillType();
		if (type == null) return;
		
		List<Block> blocks = gate.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return;
		
		GateOrientation orientation = gate.getOrientation();
		
		// Path 1: fire / soul fire / vertical nether portal — fake blocks via sendBlockChange.
		if (type.usesClientBlockChangeFill(orientation))
		{
			Material overlay = type.getClientDisplayMaterial();
			this.sendClientBlockChanges(blocks, overlay, orientation, true);
			// setContent's real LIGHT packets often arrive after this sendBlockChange and wipe it.
			this.scheduleClientOverlayResyncForGate(gate, overlay, orientation);
			return;
		}
		
		// Path 2: END_GATEWAY on older clients that cannot render it as a BlockDisplay.
		if (type == SupportedGateType.END_GATEWAY && usesEndGatewayBlockChangeFallback())
		{
			this.sendClientBlockChanges(blocks, Material.END_GATEWAY, orientation, true);
			this.scheduleClientOverlayResyncForGate(gate, Material.END_GATEWAY, orientation);
			return;
		}
		
		// Path 3: entity displays (BlockDisplay or ItemDisplay per material).
		Material displayMaterial = type.getClientDisplayMaterial();
		if (displayMaterial == null || !displayMaterial.isBlock()) return;
		
		String gateId = gate.getId();
		if (gateId == null) return;
		
		for (Block block : blocks)
		{
			this.spawnDisplay(block, displayMaterial, orientation, gateId, type);
		}
	}
	
	/**
	 * Removes BlockDisplays / ItemDisplays tagged for this gate and clears any client
	 * block-change overlays on the interior cells.
	 *
	 * @param gate Gate whose visuals to remove; null is ignored.
	 */
	public void clearGate(UGate gate)
	{
		if (gate == null) return;
		
		List<Block> blocks = gate.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return;
		
		String gateId = gate.getId();
		World world = blocks.get(0).getWorld();
		if (world != null && gateId != null)
		{
			Location center = contentCenter(blocks);
			double radius = contentSearchRadius(blocks);
			Collection<Entity> nearby = world.getNearbyEntities(center, radius, radius, radius, entity -> entity instanceof Display);
			for (Entity entity : nearby)
			{
				if (this.isGateDisplay(entity, gateId))
				{
					entity.remove();
				}
			}
		}
		
		// Restore any prior client overlay (fire / soul fire / vertical portal / END_GATEWAY).
		this.sendClientBlockChanges(blocks, null, gate.getOrientation(), false);
	}
	
	/**
	 * Ensures display fills near the player exist (respawn after /kill, chunk issues, etc.).
	 * Client block-change fills are always re-sent to this player - they vanish when the
	 * client reloads chunk data (disconnect/reconnect).
	 *
	 * @param player Player to sync nearby gates for; null is ignored.
	 */
	public void syncPlayer(Player player)
	{
		if (player == null) return;
		World world = player.getWorld();
		Location playerLoc = player.getLocation();
		
		for (UGate gate : UGateColl.get().getAll())
		{
			if (gate == null || !gate.usesBlockDisplayFill()) continue;
			
			List<Block> blocks = gate.getContentBlocks();
			if (blocks == null || blocks.isEmpty()) continue;
			if (blocks.get(0).getWorld() != world) continue;
			if (!isNear(playerLoc, contentCenter(blocks))) continue;
			
			GateType type = gate.getFillType();
			GateOrientation orientation = gate.getOrientation();
			if (type != null && type.usesClientBlockChangeFill(orientation))
			{
				this.sendClientBlockChanges(player, blocks, type.getClientDisplayMaterial(), orientation, true);
				continue;
			}
			if (type == SupportedGateType.END_GATEWAY && usesEndGatewayBlockChangeFallback())
			{
				this.sendClientBlockChanges(player, blocks, Material.END_GATEWAY, orientation, true);
				continue;
			}
			
			// Entity displays: only rebuild the whole gate if the entity count drifted.
			if (this.needsResync(gate, blocks))
			{
				this.syncGate(gate);
			}
		}
	}
	
	/**
	 * Whether a display-entity fill is missing entities (or has extras) relative to content size.
	 * Client-overlay fills never need entity resync.
	 *
	 * @param gate Gate being checked.
	 * @param blocks Interior content blocks for {@code gate}.
	 * @return True if {@link #syncGate(UGate)} should rebuild entity displays.
	 */
	private boolean needsResync(UGate gate, List<Block> blocks)
	{
		GateType type = gate.getFillType();
		if (type != null && type.usesClientBlockChangeFill(gate.getOrientation()))
		{
			return false;
		}
		if (type == SupportedGateType.END_GATEWAY && usesEndGatewayBlockChangeFallback())
		{
			return false;
		}
		
		String gateId = gate.getId();
		if (gateId == null) return true;
		
		World world = blocks.get(0).getWorld();
		if (world == null) return true;
		
		int expected = blocks.size();
		Location center = contentCenter(blocks);
		double radius = contentSearchRadius(blocks);
		int found = 0;
		for (Entity entity : world.getNearbyEntities(center, radius, radius, radius, e -> e instanceof Display))
		{
			if (this.isGateDisplay(entity, gateId)) found++;
		}
		return found != expected;
	}
	
	/**
	 * Geometric center of the content AABB (block centers), used for proximity and entity search.
	 *
	 * @param blocks Non-empty interior blocks.
	 * @return Center location in the blocks' world.
	 */
	private static Location contentCenter(List<Block> blocks)
	{
		Block first = blocks.get(0);
		int minX = first.getX();
		int minY = first.getY();
		int minZ = first.getZ();
		int maxX = minX;
		int maxY = minY;
		int maxZ = minZ;
		for (Block block : blocks)
		{
			int x = block.getX();
			int y = block.getY();
			int z = block.getZ();
			if (x < minX) minX = x;
			if (y < minY) minY = y;
			if (z < minZ) minZ = z;
			if (x > maxX) maxX = x;
			if (y > maxY) maxY = y;
			if (z > maxZ) maxZ = z;
		}
		return new Location(first.getWorld(), (minX + maxX) * 0.5 + 0.5, (minY + maxY) * 0.5 + 0.5, (minZ + maxZ) * 0.5 + 0.5);
	}
	
	/**
	 * Search radius large enough to find all gate display entities around the content AABB.
	 *
	 * @param blocks Non-empty interior blocks.
	 * @return Radius in blocks (at least 16).
	 */
	private static double contentSearchRadius(List<Block> blocks)
	{
		Block first = blocks.get(0);
		int minX = first.getX();
		int minY = first.getY();
		int minZ = first.getZ();
		int maxX = minX;
		int maxY = minY;
		int maxZ = minZ;
		for (Block block : blocks)
		{
			int x = block.getX();
			int y = block.getY();
			int z = block.getZ();
			if (x < minX) minX = x;
			if (y < minY) minY = y;
			if (z < minZ) minZ = z;
			if (x > maxX) maxX = x;
			if (y > maxY) maxY = y;
			if (z > maxZ) maxZ = z;
		}
		double halfDiag = Math.sqrt(
			(maxX - minX + 1) * (maxX - minX + 1)
				+ (maxY - minY + 1) * (maxY - minY + 1)
				+ (maxZ - minZ + 1) * (maxZ - minZ + 1)
		) * 0.5;
		return Math.max(16.0, halfDiag + 8.0);
	}
	
	/**
	 * Spawns one cell's fill visual at the block center (ItemDisplay or BlockDisplay).
	 *
	 * @param block Content cell to cover.
	 * @param material Client look material.
	 * @param orientation Gate plane orientation.
	 * @param gateId Persistent gate id stored on the entity.
	 * @param type Fill type (used for nether-portal axis / transform quirks).
	 */
	private void spawnDisplay(Block block, Material material, GateOrientation orientation, String gateId, GateType type)
	{
		World world = block.getWorld();
		if (world == null) return;
		
		Location loc = block.getLocation().add(0.5, 0.5, 0.5);
		
		if (GateFillDisplayUtil.usesItemDisplay(material))
		{
			Transformation transformation = createItemTransformation(orientation);
			this.applyItemDisplay(world, loc, material, transformation, gateId);
			return;
		}
		
		boolean netherPortal = type == SupportedGateType.NETHER_PORTAL || material == Material.NETHER_PORTAL;
		BlockData blockData = createDisplayBlockData(material, orientation, netherPortal);
		Transformation transformation = createTransformation(orientation, netherPortal);
		this.applyBlockDisplay(world, loc, blockData, transformation, gateId);
	}
	
	/**
	 * Spawns a {@link BlockDisplay} for one interior cell.
	 *
	 * @param world World to spawn in.
	 * @param loc Entity location (typically block center).
	 * @param blockData Block state rendered by the display.
	 * @param transformation Scale / rotation / translation for the gate plane.
	 * @param gateId Persistent gate id.
	 */
	private void applyBlockDisplay(World world, Location loc, BlockData blockData, Transformation transformation, String gateId)
	{
		world.spawn(loc, BlockDisplay.class, entity ->
		{
			entity.setBlock(blockData);
			this.applyCommonDisplay(entity, transformation, gateId);
		});
	}
	
	/**
	 * Spawns an {@link ItemDisplay} for one interior cell using the material's item model.
	 *
	 * @param world World to spawn in.
	 * @param loc Entity location (typically block center).
	 * @param material Item / block material for the stack.
	 * @param transformation Rotation for the gate plane (scale usually identity).
	 * @param gateId Persistent gate id.
	 */
	private void applyItemDisplay(World world, Location loc, Material material, Transformation transformation, String gateId)
	{
		ItemStack stack = new ItemStack(material);
		world.spawn(loc, ItemDisplay.class, entity ->
		{
			entity.setItemStack(stack);
			entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
			entity.setBillboard(Display.Billboard.FIXED);
			this.applyCommonDisplay(entity, transformation, gateId);
		});
	}
	
	/**
	 * Shared Display entity setup: transform, no shadow, wide culling box, persistence, PDC tags.
	 *
	 * @param entity BlockDisplay or ItemDisplay to configure.
	 * @param transformation Local transform applied to the model.
	 * @param gateId Persistent gate id written to PDC.
	 */
	private void applyCommonDisplay(Display entity, Transformation transformation, String gateId)
	{
		entity.setTransformation(transformation);
		entity.setBrightness(null);
		entity.setShadowRadius(0f);
		entity.setShadowStrength(0f);
		entity.setViewRange(1.0f);
		// width/height 0 disables frustum culling (helps nether-portal BlockDisplay freezes).
		entity.setDisplayWidth(0f);
		entity.setDisplayHeight(0f);
		entity.setTeleportDuration(0);
		entity.setPersistent(true);
		entity.getPersistentDataContainer().set(this.keyGateId, PersistentDataType.STRING, gateId);
		entity.getPersistentDataContainer().set(this.keyFillDisplay, PersistentDataType.BYTE, (byte) 1);
	}
	
	/**
	 * Builds block data for a BlockDisplay, including nether-portal axis for the gate plane.
	 *
	 * @param material Display material.
	 * @param orientation Gate orientation; may be null (treated as vertical NS).
	 * @param netherPortal True when the material/type is a nether portal fill.
	 * @return Block data to set on the BlockDisplay.
	 */
	private static BlockData createDisplayBlockData(Material material, GateOrientation orientation, boolean netherPortal)
	{
		BlockData data = material.createBlockData();
		if (netherPortal && data instanceof Orientable orientable)
		{
			if (orientation == GateOrientation.WE)
			{
				orientable.setAxis(Axis.X);
			}
			else
			{
				orientable.setAxis(orientation != null && orientation.isHorizontal() ? Axis.X : Axis.Z);
			}
			return orientable;
		}
		return data;
	}
	
	/**
	 * Local transform for BlockDisplay fills at the cell center.
	 * Nether portal horizontal uses a 90° X rotation; other fills use a thin pane scale on the
	 * gate normal (block models are corner-origin, hence the -0.5 translations).
	 *
	 * @param orientation Gate plane orientation; null treated as vertical NS.
	 * @param netherPortal True for nether-portal BlockDisplays (full cube, optional flat rotate).
	 * @return Transformation applied to the BlockDisplay.
	 */
	private static Transformation createTransformation(GateOrientation orientation, boolean netherPortal)
	{
		AxisAngle4f noRotation = new AxisAngle4f(0f, 0f, 1f, 0f);
		
		if (netherPortal)
		{
			if (orientation != null && orientation.isHorizontal())
			{
				// Lay the portal model flat on the floor/ceiling plane.
				return new Transformation(
					new Vector3f(-0.5f, 0.5f, -0.5f),
					new AxisAngle4f((float) (Math.PI / 2.0), 1f, 0f, 0f),
					new Vector3f(1f, 1f, 1f),
					noRotation
				);
			}
			return new Transformation(
				new Vector3f(-0.5f, -0.5f, -0.5f),
				noRotation,
				new Vector3f(1f, 1f, 1f),
				noRotation
			);
		}
		
		float half = PANE_THICKNESS / 2f;
		if (orientation == GateOrientation.WE)
		{
			// Thin along Z — plane faces north/south.
			return new Transformation(
				new Vector3f(-0.5f, -0.5f, -half),
				noRotation,
				new Vector3f(1f, 1f, PANE_THICKNESS),
				noRotation
			);
		}
		if (orientation != null && orientation.isHorizontal())
		{
			// Thin along Y — floor/ceiling pane.
			return new Transformation(
				new Vector3f(-0.5f, -half, -0.5f),
				noRotation,
				new Vector3f(1f, PANE_THICKNESS, 1f),
				noRotation
			);
		}
		// NS vertical: thin along X — plane faces east/west.
		return new Transformation(
			new Vector3f(-half, -0.5f, -0.5f),
			noRotation,
			new Vector3f(PANE_THICKNESS, 1f, 1f),
			noRotation
		);
	}
	
	/**
	 * Local transform for ItemDisplay fills. Item models are already flat cards centered on the
	 * entity; scale stays identity. Vertical NS gets a 90° Y rotation so the card faces the gate
	 * plane (WE uses the FIXED default facing). Horizontal keeps a 90° X lay-flat rotation.
	 *
	 * @param orientation Gate plane orientation; null treated as vertical NS.
	 * @return Transformation applied to the ItemDisplay.
	 */
	private static Transformation createItemTransformation(GateOrientation orientation)
	{
		AxisAngle4f noRotation = new AxisAngle4f(0f, 0f, 1f, 0f);
		Vector3f translation = new Vector3f(0f, 0f, 0f);
		Vector3f scale = new Vector3f(1f, 1f, 1f);
		
		if (orientation == GateOrientation.WE)
		{
			// FIXED default faces along Z — correct for WE vertical gates.
			return new Transformation(translation, noRotation, scale, noRotation);
		}
		if (orientation != null && orientation.isHorizontal())
		{
			return new Transformation(
				translation,
				new AxisAngle4f((float) (Math.PI / 2.0), 1f, 0f, 0f),
				scale,
				noRotation
			);
		}
		// NS vertical: rotate 90° around Y so the card faces east/west with the gate plane.
		return new Transformation(
			translation,
			new AxisAngle4f((float) (Math.PI / 2.0), 0f, 1f, 0f),
			scale,
			noRotation
		);
	}
	
	/**
	 * Whether an entity is a fill display owned by the given gate (PDC marker + gate id).
	 *
	 * @param entity Candidate entity.
	 * @param gateId Expected gate id.
	 * @return True if {@code entity} is a tagged Display for {@code gateId}.
	 */
	private boolean isGateDisplay(Entity entity, String gateId)
	{
		if (!(entity instanceof Display)) return false;
		Byte marker = entity.getPersistentDataContainer().get(this.keyFillDisplay, PersistentDataType.BYTE);
		if (marker == null || marker != 1) return false;
		String id = entity.getPersistentDataContainer().get(this.keyGateId, PersistentDataType.STRING);
		return gateId.equals(id);
	}
	
	/**
	 * Sends or clears a client-only block overlay on every interior cell for nearby players.
	 * Used for fire / soul fire / vertical nether portal and END_GATEWAY on MC &lt; 26.1.
	 *
	 * @param blocks Interior cells.
	 * @param displayMaterial Overlay material when {@code show} is true; ignored when clearing.
	 * @param orientation Gate orientation for portal axis.
	 * @param show True to apply overlay; false to restore real server block data.
	 */
	private void sendClientBlockChanges(List<Block> blocks, Material displayMaterial, GateOrientation orientation, boolean show)
	{
		if (blocks == null || blocks.isEmpty()) return;
		World world = blocks.get(0).getWorld();
		if (world == null) return;
		
		Location gateLoc = contentCenter(blocks);
		for (Player player : world.getPlayers())
		{
			if (!isNear(player.getLocation(), gateLoc)) continue;
			this.sendClientBlockChanges(player, blocks, displayMaterial, orientation, show);
		}
	}
	
	/**
	 * Sends or clears a client-only block overlay for one player on every interior cell.
	 *
	 * @param player Recipient of the fake blocks.
	 * @param blocks Interior cells.
	 * @param displayMaterial Overlay material when {@code show} is true; ignored when clearing.
	 * @param orientation Gate orientation for portal axis.
	 * @param show True to apply overlay; false to restore real server block data.
	 */
	private void sendClientBlockChanges(Player player, List<Block> blocks, Material displayMaterial, GateOrientation orientation, boolean show)
	{
		if (player == null || blocks == null || blocks.isEmpty()) return;
		
		BlockData overlay = null;
		if (show)
		{
			overlay = createClientOverlayBlockData(displayMaterial, orientation);
			if (overlay == null) return;
		}
		
		for (Block block : blocks)
		{
			if (overlay != null)
			{
				player.sendBlockChange(block.getLocation(), overlay);
			}
			else
			{
				player.sendBlockChange(block.getLocation(), block.getBlockData());
			}
		}
	}
	
	/**
	 * Builds client overlay block data. Nether portal gets the gate's NS/WE axis so the plane
	 * faces correctly.
	 *
	 * @param displayMaterial Overlay material.
	 * @param orientation Gate orientation; may be null.
	 * @return Block data to send, or null if {@code displayMaterial} is invalid.
	 */
	private static BlockData createClientOverlayBlockData(Material displayMaterial, GateOrientation orientation)
	{
		if (displayMaterial == null || !displayMaterial.isBlock()) return null;
		BlockData data = displayMaterial.createBlockData();
		if (displayMaterial == Material.NETHER_PORTAL && data instanceof Orientable orientable)
		{
			if (orientation == GateOrientation.WE)
			{
				orientable.setAxis(Axis.X);
			}
			else
			{
				// NS vertical (and null): faces east/west.
				orientable.setAxis(Axis.Z);
			}
			return orientable;
		}
		return data;
	}
	
	/**
	 * Re-sends client overlays for nearby overlay-style gates to one player.
	 * Used after join/teleport when chunk packets may have overwritten sendBlockChange.
	 *
	 * @param player Player to refresh overlays for.
	 */
	private void syncClientOverlaysForPlayer(Player player)
	{
		if (player == null || !player.isOnline()) return;
		World world = player.getWorld();
		Location playerLoc = player.getLocation();
		
		for (UGate gate : UGateColl.get().getAll())
		{
			if (gate == null || !gate.usesBlockDisplayFill()) continue;
			Material overlayMaterial = this.resolveClientOverlayMaterial(gate);
			if (overlayMaterial == null) continue;
			
			List<Block> blocks = gate.getContentBlocks();
			if (blocks == null || blocks.isEmpty()) continue;
			if (blocks.get(0).getWorld() != world) continue;
			if (!isNear(playerLoc, contentCenter(blocks))) continue;
			
			this.sendClientBlockChanges(player, blocks, overlayMaterial, gate.getOrientation(), true);
		}
	}
	
	/**
	 * Material used for this gate's {@code sendBlockChange} interior overlay, or null if the gate
	 * uses entity displays (or has no fill).
	 *
	 * @param gate Gate to inspect.
	 * @return Overlay material, or null when none.
	 */
	private Material resolveClientOverlayMaterial(UGate gate)
	{
		if (gate == null) return null;
		GateType type = gate.getFillType();
		if (type == null) return null;
		
		GateOrientation orientation = gate.getOrientation();
		if (type.usesClientBlockChangeFill(orientation))
		{
			Material material = type.getClientDisplayMaterial();
			return material != null && material.isBlock() ? material : null;
		}
		if (type == SupportedGateType.END_GATEWAY && usesEndGatewayBlockChangeFallback())
		{
			return Material.END_GATEWAY;
		}
		return null;
	}
	
	/**
	 * Re-applies a gate's client overlay for one player (no-op if the gate has none or is gone).
	 *
	 * @param player Player to refresh.
	 * @param gate Gate whose overlay to re-send.
	 */
	private void refreshClientOverlay(Player player, UGate gate)
	{
		if (player == null || !player.isOnline() || gate == null || !gate.attached()) return;
		Material overlay = this.resolveClientOverlayMaterial(gate);
		if (overlay == null) return;
		
		List<Block> blocks = gate.getContentBlocks();
		if (blocks == null || blocks.isEmpty()) return;
		this.sendClientBlockChanges(player, blocks, overlay, gate.getOrientation(), true);
	}
	
	/**
	 * Client prediction after right-clicking a fake portal/fire block reverts {@code sendBlockChange}
	 * to the real server LIGHT block. Re-send the overlay immediately and on the next ticks.
	 * <p>
	 * Left-click is intentionally ignored: it starts block breaking, and a delayed overlay
	 * refresh would fight gate destroy (nether portal fills looked unbreakable).
	 * </p>
	 *
	 * @param event Interact event (monitor; may already be cancelled).
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void onInteractClientOverlay(PlayerInteractEvent event)
	{
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		
		Block block = event.getClickedBlock();
		if (block == null) return;
		
		UGate gate = UGate.get(block);
		if (gate == null) return;
		if (this.resolveClientOverlayMaterial(gate) == null) return;
		
		Player player = event.getPlayer();
		this.refreshClientOverlay(player, gate);
		
		CreativeGates plugin = CreativeGates.get();
		Bukkit.getScheduler().runTask(plugin, () -> this.refreshClientOverlay(player, gate));
		Bukkit.getScheduler().runTaskLater(plugin, () -> this.refreshClientOverlay(player, gate), 2L);
	}
	
	/**
	 * Whether the player is within {@link #VIEW_DISTANCE_BLOCKS} of the gate center (same world).
	 *
	 * @param playerLoc Player location.
	 * @param gateLoc Gate content center.
	 * @return True if close enough for overlay / display sync.
	 */
	private static boolean isNear(Location playerLoc, Location gateLoc)
	{
		if (playerLoc == null || gateLoc == null) return false;
		if (playerLoc.getWorld() != gateLoc.getWorld()) return false;
		return playerLoc.distanceSquared(gateLoc) <= (double) VIEW_DISTANCE_BLOCKS * VIEW_DISTANCE_BLOCKS;
	}
	
	/**
	 * Re-sends a gate's client overlay a few ticks later so it wins over real block-update packets
	 * from {@link UGate#fill()} (otherwise nether portal / fire fills stay invisible until move).
	 *
	 * @param gate Gate that just received an overlay.
	 * @param overlayMaterial Material to re-send.
	 * @param orientation Orientation captured at schedule time; falls back to live gate if null.
	 */
	private void scheduleClientOverlayResyncForGate(UGate gate, Material overlayMaterial, GateOrientation orientation)
	{
		if (gate == null || overlayMaterial == null) return;
		CreativeGates plugin = CreativeGates.get();
		String gateId = gate.getId();
		for (long delay : new long[] { 1L, 5L, 10L })
		{
			Bukkit.getScheduler().runTaskLater(plugin, () ->
			{
				UGate live = gateId == null ? null : UGateColl.get().get(gateId);
				if (live == null || !live.attached()) return;
				if (this.resolveClientOverlayMaterial(live) == null) return;
				
				List<Block> blocks = live.getContentBlocks();
				if (blocks == null || blocks.isEmpty()) return;
				this.sendClientBlockChanges(blocks, overlayMaterial, orientation != null ? orientation : live.getOrientation(), true);
			}, delay);
		}
	}
	
	/**
	 * Client overlays are wiped when chunk data arrives after join. Spigot has no per-player
	 * chunk-receive event, so re-send several times while terrain finishes loading.
	 *
	 * @param player Player who just joined / changed world / respawned.
	 */
	private void scheduleClientOverlayResync(Player player)
	{
		if (player == null) return;
		CreativeGates plugin = CreativeGates.get();
		for (long delay : new long[] { 5L, 20L, 40L, 80L, 120L })
		{
			Bukkit.getScheduler().runTaskLater(plugin, () ->
			{
				if (!player.isOnline()) return;
				this.syncPlayer(player);
				this.syncClientOverlaysForPlayer(player);
			}, delay);
		}
	}
	
	/**
	 * After join, schedule staggered overlay / display sync while chunks load.
	 *
	 * @param event Join event.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event)
	{
		this.scheduleClientOverlayResync(event.getPlayer());
	}
	
	/**
	 * After world change, schedule staggered overlay / display sync in the destination world.
	 *
	 * @param event World-change event.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldChange(PlayerChangedWorldEvent event)
	{
		this.scheduleClientOverlayResync(event.getPlayer());
	}
	
	/**
	 * Shortly after teleport, re-sync nearby entity displays and client overlays.
	 *
	 * @param event Teleport event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event)
	{
		Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(CreativeGates.get(), () ->
		{
			if (!player.isOnline()) return;
			this.syncPlayer(player);
			this.syncClientOverlaysForPlayer(player);
		}, 5L);
	}
	
	/**
	 * After respawn, schedule staggered overlay / display sync near the respawn point.
	 *
	 * @param event Respawn event.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onRespawn(PlayerRespawnEvent event)
	{
		this.scheduleClientOverlayResync(event.getPlayer());
	}
	
	/**
	 * On block-to-block movement, periodically sync nearby display fills (throttled).
	 *
	 * @param event Move event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onMove(PlayerMoveEvent event)
	{
		Location from = event.getFrom();
		Location to = event.getTo();
		if (to == null) return;
		if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
		
		Player player = event.getPlayer();
		long now = System.currentTimeMillis();
		Long last = this.lastMoveSyncMillis.get(player.getUniqueId());
		if (last != null && now - last < MOVE_SYNC_INTERVAL_MS) return;
		this.lastMoveSyncMillis.put(player.getUniqueId(), now);
		
		this.syncPlayer(player);
	}
	
	/**
	 * Drops move-sync throttle state when a player disconnects.
	 *
	 * @param event Quit event.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event)
	{
		this.lastMoveSyncMillis.remove(event.getPlayer().getUniqueId());
	}
}
