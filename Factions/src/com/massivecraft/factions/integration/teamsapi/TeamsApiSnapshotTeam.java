package com.massivecraft.factions.integration.teamsapi;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamMember;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Immutable TeamsAPI {@link Team} projection materialized once per query from a live {@link Faction}: membership snapshots are
 * built eagerly so downstream consumers never leak mutable MassiveCraft state.
 */
public final class TeamsApiSnapshotTeam implements Team
{
	private final UUID teamId;
	private final String name;
	private final String displayName;
	private final UUID ownerUuid;
	private final List<TeamMember> members;
	private final Map<UUID, TeamMember> memberByUuid;
	/** Mirrors {@link com.massivecraft.factions.entity.MConf#factionMemberLimit}; non-positive emits {@link #getMaxSize()} {@code -1}. */
	private final int maxSize;

	/**
	 * Creates a new TeamsApiSnapshotTeam instance.
	 * 
	 * @param teamId The team ID.
	 * @param name The team name.
	 * @param displayName The team display name.
	 * @param ownerUuid The owner UUID.
	 * @param members The team members.
	 * @param memberByUuid The team members by UUID.
	 * @param maxSize The team max size.
	 */
	private TeamsApiSnapshotTeam(
		final UUID teamId,
		final String name,
		final String displayName,
		final UUID ownerUuid,
		final List<TeamMember> members,
		final Map<UUID, TeamMember> memberByUuid,
		final int maxSize)
	{
		this.teamId = teamId;
		this.name = name;
		this.displayName = displayName;
		this.ownerUuid = ownerUuid;
		this.members = members;
		this.memberByUuid = memberByUuid;
		this.maxSize = maxSize;
	}

	/**
	 * Copies all online/offline roster entries into deterministic {@link LinkedHashMap} iteration order keyed by UUID.
	 *
	 * @param faction      backing entity (need not remain stable after returning - snapshot is immutable)
	 * @param factionsPlugin optionally supplies console observer for faction display serialization; {@code null} falls back to raw name
	 */
	public static TeamsApiSnapshotTeam of(final Faction faction, final Plugin factionsPlugin)
	{
		Object observer = factionsPlugin != null ? factionsPlugin.getServer().getConsoleSender() : null;

		final UUID tid = TeamsApiFactionFacade.teamUuid(faction);
		final String rawName = faction.getName();

		final MPlayer leader = faction.getLeader();
		final UUID owner =
			leader != null ? UUID.fromString(leader.getId()) : TeamsApiFactionFacade.teamUuid(faction);

		Map<UUID, TeamMember> byUuid = new LinkedHashMap<>();
		for (MPlayer mp : faction.getMPlayers())
		{
			UUID pid = UUID.fromString(mp.getId());
			byUuid.put(pid, TeamsApiFactionFacade.memberOf(faction, mp));
		}

		List<TeamMember> list = byUuid.values().stream().collect(Collectors.toUnmodifiableList());

		String display = observer != null ? faction.getDisplayName(observer) : rawName;

		com.massivecraft.factions.entity.MConf cfg = com.massivecraft.factions.entity.MConf.get();
		int lim = cfg.factionMemberLimit;

		return new TeamsApiSnapshotTeam(tid, rawName, display, owner, list, Collections.unmodifiableMap(byUuid), lim);
	}

	/** {@inheritDoc} */
	@Override
	public UUID getId()
	{
		return this.teamId;
	}

	/** {@inheritDoc} */
	@Override
	public String getName()
	{
		return this.name;
	}

	/** {@inheritDoc} */
	@Override
	public String getDisplayName()
	{
		return this.displayName;
	}

	/** {@inheritDoc} */
	@Override
	public UUID getOwnerUUID()
	{
		return this.ownerUuid;
	}

	/** {@inheritDoc} */
	@Override
	public Collection<TeamMember> getMembers()
	{
		return this.members;
	}

	/** {@inheritDoc} */
	@Override
	public Collection<UUID> getMemberUUIDs()
	{
		return this.memberByUuid.keySet().stream().collect(Collectors.toUnmodifiableList());
	}

	/** {@inheritDoc} */
	@Override
	public int getSize()
	{
		return this.members.size();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * MassiveCraft encodes unlimited membership with non-positive configured limits translating to TeamsAPI {@code -1}.
	 */
	@Override
	public int getMaxSize()
	{
		return this.maxSize > 0 ? this.maxSize : -1;
	}

	/** {@inheritDoc} */
	@Override
	public Optional<TeamMember> getMember(final UUID playerUuid)
	{
		if (playerUuid == null) return Optional.empty();
		return Optional.ofNullable(this.memberByUuid.get(playerUuid));
	}

	/** {@inheritDoc} */
	@Override
	public boolean isMember(final UUID playerUUID)
	{
		return playerUUID != null && this.memberByUuid.containsKey(playerUUID);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isOwner(final UUID playerUUID)
	{
		return playerUUID != null && Objects.equals(this.ownerUuid, playerUUID);
	}
}
