package com.massivecraft.factions.util;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.factions.entity.Rank;

/**
 * Faction permission hierarchy: which {@link MPermable} grants a permission for a subject in a host faction,
 * and short text for tooltips. Used by {@link Faction#isPlayerPermitted} and the perm table UI.
 */
public final class PermUtil
{
	private PermUtil() { }

	/**
	 * Highest-level {@link MPermable} that grants {@code permId} to {@code mplayer} in {@code hostFaction}.
	 * Order: relation, player's faction, rank, player. Returns null if not permitted.
	 * 
	 * @param hostFaction the faction to check the permission for
	 * @param mplayer the player to check the permission for
	 * @param permId the permission id to check
	 * @return the permable that grants this permission for the player
	 */
	public static MPermable getPlayerPermissionSourcePermable(Faction hostFaction, MPlayer mplayer, String permId)
	{
		if (mplayer == null || permId == null || hostFaction == null)
		{
			return null;
		}
		Rel rel = RelationUtil.getRelationOfThatToMe(mplayer, hostFaction);
		if (hostFaction.isPermitted(rel.toString(), permId))
		{
			return rel;
		}
		if (mplayer.getFaction() != null && hostFaction.isPermitted(mplayer.getFaction().getId(), permId))
		{
			return mplayer.getFaction();
		}
		if (hostFaction.isPermitted(mplayer.getRank().getId(), permId))
		{
			return mplayer.getRank();
		}
		if (hostFaction.isPermitted(mplayer.getId(), permId))
		{
			return mplayer;
		}
		return null;
	}

	/**
	 * Highest-level grantor for {@code otherFaction} in {@code hostFaction}: relation, then that faction's id.
	 * 
	 * @param hostFaction the faction to check the permission for
	 * @param otherFaction the faction to check the permission for
	 * @param permId the permission id to check
	 * @return the permable that grants this permission for the other faction
	 */
	public static MPermable getFactionPermissionSourcePermable(Faction hostFaction, Faction otherFaction, String permId)
	{
		if (otherFaction == null || permId == null || hostFaction == null)
		{
			return null;
		}
		Rel rel = RelationUtil.getRelationOfThatToMe(otherFaction, hostFaction);
		if (hostFaction.isPermitted(rel.toString(), permId))
		{
			return rel;
		}
		if (hostFaction.isPermitted(otherFaction.getId(), permId))
		{
			return otherFaction;
		}
		return null;
	}

	/**
	 * Highest-level grantor for {@code rank} in {@code hostFaction}: rank's faction relation,
	 * rank's faction, then rank.
	 * 
	 * @param hostFaction the faction to check the permission for
	 * @param rank the rank to check the permission for
	 * @param permId the permission id to check
	 * @return the permable that grants this permission for the rank
	 */
	public static MPermable getRankPermissionSourcePermable(Faction hostFaction, Rank rank, String permId)
	{
		if (rank == null || permId == null || hostFaction == null)
		{
			return null;
		}
		Faction rankFaction = rank.getFaction();
		if (rankFaction != null)
		{
			Rel rel = RelationUtil.getRelationOfThatToMe(rankFaction, hostFaction);
			if (hostFaction.isPermitted(rel.toString(), permId))
			{
				return rel;
			}
			if (hostFaction.isPermitted(rankFaction.getId(), permId))
			{
				return rankFaction;
			}
		}
		if (hostFaction.isPermitted(rank.getId(), permId))
		{
			return rank;
		}
		return null;
	}

	/**
	 * User-facing line for tooltips: "Player - name", "Rank - name", etc.
	 * 
	 * @param source the permable to format the description for
	 * @return the user-facing line for tooltips: "Player - name", "Rank - name", etc.
	 */
	public static String formatPermissionSourceDesc(MPermable source)
	{
		if (source == null)
		{
			return null;
		}
		if (source instanceof MPlayer)
		{
			return "Player - " + ((MPlayer) source).getName();
		}
		if (source instanceof Faction)
		{
			return "Faction - " + ((Faction) source).getName();
		}
		if (source instanceof Rank)
		{
			return "Rank - " + ((Rank) source).getName();
		}
		if (source instanceof Rel)
		{
			return "Relation - " + ((Rel) source).getName();
		}
		return source.getDisplayName(null);
	}
}
