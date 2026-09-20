package com.massivecraft.creativegates.util;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Teleports living entities through gates while preserving mounts and leads.
 * <p>
 * Mount stacks are teleported as a unit via the root vehicle (no intentional dismount).
 * Leashed mobs stay leashed to living holders; fence hitches are broken when the mob
 * is brought through a gate.
 * </p>
 */
public final class GateEntityTeleport
{
	/** Vanilla lead max length is 10 blocks; search slightly beyond for safety. */
	private static final double LEASH_SEARCH_RADIUS = 12.0;

	private GateEntityTeleport() { }

	/**
	 * Returns whether the player is mounted or has nearby leashed mobs that should come along.
	 * 
	 * @param player The player to check.
	 * @return True if the player should bring their entourage.
	 */
	public static boolean shouldBringEntourage(Player player)
	{
		return hasMobEntourage(player) || hasNonLivingVehicle(player);
	}

	/**
	 * Living mount and/or leashed pets (controlled by mobs config).
	 * 
	 * @param player The player to check.
	 * @return True if the player has a mob entourage.
	 */
	public static boolean hasMobEntourage(Player player)
	{
		if (player == null) return false;
		Entity vehicle = player.getVehicle();
		if (vehicle instanceof LivingEntity) return true;
		if (!player.getPassengers().isEmpty()) return true;
		return !findLeashedTo(player).isEmpty();
	}

	/**
	 * Boat / minecart / other non-living vehicle (controlled by vehicles config).
	 * 
	 * @param player The player to check.
	 * @return True if the player has a non-living vehicle.
	 */
	public static boolean hasNonLivingVehicle(Player player)
	{
		if (player == null) return false;
		Entity vehicle = player.getVehicle();
		return vehicle != null && !(vehicle instanceof LivingEntity);
	}

	/**
	 * Finds living entities currently leashed to {@code holder} within lead range.
	 * 
	 * @param holder The entity to check.
	 * @return A list of living entities currently leashed to the holder.
	 */
	public static List<LivingEntity> findLeashedTo(Entity holder)
	{
		List<LivingEntity> result = new ArrayList<>();
		if (holder == null) return result;
		Location loc = holder.getLocation();
		World world = loc.getWorld();
		if (world == null) return result;

		for (Entity nearby : world.getNearbyEntities(loc, LEASH_SEARCH_RADIUS, LEASH_SEARCH_RADIUS, LEASH_SEARCH_RADIUS))
		{
			if (!(nearby instanceof LivingEntity living)) continue;
			if (!living.isLeashed()) continue;
			if (!holder.equals(living.getLeashHolder())) continue;
			result.add(living);
		}
		return result;
	}

	/**
	 * Collects the travel party for {@code trigger}: vehicle tree plus leash-linked living entities.
	 * 
	 * @param trigger The entity to collect the party for.
	 * @return A set of entities in the travel party.
	 */
	public static Set<Entity> collectParty(Entity trigger)
	{
		Set<Entity> party = new LinkedHashSet<>();
		if (trigger == null || !trigger.isValid()) return party;

		addVehicleTree(getRootVehicle(trigger), party);

		boolean changed = true;
		while (changed)
		{
			changed = false;
			for (Entity member : new ArrayList<>(party))
			{
				if (member instanceof LivingEntity living && living.isLeashed())
				{
					Entity holder = living.getLeashHolder();
					// Living holders (players / mobs) come along. Fence hitches are broken at teleport time.
					if (holder instanceof LivingEntity && holder.isValid() && party.add(holder))
					{
						addVehicleTree(getRootVehicle(holder), party);
						changed = true;
					}
				}
				for (LivingEntity leashed : findLeashedTo(member))
				{
					if (party.add(leashed))
					{
						addVehicleTree(getRootVehicle(leashed), party);
						changed = true;
					}
				}
			}
		}
		return party;
	}

	/**
	 * Finds a player in the travel party rooted at {@code trigger}, if any.
	 * 
	 * @param trigger The entity to collect the party for.
	 * @return The player in the travel party, or null if no player is found.
	 */
	public static Player findPlayerInParty(Entity trigger)
	{
		for (Entity member : collectParty(trigger))
		{
			if (member instanceof Player player && !MUtil.isntPlayer(player)) return player;
		}
		return null;
	}

