package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.WildTpTeleportMode;
import com.massivecraft.factions.engine.EngineWildtp;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.teleport.DestinationWildtp;
import com.massivecraft.factions.util.WildTpSearchOrchestrator;
import com.massivecraft.factions.util.WildTpSearchResult;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.mixin.MixinTeleport;
import com.massivecraft.massivecore.mixin.TeleporterException;
import com.massivecraft.massivecore.util.TimeDiffUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * Parent command for wilderness teleport: {@code /f wildtp}.
 * <p>
 * With no subcommand, teleports the player to a safe wilderness destination after a stand-still delay.
 * Child commands {@code add} / {@code edit} / {@code remove} / {@code list} / {@code goto} manage predefined locations.
 * </p>
 */
public class CmdFactionsWildtp extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	/** Subcommand: add a predefined wildtp location at the player position. */
	public CmdFactionsWildtpAdd cmdFactionsWildtpAdd = new CmdFactionsWildtpAdd();
	/** Subcommand: edit a predefined wildtp location's no-claim radius. */
	public CmdFactionsWildtpEdit cmdFactionsWildtpEdit = new CmdFactionsWildtpEdit();
	/** Subcommand: remove a predefined wildtp location by name. */
	public CmdFactionsWildtpRemove cmdFactionsWildtpRemove = new CmdFactionsWildtpRemove();
	/** Subcommand: list predefined wildtp locations. */
	public CmdFactionsWildtpList cmdFactionsWildtpList = new CmdFactionsWildtpList();
	/** Subcommand: teleport to a named predefined wildtp location (list click). */
	public CmdFactionsWildtpGoto cmdFactionsWildtpGoto = new CmdFactionsWildtpGoto();

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	/**
	 * Creates the wildtp command. Use permission is checked in {@link #perform()} so
	 * add/remove children can use their own admin permissions without requiring use.
	 */
	public CmdFactionsWildtp()
	{
		this.setDesc("teleport to wilderness");
		this.addRequirements(RequirementIsPlayer.get());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	/**
	 * Runs the player wilderness teleport flow: gates, world resolution, dual-path search,
	 * MassiveCore delayed teleport, and session tracking for cost/cooldown after success.
	 *
	 * @throws MassiveException if a MassiveCore requirement/parameter layer throws
	 */
	@Override
	public void perform() throws MassiveException
	{
		MConf conf = MConf.get();

		// ---- Permission (explicit use node; not auto-derived from this class name) ----
		Perm.WILDTP_USE.hasOrThrow(sender);

		// ---- Feature toggle ----
		if (!conf.wildTpEnabled)
		{
			msg("<b>Wilderness teleport is disabled.");
			return;
		}

		// ---- Cooldown (last teleport on MPlayer; duration from current MConf) ----
		boolean bypassCooldown = Perm.WILDTP_BYPASS_COOLDOWN.has(sender, false);
		long remainingMillis = msender.getWildTpCooldownRemainingMillis();
		if (!bypassCooldown && remainingMillis > 0L)
		{
			msg("<b>You must wait <h>%s <b>before using wildtp again.", TimeDiffUtil.formattedCountdown(remainingMillis));
			return;
		}

		// ---- Cost affordability only (charge happens after successful teleport) ----
		boolean bypassCost = Perm.WILDTP_BYPASS_COST.has(sender, false);
		boolean willCharge = conf.wildTpCostEnabled && conf.wildTpCostAmount > 0D && !bypassCost;
		if (willCharge && !EngineWildtp.canAfford(msender, conf.wildTpCostAmount))
		{
			return; // canAfford already messaged the player/faction
		}

		// ---- Target world (enabled list, or deny/redirect) ----
		World targetWorld = this.resolveTargetWorld(conf);
		if (targetWorld == null) return;

		WildTpTeleportMode mode = conf.getWildTpTeleportMode();
		int warmup = conf.getWildTpWarmupSeconds(sender);
		boolean searchDuringDelay = conf.isWildTpSearchDuringDelay(warmup);
		boolean applyCooldown = !bypassCooldown;

		// Replace any prior pending wildtp session for this player
		EngineWildtp.get().clearSession(msender.getId());

		DestinationWildtp destination = new DestinationWildtp(msender.getId(), searchDuringDelay);
		long dueMillis = System.currentTimeMillis() + warmup * TimeUnit.MILLIS_PER_SECOND;

		if (!searchDuringDelay)
		{
			// Resolve-then-delay: find a spot now (always for predefined; also when warmup < threshold)
			WildTpSearchResult found = WildTpSearchOrchestrator.search(targetWorld, mode, me);
			if (found == null)
			{
				msg("<b>Could not find a safe wilderness location. Please try again, or contact an admin if this keeps happening.");
				return;
			}
			found.applyTo(destination);
			EngineWildtp.get().startSession(msender.getId(), destination, targetWorld, mode, willCharge, applyCooldown, dueMillis, true);
		}
		else
		{
			// Search-during-delay: schedule immediately; EngineWildtp fills destination before due time
			EngineWildtp.get().startSession(msender.getId(), destination, targetWorld, mode, willCharge, applyCooldown, dueMillis, false);
		}

		// MassiveCore handles stand-still messaging and cancel-on-move/damage
		try
		{
			MixinTeleport.get().teleport(me, destination, warmup);
		}
		catch (TeleporterException e)
		{
			EngineWildtp.get().clearSession(msender.getId());
			msg("<b>%s", e.getMessage());
		}
	}

	/**
	 * Resolves the world used for destination search from the player's current world and MConf.
	 *
	 * @param conf current Factions config
	 * @return target world, or null after messaging the player on failure
	 */
	private World resolveTargetWorld(MConf conf)
	{
		World current = me.getWorld();
		if (conf.wildTpEnabledWorlds == null || conf.wildTpEnabledWorlds.isEmpty())
		{
			msg("<b>No worlds are enabled for wilderness teleport.");
			return null;
		}

		if (conf.wildTpEnabledWorlds.contains(current.getName()))
		{
			return current;
		}

		if (conf.isWildTpNonEnabledWorldRedirect())
		{
			String redirect = conf.wildTpRedirectTargetWorld;
			if (redirect == null || redirect.isEmpty())
			{
				msg("<b>Wilderness teleport redirect world is not configured.");
				return null;
			}
			World world = Bukkit.getWorld(redirect);
			if (world == null)
			{
				msg("<b>Redirect world <h>%s<b> is not loaded.", redirect);
				return null;
			}
			return world;
		}

		msg("<b>Wilderness teleport is not enabled in this world.");
		return null;
	}

}
