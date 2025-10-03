package com.massivecraft.factions.engine;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.CmdFactions;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MFlagColl;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsFlagChange;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.event.EventMassiveCorePlayerUpdate;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.DriverFlatfile;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.io.File;

public class EngineFly extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EngineFly i = new EngineFly();
	public static EngineFly get() { return i; }

	// -------------------------------------------- //
	// LISTENER
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onMassiveCorePlayerUpdate(EventMassiveCorePlayerUpdate event)
	{
		// If we are updating a player ...
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		
		// ... and that player isn't in creative or spectator mode ...
		if (EventMassiveCorePlayerUpdate.isFlyAllowed(player, false)) return;
		
		// ... and the player is alive ...
		if (player.isDead()) return;

		MPlayer mplayer = MPlayer.get(player);

		// ... and the player enables flying ...
		if (!mplayer.isFlying()) return;

		// ... and the player can fly here...
		if (!canFlyInTerritory(mplayer, PS.valueOf(player))) return;

		// ... set allowed ...
		event.setAllowed(true);
		
		// ... set speed ...
		event.setFlySpeed(MConf.get().flySpeed);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void moveChunkDetect(PlayerMoveEvent event)
	{
		// If the player is moving from one chunk to another ...
		if (MUtil.isSameChunk(event)) return;
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;

		// ... gather info on the player and the move ...
		MPlayer mplayer = MPlayer.get(player);
		PS chunkTo = PS.valueOf(event.getTo()).getChunk(true);

		// ... and they are currently flying ...
		if (!mplayer.isFlying()) return;

		// ... but can't fly at the new place ...
		if (canFlyInTerritory(mplayer, chunkTo)) return;

		// ... then perhaps they should not be
		mplayer.setFlying(false);
		deactivateForPlayer(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void flagUpdate(EventFactionsFlagChange event)
	{
		if (event.getFlag() != MFlag.getFlagFly()) return;
		if (event.isNewValue() == true) return;

		// Disable for all players when disabled
		event.getFaction().getOnlinePlayers().forEach(EngineFly::deactivateForPlayer);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void territoryShield(EntityDamageByEntityEvent event)
	{
		// If flying is diabled on PVP ...
		if (!MConf.get().flyDisableOnPvp) return;

		// ... and the defender is a player ...
		Entity entity = event.getEntity();
		if (MUtil.isntPlayer(entity)) return;
		Player defender = (Player)entity;
		MPlayer mdefender = MPlayer.get(defender);

		// ... and the attacker is a player ...
		Entity eattacker = MUtil.getLiableDamager(event);
		if (! (eattacker instanceof Player)) return;
		Player attacker = (Player) eattacker;
		MPlayer mattacker = MPlayer.get(attacker);

		// ... disable flying for both
		if (mdefender.isFlying())
		{
			mdefender.setFlying(false);
			deactivateForPlayer(defender);
			mdefender.msg("<i>Flying is disabled in combat.");
		}

		if (mattacker.isFlying())
		{
			mattacker.setFlying(false);
			deactivateForPlayer(attacker);
			mattacker.msg("<i>Flying is disabled in combat.");
		}
	}

	public static boolean canFlyInTerritory(MPlayer mplayer, PS ps)
	{
		try
		{
			canFlyInTerritoryOrThrow(mplayer, ps);
			return true;
		}
		catch (MassiveException ex)
		{
			return false;
		}
	}

	public static void canFlyInTerritoryOrThrow(MPlayer mplayer, PS ps) throws MassiveException
	{
		if (!mplayer.isPlayer())
		{
			throw new MassiveException().addMsg("<b>Only players can fly.");
		}

		Faction faction = mplayer.getFaction();
		Faction locationFaction = BoardColl.get().getFactionAt(ps.getChunk(true));

		if (faction != locationFaction)
		{
			throw new MassiveException().addMsg("<b>You can only fly within your own faction.");
		}

		// If the faction does not have the flag ...
		if (!faction.getFlag(MFlag.getFlagFly()))
		{
			MFlag flag = MFlag.getFlagFly();
			MassiveException ex = new MassiveException()
				.addMsg("<b>Flying requires that the <h>%s <b>flag is enabled for your faction.", flag.getName());

			// ... but they can change ...
			if (flag.isEditable()) {
				boolean canEdit = MPerm.getPermFlags().has(mplayer, faction, false);
				// ... and the player can edit it themselves ...
				if (canEdit) {
					// ... tell them to edit.
					ex.addMsg("<i>You can edit the flag with: ");
					ex.addMessage(CmdFactions.get().cmdFactionsFlag.cmdFactionsFlagSet.getTemplate(false, true, mplayer.getSender()));
				}
				// ... otherwise ...
				else {
					// .. tell them to have someone else edit it ...
					ex.addMsg("<i>You can ask a faction admin to change the flag.");
				}
			}
			// ... or only server admins can change it ...
			else
			{
				boolean isAdmin = Perm.OVERRIDE.has(mplayer.getSender());
				boolean isDefault = flag.isDefault();
				if (isAdmin)
				{
					boolean overriding = mplayer.isOverriding();
					ex.addMsg("<i>You can change the flag if you are overriding.");
					if (overriding) ex.addMsg("<i>You are already overriding.");
					else
					{
						ex.addMsg("<i>You can enable override with:");
						ex.addMessage(CmdFactions.get().cmdFactionsOverride.getTemplate(false, true, mplayer.getSender()));
					}

					if (!isDefault)
					{
						ex.addMsg("<i>You can also ask someone with access to the configuration files to make flying enabled by default.");
						if (MFlagColl.get().getDb().getDriver() instanceof DriverFlatfile)
						{
							File file = DriverFlatfile.getDirectory(MFlagColl.get());
							ex.addMsg("<i>Configuring the flags can be done by editing the files in <h>%s<i>.", file.getAbsoluteFile());
						}
					}
				}
				else
				{
					ex.addMsg("<b>Only server admins can change the flag. Per default flying is %s.", isDefault ? "enabled" : "disabled");
				}
			}
			throw ex;
		}
	}

	public static void deactivateForPlayer(Player player)
	{
		EventMassiveCorePlayerUpdate.resetFlyAllowed(player);
		EventMassiveCorePlayerUpdate.resetFlyActive(player);
		EventMassiveCorePlayerUpdate.resetFlySpeed(player);

		EventMassiveCorePlayerUpdate.run(player);
	}

}