	/**
	 * Teleports {@code trigger} and its mount / passenger / leash party to {@code destination}.
	 * <p>
	 * Vehicle roots are teleported with passengers left seated. Leashed pets are teleported
	 * while still attached to living holders. Fence hitches are broken first.
	 * </p>
	 *
	 * @param trigger The entity to teleport.
	 * @param destination The destination location.
	 * @return {@code true} if the trigger entity was teleported.
	 */
	public static boolean teleportParty(Entity trigger, Location destination)
	{
		if (trigger == null || !trigger.isValid() || destination == null) return false;
		World destWorld = destination.getWorld();
		if (destWorld == null) return false;

		Set<Entity> party = collectParty(trigger);
		if (party.isEmpty()) return false;

		breakFenceHitches(party);

		List<Entity> roots = teleportRoots(party);
		if (roots.isEmpty()) return false;

		World fromWorld = trigger.getWorld();
		boolean crossWorld = fromWorld != null && !fromWorld.equals(destWorld);

		Map<UUID, List<UUID>> passengersByVehicle = snapshotPassengers(party);
		boolean teleported = false;

		for (Entity root : roots)
		{
			if (!root.isValid()) continue;
			Location dest = destination.clone();
			Location from = root.getLocation();
			dest.setYaw(from.getYaw());
			dest.setPitch(from.getPitch());
			if (root.teleport(dest))
			{
				teleported = true;
			}
		}

		if (!teleported) return false;

		// Same-world Paper/vanilla keeps riders on the vehicle. If any rider was left behind
		// (older Spigot / cross-world), reseat without a pre-teleport dismount flash.
		if (needsPassengerRestore(party, passengersByVehicle, destWorld) || crossWorld)
		{
			Runnable restore = () -> restorePassengers(party, passengersByVehicle, destination);
			if (crossWorld)
			{
				Bukkit.getScheduler().runTask(CreativeGates.get(), restore);
			}
			else
			{
				restore.run();
			}
		}

		return trigger.isValid();
	}

	/**
	 * Returns whether this living entity is eligible to trigger a wandering mob gate use.
	 * 
	 * @param entity The entity to check.
	 * @return True if the entity is eligible to trigger a wandering mob gate use.
	 */
	public static boolean isEligibleWanderingMob(LivingEntity entity)
	{
		if (entity == null || !entity.isValid() || entity.isDead()) return false;
		if (entity instanceof Player) return false;
		if (entity instanceof ArmorStand) return false;

		// Player-controlled stacks are handled by the player move path.
		if (vehicleOrPassengerContainsPlayer(entity)) return false;

		return true;
	}

	/**
	 * Returns whether this entity is an eligible non-living vehicle (boat, minecart, etc.).
	 * <p>
	 * Living vehicles (horses, pigs, Happy Ghasts, …) use {@link #isEligibleWanderingMob} instead
	 * so {@code VehicleMoveEvent} and living detection do not double-fire.
	 * </p>
	 */
	public static boolean isEligibleGateVehicle(Entity entity)
	{
		if (!(entity instanceof Vehicle)) return false;
		if (!entity.isValid()) return false;
		if (entity instanceof LivingEntity) return false;
		if (vehicleOrPassengerContainsPlayer(entity)) return false;
		return true;
	}

	/**
	 * Returns whether {@code entity} may trigger a non-player gate use (wandering mob or empty vehicle).
	 * 
	 * @param entity The entity to check.
	 * @return True if the entity is eligible to trigger a non-player gate use.
	 */
	public static boolean isEligibleNonPlayerTrigger(Entity entity)
	{
		if (entity instanceof LivingEntity)
		{
			return isEligibleWanderingMob((LivingEntity) entity);
		}
		return isEligibleGateVehicle(entity);
	}

	/**
	 * Marks every entity in {@code party} as having recently used a gate (debounce helper).
	 * 
	 * @param trigger The entity to collect the party for.
	 * @return A list of unique IDs of the entities in the travel party.
	 */
	public static Collection<UUID> partyIds(Entity trigger)
	{
		List<UUID> ids = new ArrayList<>();
		for (Entity member : collectParty(trigger))
		{
			ids.add(member.getUniqueId());
		}
		return ids;
	}

	/**
	 * Breaks fence hitches for leashed members about to gate-travel (drops the lead like vanilla overdistance).
	 * 
	 * @param party The party to break the fence hitches for.
	 */
	private static void breakFenceHitches(Set<Entity> party)
	{
		for (Entity member : party)
		{
			if (!(member instanceof LivingEntity living) || !living.isLeashed()) continue;
			Entity holder = living.getLeashHolder();
			if (!(holder instanceof LeashHitch hitch)) continue;

			living.setLeashHolder(null);
			if (hitch.isValid()) hitch.remove();
		}
	}

	/**
	 * Entities that should be teleported directly: vehicle roots and free-standing party members
	 * (e.g. leashed pets) that are not passengers of another party member.
	 * 
	 * @param party The party to teleport the roots for.
	 * @return A list of entities that should be teleported directly.
	 */
	private static List<Entity> teleportRoots(Set<Entity> party)
	{
		List<Entity> roots = new ArrayList<>();
		for (Entity member : party)
		{
			Entity vehicle = member.getVehicle();
			if (vehicle != null && party.contains(vehicle)) continue;
			roots.add(member);
		}
		return roots;
	}

