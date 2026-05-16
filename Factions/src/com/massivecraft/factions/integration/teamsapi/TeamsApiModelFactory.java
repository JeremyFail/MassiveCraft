package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;

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
 * Produces JDK dynamic proxies for TeamsAPI model interfaces using the Teams plugin {@link ClassLoader},
 * avoiding reflective copies of TeamsAPI types on the compiling classpath.
 * <p>
 * Present-day Factions distribution ships snapshot DTO implementations instead ({@link TeamsApiSnapshotTeam}, etc.);
 * this factory remains available for experiments or alternate loaders that must avoid static TeamsAPI bytecode linkage.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class TeamsApiModelFactory
{

	private final Factions factionsPlugin;

	private final ClassLoader apiLoader;

	private final Class<?> teamInterfaceClass;

	private final Class<?> memberInterfaceClass;

	private final Class<?> warpInterfaceClass;

	private final Class<?> claimInterfaceClass;

	private final Class<?> teamRoleEnumClass;

	/**
	 * @param teamsPlugin     enabled TeamsAPI plugin (defines the API classpath)
	 * @param factionsPlugin  this plugin reference for faction display lookups
	 * @param teamInterface   TeamsAPI Team interface {@code Class}
	 * @param memberInterface TeamsAPI TeamMember interface
	 * @param warpInterface   TeamsAPI TeamWarp interface
	 * @param claimInterface  TeamsAPI TeamClaim interface
	 * @param teamRoleEnum    TeamsAPI TeamRole enum class
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
	 * Resolves TeamsAPI TeamRole ordinal enum constant (<code>OWNER</code>/<code>ADMIN</code>/<code>MEMBER</code>).
	 *
	 * @param canonicalConstantName constant name identical to TeamsAPI enumeration members
	 * @throws IllegalArgumentException if the TeamsAPI artifact does not contain the expected constant name
	 */
	Object teamRoleConstant(final String canonicalConstantName)
	{
		final Class<? extends Enum> e = teamRoleEnumClass.asSubclass(Enum.class);

		return Enum.valueOf(e, canonicalConstantName);
	}

	/**
	 * Wraps faction presence into TeamsAPI-compatible {@link Optional} payloads when available.
	 */
	Optional optionalTeam(final Optional<Faction> factionMaybe)
	{
		if (!factionMaybe.isPresent()) return Optional.empty();

		return Optional.of(teamProxyFor(factionMaybe.get()));
	}

	/**
	 * @return JDK proxy honoring TeamsAPI Team contract for {@code faction}
	 */
	Object teamProxyFor(final Faction faction)
	{
		return Proxy.newProxyInstance(apiLoader, new Class<?>[] {teamInterfaceClass},
			new TeamProxyHandler(this, faction));
	}

	/** @return {@link Proxy} honoring TeamsAPI TeamMember for {@code mplayer}'s faction membership slice */
	Object memberProxyFor(final Faction faction, final MPlayer mplayer)
	{
		return Proxy.newProxyInstance(apiLoader, new Class<?>[] {memberInterfaceClass},
			new MemberProxyHandler(this, faction, mplayer));
	}

	/** Assembles warp proxy honoring TeamsAPI accessors for location/creator sentinel fields. */
	Object warpProxy(final UUID factionUuid, final String warpName, final Location location, final UUID creatorUuid,
		final Instant createdInstant)
	{
		return Proxy.newProxyInstance(apiLoader, new Class<?>[] {warpInterfaceClass},
			new WarpProxyHandler(factionUuid, warpName, location, creatorUuid, createdInstant));
	}

	/** Shortcut producing warp proxies with sentinel metadata matching snapshot bridge defaults. */
	Optional optionalWarp(final UUID factionUuid, final String warpName, final Location location)
	{
		if (factionUuid == null || warpName == null || location == null) return Optional.empty();

		return Optional.of(warpProxy(factionUuid, warpName, location, TeamsApiWarpIds.UNKNOWN, Instant.EPOCH));
	}

	/**
	 * @param warpRows sanitized warp name/location tuples (null rows skipped silently)
	 * @return unmodifiable heterogeneous collection interpreted by TeamsAPI consumer as warp list snapshots
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

	/** @return chunk claim proxy projecting MassiveCraft board coordinates onto TeamsAPI claim contract */
	Object claimProxy(final UUID factionUuid, final String worldName, final int chunkX, final int chunkZ)
	{
		return Proxy.newProxyInstance(apiLoader, new Class<?>[] {claimInterfaceClass},
			new ClaimProxyHandler(factionUuid, worldName, chunkX, chunkZ));
	}

	/** Bulk variant of {@link #claimProxy(UUID, String, int, int)} iterating board rows gathered elsewhere. */
	Collection claimProxies(final UUID factionUuid, final Iterable<ClaimTableRow> claims)
	{
		final ArrayList<Object> snaps = new ArrayList<>();

		for (final ClaimTableRow row : claims)
		{
			snaps.add(claimProxy(factionUuid, row.worldName, row.chunkX, row.chunkZ));
		}

		return Collections.unmodifiableCollection(snaps);
	}

	/** Optional facade when chunk lookup already resolved authoritative team UUID. */
	Optional optionalClaimMaybe(final UUID teamUuidMaybe, final String worldName, final int chunkX, final int chunkZ)
	{
		if (teamUuidMaybe == null || worldName == null) return Optional.empty();

		return Optional.of(claimProxy(teamUuidMaybe, worldName, chunkX, chunkZ));
	}

	/** Plain record-style tuple used when iterating faction warps for proxy hydration. */
	static final class WarpTableRow
	{
		final String name;

		final Location location;

		WarpTableRow(final String name, final Location location)
		{
			this.name = name;
			this.location = location;
		}

	}

	/** Board chunk tuple mirroring faction claim iteration without retaining {@link com.massivecraft.massivecore.ps.PS} references longer than needed. */
	static final class ClaimTableRow
	{
		final String worldName;

		final int chunkX;

		final int chunkZ;

		ClaimTableRow(final String worldName, final int chunkX, final int chunkZ)
		{
			this.worldName = worldName;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}

	}

	/**
	 * Dynamic proxy implementation for TeamsAPI Team backed by mutable {@link Faction} lookups at invocation time where needed.
	 */
	private static final class TeamProxyHandler implements InvocationHandler
	{

		private final TeamsApiModelFactory factory;

		private final Faction faction;


		private TeamProxyHandler(final TeamsApiModelFactory factory, final Faction faction)
		{
			this.factory = factory;
			this.faction = faction;
		}


		/**
		 * {@link InvocationHandler#invoke}: maps TeamsAPI accessor names to faction getters with lightweight equals/hash bridging.
		 */
		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
		{
			final String name = method.getName();

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





		/** Team equality compares delegated Massive faction ids ignoring proxy wrappers. */
		boolean sameUnderlyingTeam(final Object rhs)



		{






			if (!(rhs instanceof Proxy)) return false;





			final InvocationHandler h = Proxy.getInvocationHandler(rhs);





			if (!(h instanceof TeamProxyHandler))



			{






				return false;





			}





			final TeamProxyHandler ot = (TeamProxyHandler) h;





			return this.faction.getId().equals(ot.faction.getId());





		}





	}




	/** Safely reads TeamsAPI unary UUID parameter from reflective {@code invoke} arrays. */
	private static UUID firstArgUuid(final Object[] args)



	{

		if (args == null || args.length == 0) return null;

		return (UUID) args[0];

	}




	/**
	 * TeamsAPI TeamMember proxy wrapping an {@link MPlayer} belonging to {@link Faction}.
	 */
	private static final class MemberProxyHandler implements InvocationHandler



	{

		private final TeamsApiModelFactory factory;

		private final Faction faction;

		private final MPlayer mplayer;







		private MemberProxyHandler(final TeamsApiModelFactory factory,
			final Faction faction,

			final MPlayer mplayer)
		{


			this.factory = factory;

			this.faction = faction;

			this.mplayer = mplayer;

		}




		/**
		 * {@link InvocationHandler#invoke} projecting {@link MPlayer} rank and ids into TeamsAPI accessors.
		 */
		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable

		{






			final String nm = method.getName();






			switch (nm)



			{




				case "getPlayerUUID":


					return UUID.fromString(this.mplayer.getId());






				case "getRole":




				{






					final String canonical = TeamsApiFactionFacade


						.teamsApiRoleCodeForFactionRank(this.faction, this.mplayer.getRank());







					return factory.teamRoleConstant(canonical);



				}




				case "getJoinedAt":
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



					return Boolean.valueOf(faction.getId().equals(mh.faction.getId())


						&& mplayer.getId().equals(mh.mplayer.getId()));



				}








				case "toString":
					return "FactionsTeamsApiMember{factionId=" + faction.getId()

						+ ",playerUuid=" + mplayer.getId() + "}";



				default:


					throw new UnsupportedOperationException(method.toString());





			}



		}




	}





	/** Immutable warp tuple proxy projecting Bukkit locations and sentinel creator timing into Teams accessors. */
	private static final class WarpProxyHandler implements InvocationHandler


	{

		private final UUID teamId;

		private final String warpName;



		private final Location location;



		private final UUID creatorUuid;



		private final Instant creationInstant;



		private WarpProxyHandler(final UUID tid, final String warpName,

			final Location location, final UUID creatorUuid, final Instant creationInstant)



		{






			this.teamId = tid;


			this.warpName = warpName;


			this.location = location;


			this.creatorUuid = creatorUuid;


			this.creationInstant = creationInstant;


		}




		/**
		 * Stateless warp tuple proxy - getters only mirror constructor fields captured at creation.
		 */
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



					return Boolean.valueOf(teamId.equals(w.teamId) && warpName.equalsIgnoreCase(w.warpName));


				}




				case "toString":
					return "FactionsTeamsApiWarp{teamUuid=" + teamId + "," + warpName + "}";





				default:
					throw new UnsupportedOperationException(method.toString());





			}


		}


	}




	/** Immutable TeamsAPI proxy row exposing fixed board coordinates frozen at instantiation. */
	private static final class ClaimProxyHandler implements InvocationHandler


	{

		private final UUID teamUuid;

		private final String worldName;



		private final int chunkX;

		private final int chunkZ;



		private ClaimProxyHandler(final UUID teamUuid, final String worldName,

			final int chunkX,

			final int chunkZ)



		{







			this.teamUuid = teamUuid;

			this.worldName = worldName;

			this.chunkX = chunkX;

			this.chunkZ = chunkZ;



		}








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

					if (!(rhs instanceof Proxy))


					{

						return Boolean.FALSE;

					}




					final InvocationHandler hdl = Proxy.getInvocationHandler(rhs);

					if (!(hdl instanceof ClaimProxyHandler))


					{




						return Boolean.FALSE;

					}




					final ClaimProxyHandler o = (ClaimProxyHandler) hdl;


					return Boolean.valueOf(teamUuid.equals(o.teamUuid) && chunkX == o.chunkX && chunkZ == o.chunkZ





						&& worldName.equals(o.worldName));


				}




				case "toString":
					return "FactionsTeamsApiClaim{teamUuid=" + teamUuid + "," + chunkX + "," + chunkZ + "}";

				default:


					throw new UnsupportedOperationException(method.toString());





			}


		}




	}




}
