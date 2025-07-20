package com.massivecraft.factions.engine;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.factions.entity.Board;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.util.EnumerationUtil;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EnginePermBuild extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EnginePermBuild i = new EnginePermBuild();
	public static EnginePermBuild get() { return i; }

	// Map to track last location messaged per player - only used by some events
	// This is used to prevent spam messages from certain events
	private static final Map<UUID, Location> lastMessagedLoc = new HashMap<>();

	// -------------------------------------------- //
	// LOGIC > PROTECT
	// -------------------------------------------- //
	
	public static Boolean isProtected(ProtectCase protectCase, boolean verboose, MPlayer mplayer, PS ps, Object object)
	{
		if (mplayer == null) return null;
		if (protectCase == null) return null;
		String name = mplayer.getName();
		if (MConf.get().playersWhoBypassAllProtection.contains(name)) return false;
		if (mplayer.isOverriding()) return false;
		
		MPerm perm = protectCase.getPerm(object);
		if (perm == null) return null;
		if (protectCase != ProtectCase.BUILD) return !perm.has(mplayer, ps, verboose);
		
		if (!perm.has(mplayer, ps, false) && MPerm.getPermPainbuild().has(mplayer, ps, false))
		{
			if (!verboose) return false;
			
			Faction hostFaction = BoardColl.get().getFactionAt(ps);
			mplayer.msg("<b>It is painful to build in the territory of %s<b>.", hostFaction.describeTo(mplayer));
			Player player = mplayer.getPlayer();
			if (player != null) player.damage(MConf.get().actionDeniedPainAmount);
		}
		
		return !perm.has(mplayer, ps, verboose);
	}
	
	public static Boolean protect(ProtectCase protectCase, boolean verboose, Player player, PS ps, Object object, Cancellable cancellable)
	{
		Boolean ret = isProtected(protectCase, verboose, MPlayer.get(player), ps, object);
		if (Boolean.TRUE.equals(ret) && cancellable != null) 
		{
			cancellable.setCancelled(true);
		}

		return ret;
	}
	
	public static Boolean build(Entity entity, Block block, Event event)
	{
		return build(entity, block, !isFake(event), event);
	}

	public static Boolean build(Entity entity, Block block, boolean verboose, Event event)
	{
		if (!(event instanceof Cancellable)) return true;

		// If the entity is a projectile, we need to check its shooter
		if (entity instanceof Arrow && ((Arrow) entity).getShooter() instanceof Entity) 
		{
			entity = ((Entity) ((Arrow) entity).getShooter());
		}

		// If the entity is not a player, we need to check any passengers if applicable
		if (entity != null && MUtil.isntPlayer(entity))
		{
			List<Entity> passengers = entity.getPassengers();
			if (!passengers.isEmpty())
			{
				for (Entity passenger : passengers)
				{
					if (MUtil.isPlayer(passenger))
					{
						entity = passenger;
						break;
					}
				}
			}
			else return false;
		}

		// If the entity is still not a player return false
		if (entity == null || MUtil.isntPlayer(entity)) return false;
		
		Player player = (Player) entity;
		return protect(ProtectCase.BUILD, verboose, player, PS.valueOf(block), block, (Cancellable) event);
	}
	
	public static Boolean useItem(Player player, Block block, Material material, Cancellable cancellable)
	{
		return protect(ProtectCase.USE_ITEM, true, player, PS.valueOf(block), material, cancellable);
	}
	
	public static Boolean useEntity(Player player, Entity entity, boolean verboose, Cancellable cancellable)
	{
		return protect(ProtectCase.USE_ENTITY, verboose, player, PS.valueOf(entity), entity, cancellable);
	}
	
	public static Boolean useBlock(Player player, Block block, boolean verboose, Cancellable cancellable)
	{
		return protect(ProtectCase.USE_BLOCK, verboose, player, PS.valueOf(block), block.getType(), cancellable);
	}

	public static Boolean useRedstoneBlock(Player player, Block block, Material material, boolean verboose)
	{
		return protect(ProtectCase.USE_REDSTONE_BLOCK, verboose, player, PS.valueOf(block), material, null);
	}

	public static Boolean useLeash(Player player, Block block, Material material, Cancellable cancellable)
	{
		return protect(ProtectCase.LEASH_MOB, true, player, PS.valueOf(block), material, cancellable);
	}

	public static Boolean useLeashEntity(Player player, Entity entity, Cancellable cancellable)
	{
		return protect(ProtectCase.LEASH_MOB, true, player, PS.valueOf(entity.getLocation()), entity, cancellable);
	}

	public static Boolean leashMob(Player player, Entity entity, Cancellable cancellable)
	{
		return protect(ProtectCase.LEASH_MOB, true, player, PS.valueOf(entity.getLocation()), entity, cancellable);
	}
	
	// -------------------------------------------- //
	// LOGIC > PROTECT > BUILD
	// -------------------------------------------- //
	
	public static boolean canPlayerBuildAt(Object senderObject, PS ps, boolean verboose)
	{
		MPlayer mplayer = MPlayer.get(senderObject);
		if (mplayer == null) return false;
		
		Boolean ret = isProtected(ProtectCase.BUILD, verboose, mplayer, ps, null);
		return !Boolean.TRUE.equals(ret);
	}
	
	// -------------------------------------------- //
	// BUILD > BLOCK
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void build(BlockPlaceEvent event) { build(event.getPlayer(), event.getBlock(), event); }

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void build(BlockBreakEvent event) { build(event.getPlayer(), event.getBlock(), event); }

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void build(SignChangeEvent event) { build(event.getPlayer(), event.getBlock(), event); }
	
	// Handles placing entity items such as item frames, paintings, leashes
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void build(HangingPlaceEvent event) 
	{ 
		build(event.getPlayer(), event.getBlock(), true, event);
	}
	
	// Handles breaking entity items such as item frames, paintings, leashes
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void build(HangingBreakByEntityEvent event) { build(event.getRemover(), event.getEntity().getLocation().getBlock(), event); }

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void build(EntityChangeBlockEvent event)
	{
		// Handling lilypads being broken by boats
		Entity entity = event.getEntity();
		if (!EnumerationUtil.isEntityBoat(entity.getType()) || entity.getPassengers().size() <= 0) return;
		Entity player = entity.getPassengers().stream().filter(MUtil::isPlayer).findAny().orElse(entity);

		build(player, event.getBlock(), event);
	}

	// -------------------------------------------- //
	// USE > ITEM
	// -------------------------------------------- //

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void useBlockItem(PlayerInteractEvent event)
	{
		// If the player right clicks (or is physical with) a block ...
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) return;

		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		if (block == null) return;

		// ... and we are either allowed to use this block ...
		Boolean ret = useBlock(player, block, true, event);
		if (Boolean.TRUE.equals(ret)) return;
		
		// ... or are allowed to right click with the item, this event is safe to perform.
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		// Check if we're dealing with a lead/fence attachment or just regular item use
		Material material = event.getMaterial();
		boolean blockIsFence = block.getType().toString().endsWith("_FENCE");
		if (blockIsFence)
		{
			boolean playerHasLeashedEntities = player.getWorld().getNearbyEntities(player.getLocation(), 20, 20, 20).stream()
				.filter(e -> e instanceof LivingEntity)
				.map(e -> (LivingEntity) e)
				.anyMatch(e -> e.isLeashed() && player.equals(e.getLeashHolder()));

			// Make sure we don't run this twice (for both hands)
			if (playerHasLeashedEntities && event.getHand() != EquipmentSlot.OFF_HAND)
			{
				useLeash(player, block, material, event);
			}
			else
			{
				// If the player does not have leashed entities, we can use the item normally
				useItem(player, block, material, event);
			}
		}
		else
		{
			useItem(player, block, material, event);
		}
	}

	// For some reason onPlayerInteract() sometimes misses bucket events depending on distance
	// (something like 2-3 blocks away isn't detected), but these separate bucket events below always fire without fail

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void useItem(PlayerBucketEmptyEvent event) { useItem(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace()), event.getBucket(), event); }
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void useItem(PlayerBucketFillEvent event) { useItem(event.getPlayer(), event.getBlockClicked(), event.getBucket(), event); }

	// -------------------------------------------- //
	// USE > REDSTONE BLOCK
	// -------------------------------------------- //

	// Prevent certain redstone blocks from being activated by players
	@EventHandler
	public void onBlockRedstoneChange(BlockRedstoneEvent event)
	{
		Block block = event.getBlock();
		Material blockMaterial = block.getType();

		// Only care about pressure plates (for now)
		if (!EnumerationUtil.isMaterialPressurePlate(blockMaterial)) return;

		// Only check entities within a small radius (should cover standing on the plate)
		Location blockLocation = block.getLocation();
		for (Entity entity : block.getWorld().getNearbyEntities(blockLocation, 1, 1, 1))
		{
			// If the entity is not a player, skip it
			if (entity == null || MUtil.isntPlayer(entity)) continue;

			Player player = (Player) entity;
			UUID uuid = player.getUniqueId();
			Location last = lastMessagedLoc.get(uuid);
			boolean verboose = (last == null || !last.equals(blockLocation));

			Boolean prevent = useRedstoneBlock(player, block, blockMaterial, verboose);

			if (Boolean.TRUE.equals(prevent))
			{
				event.setNewCurrent(0);
				if (verboose)
				{
					lastMessagedLoc.put(uuid, blockLocation);
				}
			}
		}
	}

	// -------------------------------------------- //
	// ENTITY > LEASH
	// -------------------------------------------- //

	// These event handlers primarily have been added to handle changes to leads in Minecraft 1.21.6
	// Prevent players from attaching leashes to entities
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityLeash(PlayerLeashEntityEvent event)
	{
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		leashMob(player, event.getEntity(), event);
	}

	// Prevent players from unleashing entities
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityUnleash(PlayerUnleashEntityEvent event)
	{
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		leashMob(player, event.getEntity(), event);
	}

	// -------------------------------------------- //
	// USE > ENTITY
	// -------------------------------------------- //
	
	// This event will not fire for Minecraft 1.8 armor stands.
	// Armor stands are handled in EngineSpigot instead.
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void useEntity(PlayerInteractEntityEvent event)
	{
		// Ignore Off Hand
		if (isOffHand(event)) return;

		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		if (entity.getType() == EntityType.LEASH_KNOT)
		{
			useLeashEntity(player, entity, event);
		}
		else
		{
			useEntity(player, entity, true, event);
		}
	}

	// This is a special Spigot event that fires for Minecraft 1.8 armor stands.
	// It also fires for other entity types but for those the event is buggy.
	// It seems we can only cancel interaction with armor stands from here.
	// Thus we only handle armor stands from here and handle everything else in EngineMain.
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void handleArmorStand(PlayerInteractAtEntityEvent event)
	{
		// Ignore Off Hand
		if (isOffHand(event)) return;

		// Gather Info
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		final Entity entity = event.getRightClicked();
		final boolean verboose = true;

		// Only care for armor stands.
		if (entity.getType() != EntityType.ARMOR_STAND) return;

		// If we can't use, block it
		EnginePermBuild.useEntity(player, entity, verboose, event);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void handleLectern(PlayerTakeLecternBookEvent event)
	{
		// If the player tries to take from a lectern ...
		
		// Gather Info
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		final MPlayer mplayer = MPlayer.get(player);
		final Block block = event.getLectern().getBlock();
		final boolean verbose = true;
		
		// ... and they're not bypassing/overriding ...
		if (MConf.get().playersWhoBypassAllProtection.contains(mplayer.getName())) return;
		if (mplayer.isOverriding()) return;
		
		// ... check if they can use containers
		if (Boolean.TRUE.equals(!MPerm.getPermContainer().has(mplayer, PS.valueOf(block), verbose))) event.setCancelled(true);
	}

	// -------------------------------------------- //
	// BUILD > ENTITY
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void buildEntity(EntityDamageByEntityEvent event)
	{
		// If a player ...
		Entity damager = MUtil.getLiableDamager(event);
		if (MUtil.isntPlayer(damager)) return;
		Player player = (Player)damager;
		
		// ... damages an entity which is edited on damage ...
		Entity entity = event.getEntity();
		if (entity == null || !EnumerationUtil.isEntityTypeEditOnDamage(entity.getType())) return;
		
		// ... and the player can't build there, cancel the event
		build(player, entity.getLocation().getBlock(), event);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void combustEntity(EntityCombustByEntityEvent event)
	{	
		// If a burning projectile ...
		if (!(event.getCombuster() instanceof Projectile)) return;
		Projectile entityProjectile = (Projectile)event.getCombuster();
		
		// ... fired by a player ...
		ProjectileSource projectileSource = entityProjectile.getShooter();
		if (MUtil.isntPlayer(projectileSource)) return;
		Player player = (Player) projectileSource;
		
		// ... and hits an entity which is edited on damage (and thus likely to burn) ...
		Entity entityTarget = event.getEntity();
		if (entityTarget == null || !EnumerationUtil.isEntityTypeEditOnDamage(entityTarget.getType())) return;

		// ... and the player can't build there, cancel the event
		Block block = entityTarget.getLocation().getBlock();
		protect(ProtectCase.BUILD, false, player, PS.valueOf(block), block, event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void vehicleDestroy(VehicleDestroyEvent event)
	{
		// If a player destroys a vehicle...
		if (MUtil.isntPlayer(event.getAttacker())) return;
		Player player = (Player) event.getAttacker();
		
		// then check for build permissions.
		build(player, player.getLocation().getBlock(), event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void frostWalker(EntityBlockFormEvent event)
	{
		// If a player forms a block (I.e. FrostWalker) ...
		Entity entity = event.getEntity();
		if (MUtil.isntPlayer(entity)) return;
		Player player = (Player) entity;
		
		// ... and the player can't build there, cancel the event
		protect(ProtectCase.BUILD, false, player, PS.valueOf(event.getBlock()), event.getBlock(), event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void containerBreak(BlockBreakEvent event)
	{
		Block block  = event.getBlock();
		
		// If a block is a container...
		Container container = null;
		if (block.getState() instanceof Container)
		{
			container = (Container) block.getState();
		} 
		else
		{
			return;
		}
		
		// ...and the entity breaking the block is a player...
		if (MUtil.isntPlayer(event.getPlayer())) return;
		MPlayer mPlayer = MPlayer.get(event.getPlayer());
		
		Inventory inventory = container.getInventory();
		
		// ...and the inventory of the container isn't empty...
		if (inventory.isEmpty()) return;
		
		if (MConf.get().playersWhoBypassAllProtection.contains(mPlayer.getName())) return;
		if (mPlayer.isOverriding()) return;
		
		// ...check if they have container permissions.
		if (Boolean.TRUE.equals(!MPerm.getPermContainer().has(mPlayer, PS.valueOf(block), true))) event.setCancelled(true);
	}
	
	// -------------------------------------------- //
	// BUILD > PISTON
	// -------------------------------------------- //
	
	/*
	* Note: With 1.8 and the slime blocks, retracting and extending pistons
	* became more of a problem. Blocks located on the border of a chunk
	* could have easily been stolen. That is the reason why every block
	* needs to be checked now, whether he moved into a territory which
	* he actually may not move into.
	*/
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockBuild(BlockPistonExtendEvent event)
	{
		// Is checking deactivated by MConf?
		if ( ! MConf.get().handlePistonProtectionThroughDenyBuild) return;

		Faction pistonFaction = BoardColl.get().getFactionAt(PS.valueOf(event.getBlock()));

		List<Block> blocks = event.getBlocks();

		// Check for all extended blocks
		for (Block block : blocks)
		{
			// Block which is being pushed into
			Block targetBlock = block.getRelative(event.getDirection());

			// Members of a faction might not have build rights in their own territory, but pistons should still work regardless
			Faction targetFaction = BoardColl.get().getFactionAt(PS.valueOf(targetBlock));
			if (targetFaction == pistonFaction) continue;

			// Perm check
			if (MPerm.getPermBuild().has(pistonFaction, targetFaction)) continue;

			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockBuild(BlockPistonRetractEvent event)
	{
		// Is checking deactivated by MConf?
		if ( ! MConf.get().handlePistonProtectionThroughDenyBuild) return;

		Faction pistonFaction = BoardColl.get().getFactionAt(PS.valueOf(event.getBlock()));

		List<Block> blocks = event.getBlocks();

		// Check for all retracted blocks
		for (Block block : blocks)
		{
			// Is the retracted block air/water/lava? Don't worry about it
			if (block.isEmpty() || block.isLiquid()) return;

			// Members of a faction might not have build rights in their own territory, but pistons should still work regardless
			Faction targetFaction = BoardColl.get().getFactionAt(PS.valueOf(block));
			if (targetFaction == pistonFaction) continue;

			// Perm check
			if (MPerm.getPermBuild().has(pistonFaction, targetFaction)) continue;

			event.setCancelled(true);
			return;
		}
	}
	
	// -------------------------------------------- //
	// BUILD > FIRE
	// -------------------------------------------- //
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL)
	public void buildFire(PlayerInteractEvent event)
	{
		// If it is a left click on block and the clicked block is not null...
		if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) return;
		
		// ... and the potential block is not null either ...
		Block potentialBlock = event.getClickedBlock().getRelative(BlockFace.UP, 1);
		if (potentialBlock == null) return;
		
		Material blockType = potentialBlock.getType();
		
		// ... and we're only going to check for fire ... (checking everything else would be bad performance wise)
		if (blockType != Material.FIRE) return;
		
		// ... check if they can't build, cancel the event ...
		if (!Boolean.FALSE.equals(build(event.getPlayer(), potentialBlock, event))) return;
		
		// ... and compensate for client side prediction
		event.getPlayer().sendBlockChange(potentialBlock.getLocation(), blockType, potentialBlock.getState().getRawData());
	}
	
	// -------------------------------------------- //
	// BUILD > MOVE
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void buildMove(BlockFromToEvent event)
	{
		if ( ! MConf.get().protectionLiquidFlowEnabled) return;
		
		// Prepare fields
		Block fromBlock = event.getBlock();
		int chunkFromX = fromBlock.getX() >> 4;
		int chunkFromZ = fromBlock.getZ() >> 4;
		BlockFace face = event.getFace();
		int chunkToX = (fromBlock.getX() + face.getModX()) >> 4;
		int chunkToZ = (fromBlock.getZ() + face.getModZ()) >> 4;
		
		// If a liquid (or dragon egg) moves from one chunk to another ...
		if (chunkToX == chunkFromX && chunkToZ == chunkFromZ) return;
		
		// ... get the correct board for this block ...
		Board board = BoardColl.get().getFixed(fromBlock.getWorld().getName().toLowerCase(), false);
		if (board == null) return;
		
		// ... get the access map ...
		Map<PS, TerritoryAccess> map = board.getMapRaw();
		if (map.isEmpty()) return;
		
		// ... get the faction ids from and to ...
		PS fromPs = PS.valueOf(chunkFromX, chunkFromZ);
		PS toPs = PS.valueOf(chunkToX, chunkToZ);
		TerritoryAccess fromTa = map.get(fromPs);
		TerritoryAccess toTa = map.get(toPs);
		
		// Null checks are needed here since automatic board cleaning can be undesired sometimes
		String fromId = fromTa != null ? fromTa.getHostFactionId() : Factions.ID_NONE;
		String toId = toTa != null ? toTa.getHostFactionId() : Factions.ID_NONE;
		
		// ... and the chunks belong to different factions ...
		if (toId.equals(fromId)) return;
		
		// ... and the faction "from" can not build at "to" ...
		Faction fromFac = FactionColl.get().getFixed(fromId);
		if (fromFac == null) fromFac = FactionColl.get().getNone();
		Faction toFac = FactionColl.get().getFixed(toId);
		if (toFac == null) toFac = FactionColl.get().getNone();
		if (MPerm.getPermBuild().has(fromFac, toFac)) return;
		
		// ... cancel the event!
		event.setCancelled(true);
	}
}
