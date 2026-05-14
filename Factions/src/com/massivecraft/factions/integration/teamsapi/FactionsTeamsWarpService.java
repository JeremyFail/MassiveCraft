package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.factions.event.EventFactionsWarpAdd;
import com.massivecraft.factions.event.EventFactionsWarpRemove;
import com.skyblockexp.teamsapi.api.TeamsWarpService;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Surfaces faction {@link Warp} collections as TeamsAPI {@link com.skyblockexp.teamsapi.model.TeamWarp TeamWarp} snapshots;
 * metadata such as canonical creator/time is synthesized where MassiveCraft does not persist it.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class FactionsTeamsWarpService implements TeamsWarpService
{
	private final Factions factionsPlugin;

	/**
	 * @param factionsPlugin plugin handle for warp events and fallback console routing
	 */
	public FactionsTeamsWarpService(final Factions factionsPlugin)
	{
		this.factionsPlugin = factionsPlugin;
	}

	/** Fallback sender when invoking player is offline/API-only - keeps events non-null without fabricating Players. */
	private CommandSender cmdConsole()
	{
		return this.factionsPlugin.getServer().getConsoleSender();
	}

	/**
	 * @return first warp whose stored name compares case-insensitively against {@code name}
	 */
	private static Optional<Warp> findWarp(final Faction faction, final String name)
	{
		if (faction == null || name == null) return Optional.empty();

		for (Warp warp : faction.getWarps().getAll())
		{
			if (warp != null && warp.getName().equalsIgnoreCase(name)) return Optional.of(warp);
		}

		return Optional.empty();
	}

	/**
	 * Translates persisted {@link PS} into {@link Location} without throwing across broken world references during async reads.
	 */
	private static Location toLocationSafe(final Warp warp)
	{
		if (warp == null || warp.getLocation() == null) return null;

		try
		{
			return warp.getLocation().asBukkitLocation(false);
		}
		catch (Throwable ignored)
		{
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Duplicate names replace in place (detach old warp). Warp caps and territory validation mirror player commands unless admin override bypasses caps.
	 */
	@Override
	public boolean setWarp(final UUID teamId, final String name, final Location location, final UUID creatorUUID)
	{
		if (teamId == null || name == null || name.isBlank() || location == null || creatorUUID == null)
		{
			return false;
		}

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;

		final Faction faction = facOpt.get();

		final MPlayer creator = TeamsApiFactionFacade.resolvePlayer(creatorUUID);

		final boolean overriding = creator.isOverriding();
		final boolean existsAlready =
			faction.getWarps().getAll().stream().anyMatch(w -> name.equalsIgnoreCase(w.getName()));

		final int maxWarps = MConf.get().warpsMax;

		if (!overriding && maxWarps >= 0 && !existsAlready && faction.getWarps().size() >= maxWarps)
		{
			return false;
		}

		final Warp warp = new Warp(name, PS.valueOf(location));

		if (!overriding && !warp.isValidFor(faction))
		{
			return false;
		}

		if (!MPerm.getPermSetwarp().has(creator, faction, false)) return false;

		findWarp(faction, name).ifPresent(Warp::detach);

		final CommandSender sender = creator.getSender() != null ? creator.getSender() : cmdConsole();

		EventFactionsWarpAdd event = new EventFactionsWarpAdd(sender, faction, warp);
		event.run();

		if (event.isCancelled()) return false;

		faction.getWarps().attach(event.getNewWarp());
		faction.changed();

		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Removal uses console attribution because TeamsAPI payloads lack an actor id suitable for auditing perm checks separately.
	 */
	@Override
	public boolean removeWarp(final UUID teamId, final String name)
	{
		if (teamId == null || name == null) return false;

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return false;

		final Faction faction = facOpt.get();

		Optional<Warp> warpOpt = findWarp(faction, name);

		if (warpOpt.isEmpty()) return false;

		final Warp warp = warpOpt.get();

		EventFactionsWarpRemove event = new EventFactionsWarpRemove(cmdConsole(), faction, warp);
		event.run();

		if (event.isCancelled()) return false;

		event.getWarp().detach();
		faction.changed();

		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Empty optional when warp missing or world cannot coerce to {@link Location} client-side snapshot.
	 */
	@Override
	public Optional getWarp(final UUID teamId, final String name)
	{
		if (teamId == null || name == null) return Optional.empty();

		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return Optional.empty();

		Optional<Warp> warpOpt = findWarp(facOpt.get(), name);
		if (warpOpt.isEmpty()) return Optional.empty();

		Location location = toLocationSafe(warpOpt.get());
		if (location == null) return Optional.empty();

		UUID factionUuid = TeamsApiFactionFacade.teamUuid(facOpt.get());

		Warp warp = warpOpt.get();

		return Optional.of(new TeamsApiSnapshotTeamWarp(factionUuid, warp.getName(), location, TeamsApiWarpIds.UNKNOWN,
			Instant.EPOCH));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection getWarps(final UUID teamId)
	{
		Optional<Faction> facOpt = TeamsApiFactionFacade.resolveNormal(teamId);
		if (facOpt.isEmpty()) return Collections.emptyList();

		final UUID factionUuid = TeamsApiFactionFacade.teamUuid(facOpt.get());
		final ArrayList<Object> snaps = new ArrayList<>();

		for (Warp warp : facOpt.get().getWarps().getAll())
		{
			Location loc = toLocationSafe(warp);

			if (loc == null) continue;

			snaps.add(new TeamsApiSnapshotTeamWarp(factionUuid, warp.getName(), loc, TeamsApiWarpIds.UNKNOWN, Instant.EPOCH));
		}

		return snaps.stream().collect(Collectors.toUnmodifiableList());
	}
}
