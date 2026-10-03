package com.massivecraft.factions.teleport;

import com.massivecraft.factions.engine.EngineWildtp;
import com.massivecraft.factions.util.WildTpDestinationValidator;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.teleport.DestinationAbstract;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;

/**
 * Mutable wildtp destination for MassiveCore delayed teleport.
 * <p>
 * {@link com.massivecraft.massivecore.mixin.MixinTeleport} always calls {@link #getPs(Object)} before
 * scheduling a delayed teleport. For search-during-delay, the first call may return a placeholder
 * (the player's current position); later calls return the resolved wilderness PS (revalidated) or throw
 * if search failed.
 * </p>
 */
public class DestinationWildtp extends DestinationAbstract
{
	private static final long serialVersionUID = 1L;

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	/** Id of the player being teleported. */
	private final String teleporteeId;
	/** Validated destination once search succeeds; null until then. */
	private volatile PS resolved;
	/** When true, revalidate with predefined rules (skip global claim/spawn/biome/war-safe). */
	private volatile boolean resolvedFromPredefined;
	/** When true, subsequent {@link #getPs(Object)} calls throw a failure message. */
	private volatile boolean searchFailed;
	/** Whether the schedule-time placeholder {@link #getPs(Object)} has already been used. */
	private volatile boolean placeholderUsed;
	/** When true, the first {@link #getPs(Object)} may return the player's current PS. */
	private final boolean allowPlaceholder;

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	/**
	 * @param teleporteeId player id passed to MassiveCore teleport APIs
	 * @param allowPlaceholder when true, first getPs returns the player's current PS so Core can schedule before search finishes
	 */
	public DestinationWildtp(String teleporteeId, boolean allowPlaceholder)
	{
		this.teleporteeId = teleporteeId;
		this.allowPlaceholder = allowPlaceholder;
		this.setDesc("wilderness");
	}

	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //

	/**
	 * Sets a random/claim-edge destination (full revalidation rules).
	 *
	 * @param ps validated destination PS
	 */
	public void setResolved(PS ps)
	{
		this.setResolved(ps, false);
	}

	/**
	 * Sets the resolved destination and whether predefined revalidation applies.
	 *
	 * @param ps validated destination PS
	 * @param fromPredefined true to use predefined revalidation at fire time
	 */
	public void setResolved(PS ps, boolean fromPredefined)
	{
		this.resolved = ps;
		this.resolvedFromPredefined = fromPredefined;
	}

	/**
	 * @return resolved PS, or null if not yet found
	 */
	public PS getResolved()
	{
		return this.resolved;
	}

	/**
	 * Marks search as failed so subsequent {@link #getPs(Object)} calls throw.
	 */
	public void setSearchFailed()
	{
		this.searchFailed = true;
	}

	/**
	 * @return true if a destination has been resolved
	 */
	public boolean isResolved()
	{
		return this.resolved != null;
	}

	/**
	 * @return teleportee id this destination belongs to
	 */
	public String getTeleporteeId()
	{
		return this.teleporteeId;
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	/**
	 * Returns the teleport PS for schedule-time and fire-time calls.
	 *
	 * @param watcherObject unused watcher context from MassiveCore
	 * @return destination PS
	 * @throws MassiveException if search failed, revalidation failed, or no destination is available
	 */
	@Override
	public PS getPs(Object watcherObject) throws MassiveException
	{
		if (this.searchFailed)
		{
			throw new MassiveException().addMessage(Txt.parse("<b>Could not find a safe wilderness location. Please try again, or contact an admin if this keeps happening."));
		}

		if (this.resolved != null)
		{
			Player teleportee = IdUtil.getPlayer(this.teleporteeId);
			boolean ok = this.resolvedFromPredefined
				? WildTpDestinationValidator.isValidPredefined(this.resolved, teleportee)
				: WildTpDestinationValidator.isValid(this.resolved, false, teleportee);
			if (!ok)
			{
				EngineWildtp.get().clearSession(this.teleporteeId);
				throw new MassiveException().addMessage(Txt.parse("<b>That wilderness location is no longer valid. Please try again."));
			}
			return this.resolved;
		}

		if (this.allowPlaceholder && !this.placeholderUsed)
		{
			this.placeholderUsed = true;
			Player player = IdUtil.getPlayer(this.teleporteeId);
			if (player == null)
			{
				throw new MassiveException().addMessage(Txt.parse("<b>Player is offline."));
			}
			return PS.valueOf(player.getLocation());
		}

		throw new MassiveException().addMessage(Txt.parse("<b>Could not find a safe wilderness location. Please try again, or contact an admin if this keeps happening."));
	}

	/**
	 * @return the resolved PS without side effects, or null
	 */
	@Override
	public PS getPsInner()
	{
		return this.resolved;
	}

	/**
	 * @param watcherObject unused
	 * @return player-facing message when no PS is available
	 */
	@Override
	public String getMessagePsNull(Object watcherObject)
	{
		return Txt.parse("<b>Could not find a safe wilderness location. Please try again, or contact an admin if this keeps happening.");
	}

}