	/**
	 * Takes a snapshot of the passengers by vehicle for the given party.
	 * 
	 * @param party The party to snapshot the passengers for.
	 * @return A map of vehicle IDs to lists of passenger IDs.
	 */
	private static Map<UUID, List<UUID>> snapshotPassengers(Set<Entity> party)
	{
		Map<UUID, List<UUID>> passengersByVehicle = new LinkedHashMap<>();
		for (Entity member : party)
		{
			List<UUID> passengerIds = new ArrayList<>();
			for (Entity passenger : member.getPassengers())
			{
				if (party.contains(passenger)) passengerIds.add(passenger.getUniqueId());
			}
			if (!passengerIds.isEmpty())
			{
				passengersByVehicle.put(member.getUniqueId(), passengerIds);
			}
		}
		return passengersByVehicle;
	}

	/**
	 * Checks if the passengers need to be restored after teleporting.
	 * 
	 * @param party The party to check.
	 * @param passengersByVehicle The passengers by vehicle.
	 * @param destWorld The destination world.
	 * @return True if the passengers need to be restored.
	 */
	private static boolean needsPassengerRestore(Set<Entity> party, Map<UUID, List<UUID>> passengersByVehicle, World destWorld)
	{
		Map<UUID, Entity> byId = indexById(party);
		for (Map.Entry<UUID, List<UUID>> entry : passengersByVehicle.entrySet())
		{
			Entity vehicle = byId.get(entry.getKey());
			if (vehicle == null || !vehicle.isValid()) return true;
			for (UUID passengerId : entry.getValue())
			{
				Entity passenger = byId.get(passengerId);
				if (passenger == null || !passenger.isValid()) return true;
				if (!vehicle.getPassengers().contains(passenger)) return true;
				if (passenger.getWorld() == null || !passenger.getWorld().equals(destWorld)) return true;
			}
		}
		return false;
	}

	/**
	 * Restores the passengers to their vehicles after teleporting.
	 * 
	 * @param party The party to restore the passengers for.
	 * @param passengersByVehicle The passengers by vehicle.
	 * @param destination The destination location.
	 */
	private static void restorePassengers(Set<Entity> party, Map<UUID, List<UUID>> passengersByVehicle, Location destination)
	{
		Map<UUID, Entity> byId = indexById(party);
		for (Map.Entry<UUID, List<UUID>> entry : passengersByVehicle.entrySet())
		{
			Entity vehicle = byId.get(entry.getKey());
			if (vehicle == null || !vehicle.isValid()) continue;

			for (UUID passengerId : entry.getValue())
			{
				Entity passenger = byId.get(passengerId);
				if (passenger == null || !passenger.isValid()) continue;

				if (!vehicle.getPassengers().contains(passenger))
				{
					Location dest = destination.clone();
					Location from = passenger.getLocation();
					dest.setYaw(from.getYaw());
					dest.setPitch(from.getPitch());
					passenger.teleport(dest);
					vehicle.addPassenger(passenger);
				}
			}
		}
	}

	/**
	 * Indexes the entities in the party by their unique IDs.
	 * 
	 * @param party The party to index.
	 * @return A map of entity IDs to entities.
	 */
	private static Map<UUID, Entity> indexById(Set<Entity> party)
	{
		Map<UUID, Entity> byId = new LinkedHashMap<>();
		for (Entity member : party)
		{
			byId.put(member.getUniqueId(), member);
		}
		return byId;
	}

	/**
	 * Checks if the entity or any of its passengers is a player.
	 * 
	 * @param entity The entity to check.
	 * @return True if the entity or any of its passengers is a player.
	 */
	private static boolean vehicleOrPassengerContainsPlayer(Entity entity)
	{
		Entity cursor = entity;
		while (cursor != null)
		{
			if (cursor instanceof Player player && !MUtil.isntPlayer(player)) return true;
			cursor = cursor.getVehicle();
		}
		return passengerTreeContainsPlayer(entity);
	}

	/**
	 * Checks if the entity or any of its passengers is a player.
	 * 
	 * @param entity The entity to check.
	 * @return True if the entity or any of its passengers is a player.
	 */
	private static boolean passengerTreeContainsPlayer(Entity entity)
	{
		for (Entity passenger : entity.getPassengers())
		{
			if (passenger instanceof Player player && !MUtil.isntPlayer(player)) return true;
			if (passengerTreeContainsPlayer(passenger)) return true;
		}
		return false;
	}

	/**
	 * Gets the root vehicle of the entity.
	 * 
	 * @param entity The entity to get the root vehicle for.
	 * @return The root vehicle of the entity.
	 */
	private static Entity getRootVehicle(Entity entity)
	{
		Entity root = entity;
		while (root.getVehicle() != null)
		{
			root = root.getVehicle();
		}
		return root;
	}

	/**
	 * Adds the vehicle tree of the entity to the party.
	 * 
	 * @param root The root vehicle to add.
	 * @param party The party to add the vehicle tree to.
	 */
	private static void addVehicleTree(Entity root, Set<Entity> party)
	{
		if (root == null || !root.isValid()) return;
		if (!party.add(root)) return;
		for (Entity passenger : root.getPassengers())
		{
			addVehicleTree(passenger, party);
		}
	}
}
