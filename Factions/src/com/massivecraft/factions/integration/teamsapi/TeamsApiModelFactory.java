package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamMember;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/**
 * Produces JDK dynamic proxies for TeamsAPI model interfaces using the TeamsAPI plugin {@link ClassLoader}.
 * <p>
 * Interface {@code Class} tokens and the {@code TeamRole} enum are resolved from the runtime TeamsAPI artifact so
 * Factions can compile against a {@code provided} dependency while still returning types the consumer class loader
 * recognizes. Each {@link InvocationHandler} maps TeamsAPI accessor names onto live {@link Faction} / {@link MPlayer}
 * state (or frozen tuples for warps and claims).
 * <p>
 * Production bridging uses immutable snapshot DTOs ({@link TeamsApiSnapshotTeam}, {@link TeamsApiSnapshotTeamMember},
 * etc.) instead of proxies. This factory remains for experiments or deployments that must avoid static linkage to
 * TeamsAPI bytecode on the Factions compile classpath.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class TeamsApiModelFactory
{
	/** Used for faction display-name serialization when a console observer is required. */
	private final Factions factionsPlugin;

	/** Class loader of the enabled TeamsAPI plugin; defines proxy interface types. */
	private final ClassLoader apiLoader;

	private final Class<?> teamInterfaceClass;
	private final Class<?> memberInterfaceClass;
	private final Class<?> warpInterfaceClass;
	private final Class<?> claimInterfaceClass;
	private final Class<?> teamRoleEnumClass;

	/**
	 * Captures TeamsAPI model interface classes from the runtime plugin class loader.
	 *
	 * @param teamsPlugin     enabled TeamsAPI plugin (defines the API classpath)
	 * @param factionsPlugin  Factions plugin reference for faction display lookups
	 * @param teamInterface   TeamsAPI {@link Team} interface {@code Class}
	 * @param memberInterface TeamsAPI {@link TeamMember} interface {@code Class}
	 * @param warpInterface   TeamsAPI TeamWarp interface {@code Class}
	 * @param claimInterface  TeamsAPI TeamClaim interface {@code Class}
	 * @param teamRoleEnum    TeamsAPI TeamRole enum {@code Class}
	 */
	TeamsApiModelFactory(
		final Plugin teamsPlugin,
		final Factions factionsPlugin,
		final Class<?> teamInterface,
		final Class<?> memberInterface,
		final Class<?> warpInterface,
		final Class<?> claimInterface,
		final Class<?> teamRoleEnum)
	{
		this.factionsPlugin = factionsPlugin;
		this.apiLoader = teamsPlugin.getClass().getClassLoader();
		this.teamInterfaceClass = teamInterface;
		this.memberInterfaceClass = memberInterface;
		this.warpInterfaceClass = warpInterface;
		this.claimInterfaceClass = claimInterface;
		this.teamRoleEnumClass = teamRoleEnum;
	}

	/**
	 * Resolves a TeamsAPI {@code TeamRole} enum constant from the API class loader.
	 * <p>
	 * The returned object is an enum instance understood by TeamsAPI consumers on the same loader
	 * ({@code OWNER}, {@code ADMIN}, {@code MEMBER}).
	 *
	 * @param canonicalConstantName {@link Enum#name()} of the desired constant
	 * @return enum constant loaded via {@link #teamRoleEnumClass}
	 * @throws IllegalArgumentException if the TeamsAPI artifact does not define the constant
	 */
	Object teamRoleConstant(final String canonicalConstantName)
	{
		final Class<? extends Enum> e = teamRoleEnumClass.asSubclass(Enum.class);
		return Enum.valueOf(e, canonicalConstantName);
	}

	/**
	 * Retrieves a TeamsAPI Team proxy for a faction.
	 * 
	 * @param factionMaybe resolved faction, or empty when lookup failed
	 * @return {@link Optional} containing a Team proxy, or empty when {@code factionMaybe} is absent
	 */
	Optional optionalTeam(final Optional<Faction> factionMaybe)
	{
		if (!factionMaybe.isPresent()) return Optional.empty();
		return Optional.of(teamProxyFor(factionMaybe.get()));
	}

	/**
	 * Creates a TeamsAPI Team proxy for a faction.
	 * 
	 * @param faction backing faction (live reference; proxy reads current state on each call)
	 * @return JDK proxy implementing the TeamsAPI Team interface for {@code faction}
	 */
	Object teamProxyFor(final Faction faction)
	{
		return Proxy.newProxyInstance(apiLoader, new Class<?>[] {teamInterfaceClass},
			new TeamProxyHandler(this, faction));
	}

	/**
	 * Creates a TeamsAPI TeamMember proxy for a member.
	 * 
	 * @param faction faction roster context
	 * @param mplayer member row within {@code faction}
	 * @return JDK proxy implementing the TeamsAPI TeamMember interface
	 */
	Object memberProxyFor(final Faction faction, final MPlayer mplayer)
	{
		return Proxy.newProxyInstance(apiLoader, new Class<?>[] {memberInterfaceClass},
			new MemberProxyHandler(this, faction, mplayer));
	}

	/**
	 * Builds a warp proxy with explicit creator and timestamp metadata.
	 *
	 * @param factionUuid     team id presented to TeamsAPI
	 * @param warpName        warp label inside the faction
	 * @param location        Bukkit location of the warp
	 * @param creatorUuid     creator player id, or a sentinel such as {@link TeamsApiWarpIds#UNKNOWN}
	 * @param createdInstant  creation instant, or {@link Instant#EPOCH} when unknown
	 * @return JDK proxy implementing the TeamsAPI TeamWarp interface
	 */
	Object warpProxy(final UUID factionUuid, final String warpName, final Location location, final UUID creatorUuid,
		final Instant createdInstant)
	{
		return Proxy.newProxyInstance(apiLoader, new Class<?>[] {warpInterfaceClass},
			new WarpProxyHandler(factionUuid, warpName, location, creatorUuid, createdInstant));
	}

	/**
	 * Convenience warp proxy using snapshot-bridge defaults for unknown creator and time.
	 *
	 * @param factionUuid team id
	 * @param warpName    warp label
	 * @param location    warp location
	 * @return empty when any argument is {@code null}; otherwise a warp proxy
	 */
	Optional optionalWarp(final UUID factionUuid, final String warpName, final Location location)
	{
		if (factionUuid == null || warpName == null || location == null) return Optional.empty();
		return Optional.of(warpProxy(factionUuid, warpName, location, TeamsApiWarpIds.UNKNOWN, Instant.EPOCH));
	}

	/**
	 * Materializes one warp proxy per table row, skipping null rows or rows without a location.
	 *
	 * @param factionUuid owning team id
	 * @param warpRows    pre-sanitized warp tuples from faction iteration
	 * @return unmodifiable collection of TeamWarp proxies (may be empty, never {@code null})
	 */
	Collection warpProxyList(final UUID factionUuid, final List<WarpTableRow> warpRows)
	{
		if (warpRows.isEmpty()) return Collections.emptyList();

		final ArrayList<Object> snaps = new ArrayList<>(warpRows.size());
		for (final WarpTableRow row : warpRows)
		{
			if (row == null || row.location == null) continue;
			snaps.add(warpProxy(factionUuid, row.name, row.location, TeamsApiWarpIds.UNKNOWN, Instant.EPOCH));
		}
		return Collections.unmodifiableCollection(snaps);
	}

	/**
	 * Creates a TeamsAPI TeamClaim proxy for a chunk.
	 * 
	 * @param factionUuid canonical team id for the claim
	 * @param worldName   world containing the chunk
	 * @param chunkX      chunk X coordinate
	 * @param chunkZ      chunk Z coordinate
	 * @return JDK proxy implementing the TeamsAPI TeamClaim interface
	 */
	Object claimProxy(final UUID factionUuid, final String worldName, final int chunkX, final int chunkZ)
	{
		return Proxy.newProxyInstance(apiLoader, new Class<?>[] {claimInterfaceClass},
			new ClaimProxyHandler(factionUuid, worldName, chunkX, chunkZ));
	}

	/**
	 * Bulk variant of {@link #claimProxy(UUID, String, int, int)} for board iteration results.
	 *
	 * @param factionUuid owning team id
	 * @param claims      chunk tuples gathered from the faction board
	 * @return unmodifiable collection of TeamClaim proxies
	 */
	Collection claimProxies(final UUID factionUuid, final Iterable<ClaimTableRow> claims)
	{
		final ArrayList<Object> snaps = new ArrayList<>();
		for (final ClaimTableRow row : claims)
		{
			snaps.add(claimProxy(factionUuid, row.worldName, row.chunkX, row.chunkZ));
		}
		return Collections.unmodifiableCollection(snaps);
	}

	/**
	 * Retrieves a TeamsAPI TeamClaim proxy for a chunk.
	 * 
	 * @param teamUuidMaybe team id when already resolved for a chunk lookup; {@code null} yields empty
	 * @param worldName     world name
	 * @param chunkX        chunk X
	 * @param chunkZ        chunk Z
	 * @return claim proxy wrapped in {@link Optional}, or empty when inputs are incomplete
	 */
	Optional optionalClaimMaybe(final UUID teamUuidMaybe, final String worldName, final int chunkX, final int chunkZ)
	{
		if (teamUuidMaybe == null || worldName == null) return Optional.empty();
		return Optional.of(claimProxy(teamUuidMaybe, worldName, chunkX, chunkZ));
	}

	/**
	 * Lightweight warp tuple used while iterating faction warp tables before proxy creation.
	 */
	static final class WarpTableRow
	{
		/** Warp name as stored on the faction. */
		final String name;

		/** Resolved Bukkit location for the warp. */
		final Location location;

		/**
		 * @param name     warp label
		 * @param location warp location
		 */
		WarpTableRow(final String name, final Location location)
		{
			this.name = name;
			this.location = location;
		}
	}

	/**
	 * Lightweight board chunk tuple used while iterating faction claims before proxy creation.
	 */
	static final class ClaimTableRow
	{
		final String worldName;
		final int chunkX;
		final int chunkZ;

		/**
		 * @param worldName world containing the chunk
		 * @param chunkX    chunk X coordinate
		 * @param chunkZ    chunk Z coordinate
		 */
		ClaimTableRow(final String worldName, final int chunkX, final int chunkZ)
		{
			this.worldName = worldName;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}
	}

	/**
	 * Reads the first reflective invocation argument as a {@link UUID}.
	 * <p>
	 * TeamsAPI interface methods pass player or team ids as the sole parameter; this helper avoids
	 * repeating null/length guards in each proxy handler branch.
	 *
	 * @param args {@link InvocationHandler#invoke} argument array
	 * @return first element cast to {@link UUID}, or {@code null} when absent
	 */
	private static UUID firstArgUuid(final Object[] args)
	{
		if (args == null || args.length == 0) return null;
		return (UUID) args[0];
	}

	/**
	 * {@link InvocationHandler} for TeamsAPI {@link Team}: delegates to a live {@link Faction} on each accessor call.
	 * <p>
	 * Member lists are rebuilt on every {@code getMembers} invocation so roster changes are visible without
	 * recreating the team proxy. {@link Object#equals(Object)} only matches other team proxies backed by the
	 * same faction id.
	 */
	private static final class TeamProxyHandler implements InvocationHandler
	{
		private final TeamsApiModelFactory factory;
		private final Faction faction;

		/**
		 * @param factory shared factory for nested member proxies
		 * @param faction backing faction entity
		 */
		private TeamProxyHandler(final TeamsApiModelFactory factory, final Faction faction)
		{
			this.factory = factory;
			this.faction = faction;
		}

		/**
		 * Dispatches TeamsAPI Team interface methods by method name.
		 *
		 * @throws UnsupportedOperationException for accessors not mapped by this handler
		 */
		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
		{
			final String name = method.getName();

			// Handled outside the switch so equals does not fall through to default
			if ("equals".equals(name))
			{
				final Object rhs = args != null && args.length > 0 ? args[0] : null;
				return Boolean.valueOf(sameUnderlyingTeam(rhs));
			}

			switch (name)
			{
				case "hashCode":
					return Integer.valueOf(this.faction.getId().hashCode());
				case "toString":
					return "FactionsTeamsApiTeam{factionId=" + this.faction.getId() + "}";
				case "getId":
					return TeamsApiFactionFacade.teamUuid(this.faction);
				case "getName":
					return this.faction.getName();
				case "getDisplayName":
				{
					// Faction display names may depend on an observing CommandSender
					final Object observer = factory.factionsPlugin != null
						? factory.factionsPlugin.getServer().getConsoleSender()
						: null;
					return this.faction.getDisplayName(observer);
				}
				case "getOwnerUUID":
				{
					final MPlayer leader = this.faction.getLeader();
					if (leader != null)
					{
						return UUID.fromString(leader.getId());
					}
					// Leaderless edge case: fall back to team id so API callers always receive a UUID
					return TeamsApiFactionFacade.teamUuid(this.faction);
				}
				case "getMembers":
				{
					final List<Object> snaps = new ArrayList<>();
					for (final MPlayer mp : this.faction.getMPlayers())
					{
						snaps.add(this.factory.memberProxyFor(this.faction, mp));
					}
					return Collections.unmodifiableCollection(snaps);
				}
				case "getMemberUUIDs":
				{
					final List<UUID> uids = new ArrayList<>();
					for (final MPlayer mp : this.faction.getMPlayers())
					{
						uids.add(UUID.fromString(mp.getId()));
					}
					return Collections.unmodifiableCollection(uids);
				}
				case "getSize":
					return Integer.valueOf(this.faction.getMPlayers().size());
				case "getMaxSize":
				{
					final int lim = MConf.get().factionMemberLimit;
					// TeamsAPI convention: non-positive configured limit means unlimited (-1)
					return Integer.valueOf(lim > 0 ? lim : (-1));
				}
				case "getMember":
				{
					final UUID pid = firstArgUuid(args);
					if (pid == null) return Optional.empty();
					for (final MPlayer mp : this.faction.getMPlayers())
					{
						if (pid.equals(UUID.fromString(mp.getId())))
						{
							return Optional.of(factory.memberProxyFor(this.faction, mp));
						}
					}
					return Optional.empty();
				}
				case "isMember":
				{
					final UUID pid = firstArgUuid(args);
					if (pid == null) return Boolean.FALSE;
					for (final MPlayer mp : this.faction.getMPlayers())
					{
						// MPlayer ids are stored as strings; compare via string form for consistency with persistence
						if (pid.toString().equals(mp.getId()))
						{
							return Boolean.TRUE;
						}
					}
					return Boolean.FALSE;
				}
				case "isOwner":
				{
					final UUID pid = firstArgUuid(args);
					if (pid == null) return Boolean.FALSE;
					final MPlayer leader = this.faction.getLeader();
					return Boolean.valueOf(leader != null && pid.equals(UUID.fromString(leader.getId())));
				}
				default:
					throw new UnsupportedOperationException(method.toString());
			}
		}

		/**
		 * Team proxy equality is defined by underlying MassiveCraft faction id, not proxy identity.
		 *
		 * @param rhs object compared against this proxy (typically another Team proxy)
		 * @return {@code true} when {@code rhs} is a {@link TeamProxyHandler} for the same faction id
		 */
		boolean sameUnderlyingTeam(final Object rhs)
		{
			if (!(rhs instanceof Proxy)) return false;
			final InvocationHandler h = Proxy.getInvocationHandler(rhs);
			if (!(h instanceof TeamProxyHandler)) return false;
			final TeamProxyHandler ot = (TeamProxyHandler) h;
			return this.faction.getId().equals(ot.faction.getId());
		}
	}

	/**
	 * {@link InvocationHandler} for TeamsAPI {@link TeamMember}: projects one {@link MPlayer} row within a faction.
	 * <p>
	 * {@code getRole} returns the API class loader's {@code TeamRole} enum; {@code getRoleDefinition} supplies
	 * per-rank name and prefix via {@link TeamsApiFactionFacade#roleDefinitionForFactionRank(Faction, com.massivecraft.factions.entity.Rank)}.
	 */
	private static final class MemberProxyHandler implements InvocationHandler
	{
		private final TeamsApiModelFactory factory;
		private final Faction faction;
		private final MPlayer mplayer;

		/**
		 * Creates a TeamsAPI TeamMember proxy for a member.
		 * 
		 * @param factory shared factory for enum resolution
		 * @param faction roster context
		 * @param mplayer member row
		 */
		private MemberProxyHandler(final TeamsApiModelFactory factory, final Faction faction, final MPlayer mplayer)
		{
			this.factory = factory;
			this.faction = faction;
			this.mplayer = mplayer;
		}

		/** {@inheritDoc} */
		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
		{
			switch (method.getName())
			{
				case "getPlayerUUID":
					return UUID.fromString(this.mplayer.getId());
				case "getRole":
				{
					final String canonical = TeamsApiFactionFacade.teamsApiRoleCodeForFactionRank(
						this.faction, this.mplayer.getRank());
					// Must use teamRoleConstant so the enum instance belongs to the TeamsAPI class loader
					return factory.teamRoleConstant(canonical);
				}
				case "getRoleDefinition":
					return TeamsApiFactionFacade.roleDefinitionForFactionRank(this.faction, this.mplayer.getRank());
				case "getJoinedAt":
					// Factions does not persist per-member join timestamps for API export
					return Instant.EPOCH;
				case "hashCode":
				{
					int h = faction.getId().hashCode();
					h = 31 * h + mplayer.getId().hashCode();
					return Integer.valueOf(h);
				}
				case "equals":
				{
					final Object rhs = args != null && args.length > 0 ? args[0] : null;
					if (!(rhs instanceof Proxy)) return Boolean.FALSE;
					final InvocationHandler h = Proxy.getInvocationHandler(rhs);
					if (!(h instanceof MemberProxyHandler)) return Boolean.FALSE;
					final MemberProxyHandler mh = (MemberProxyHandler) h;
					return Boolean.valueOf(
						faction.getId().equals(mh.faction.getId()) && mplayer.getId().equals(mh.mplayer.getId()));
				}
				case "toString":
					return "FactionsTeamsApiMember{factionId=" + faction.getId() + ",playerUuid=" + mplayer.getId() + "}";
				default:
					throw new UnsupportedOperationException(method.toString());
			}
		}
	}

	/**
	 * Immutable {@link InvocationHandler} for TeamsAPI TeamWarp: all fields are fixed at construction time.
	 */
	private static final class WarpProxyHandler implements InvocationHandler
	{
		private final UUID teamId;
		private final String warpName;
		private final Location location;
		private final UUID creatorUuid;
		private final Instant creationInstant;

		/**
		 * Creates a TeamsAPI TeamWarp proxy.
		 * 
		 * @param tid               owning team id
		 * @param warpName          warp label
		 * @param location          warp location
		 * @param creatorUuid       creator id or sentinel
		 * @param creationInstant   creation time or {@link Instant#EPOCH}
		 */
		private WarpProxyHandler(final UUID tid, final String warpName, final Location location,
			final UUID creatorUuid, final Instant creationInstant)
		{
			this.teamId = tid;
			this.warpName = warpName;
			this.location = location;
			this.creatorUuid = creatorUuid;
			this.creationInstant = creationInstant;
		}

		/** {@inheritDoc} */
		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
		{
			switch (method.getName())
			{
				case "getTeamId":
					return teamId;
				case "getName":
					return warpName;
				case "getLocation":
					return location;
				case "getCreatorUUID":
					return creatorUuid;
				case "getCreatedAt":
					return creationInstant;
				case "hashCode":
				{
					int h = teamId.hashCode();
					h = 31 * h + warpName.toLowerCase().hashCode();
					return Integer.valueOf(h);
				}
				case "equals":
				{
					final Object rhs = args != null && args.length > 0 ? args[0] : null;
					if (!(rhs instanceof Proxy)) return Boolean.FALSE;
					final InvocationHandler h = Proxy.getInvocationHandler(rhs);
					if (!(h instanceof WarpProxyHandler)) return Boolean.FALSE;
					final WarpProxyHandler w = (WarpProxyHandler) h;
					// Warp names are compared case-insensitively to mirror faction warp lookup behavior
					return Boolean.valueOf(teamId.equals(w.teamId) && warpName.equalsIgnoreCase(w.warpName));
				}
				case "toString":
					return "FactionsTeamsApiWarp{teamUuid=" + teamId + "," + warpName + "}";
				default:
					throw new UnsupportedOperationException(method.toString());
			}
		}
	}

	/**
	 * Immutable {@link InvocationHandler} for TeamsAPI TeamClaim: chunk coordinates are frozen at construction.
	 */
	private static final class ClaimProxyHandler implements InvocationHandler
	{
		private final UUID teamUuid;
		private final String worldName;
		private final int chunkX;
		private final int chunkZ;

		/**
		 * Creates a TeamsAPI TeamClaim proxy for a chunk.
		 * 
		 * @param teamUuid   owning team id
		 * @param worldName  world containing the chunk
		 * @param chunkX     chunk X
		 * @param chunkZ     chunk Z
		 */
		private ClaimProxyHandler(final UUID teamUuid, final String worldName, final int chunkX, final int chunkZ)
		{
			this.teamUuid = teamUuid;
			this.worldName = worldName;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}

		/** {@inheritDoc} */
		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
		{
			switch (method.getName())
			{
				case "getTeamId":
					return teamUuid;
				case "getWorldName":
					return worldName;
				case "getChunkX":
					return Integer.valueOf(chunkX);
				case "getChunkZ":
					return Integer.valueOf(chunkZ);
				case "getClaimedAt":
					return Instant.EPOCH;
				case "hashCode":
				{
					int h = teamUuid.hashCode();
					h = 31 * h + worldName.hashCode();
					h = 31 * h + chunkX;
					h = 31 * h + chunkZ;
					return Integer.valueOf(h);
				}
				case "equals":
				{
					final Object rhs = args != null && args.length > 0 ? args[0] : null;
					if (!(rhs instanceof Proxy)) return Boolean.FALSE;
					final InvocationHandler hdl = Proxy.getInvocationHandler(rhs);
					if (!(hdl instanceof ClaimProxyHandler)) return Boolean.FALSE;
					final ClaimProxyHandler o = (ClaimProxyHandler) hdl;
					return Boolean.valueOf(
						teamUuid.equals(o.teamUuid) && chunkX == o.chunkX && chunkZ == o.chunkZ && worldName.equals(o.worldName));
				}
				case "toString":
					return "FactionsTeamsApiClaim{teamUuid=" + teamUuid + "," + chunkX + "," + chunkZ + "}";
				default:
					throw new UnsupportedOperationException(method.toString());
			}
		}
	}
}
