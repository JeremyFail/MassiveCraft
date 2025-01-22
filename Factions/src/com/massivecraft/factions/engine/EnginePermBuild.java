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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.Map;

public class EnginePermBuild extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EnginePermBuild i = new EnginePermBuild();
	public static EnginePermBuild get() { return i; }

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
		if (Boolean.TRUE.equals(ret) && cancellable != null) cancellable.setCancelled(true);

		return ret;
	}
	
	public static Boolean build(Entity entity, Block block, Event event)
	{
		if (!(event instanceof Cancellable)) return true;
		if (MUtil.isntPlayer(entity)) return false;
		Player player = (Player) entity;
		boolean verboose = !isFake(event);
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
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void build(HangingPlaceEvent event) { build(event.getPlayer(), event.getBlock(), event); }
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void build(HangingBreakByEntityEvent event) { build(event.getRemover(), event.getEntity().getLocation().getBlock(), event); }

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void build(EntityChangeBlockEvent event)
	{
		// Handling lilypads being broken by boats
		Entity entity = event.getEntity();
		if (!isEntityBoat(entity.getType()) || entity.getPassengers().size() <= 0) return;
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
		useItem(player, block, event.getMaterial(), event);
	}

	// For some reason onPlayerInteract() sometimes misses bucket events depending on distance
	// (something like 2-3 blocks away isn't detected), but these separate bucket events below always fire without fail

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void useItem(PlayerBucketEmptyEvent event) { useItem(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace()), event.getBucket(), event); }
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void useItem(PlayerBucketFillEvent event) { useItem(event.getPlayer(), event.getBlockClicked(), event.getBucket(), event); }
	
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
		useEntity(event.getPlayer(), event.getRightClicked(), true, event);
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
	public void combustEntity(EntityCombustByEntityEvent event) {
		
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
	
	private boolean isEntityBoat(EntityType entityType)
	{
		if (entityType == EntityType.ACACIA_BOAT ||
				entityType == EntityType.ACACIA_CHEST_BOAT ||
				entityType == EntityType.BIRCH_BOAT ||
				entityType == EntityType.BIRCH_CHEST_BOAT ||
				entityType == EntityType.CHERRY_BOAT ||
				entityType == EntityType.CHERRY_CHEST_BOAT ||
				entityType == EntityType.DARK_OAK_BOAT ||
				entityType == EntityType.DARK_OAK_CHEST_BOAT || 
				entityType == EntityType.JUNGLE_BOAT || 
				entityType == EntityType.JUNGLE_CHEST_BOAT ||
				entityType == EntityType.MANGROVE_BOAT ||
				entityType == EntityType.MANGROVE_CHEST_BOAT ||
				entityType == EntityType.OAK_BOAT ||
				entityType == EntityType.OAK_CHEST_BOAT ||
				entityType == EntityType.PALE_OAK_BOAT ||
				entityType == EntityType.PALE_OAK_CHEST_BOAT ||
				entityType == EntityType.SPRUCE_BOAT ||
				entityType == EntityType.SPRUCE_CHEST_BOAT)
		{
			return true;
		}
		return false;
	}
	
}
