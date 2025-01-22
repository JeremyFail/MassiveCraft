package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeWarp;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.factions.event.EventFactionsWarpTeleport;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mixin.MixinTeleport;
import com.massivecraft.massivecore.mixin.TeleporterException;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.teleport.Destination;
import com.massivecraft.massivecore.teleport.DestinationSimple;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CmdFactionsWarpGo extends FactionsCommandWarp
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsWarpGo()
	{
		// Parameters
		this.addParameter(TypeWarp.get(), "warp");
		this.addParameter(TypeFaction.get(), "faction", "you");

		// Requirements
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Faction faction = this.readArgAt(1, msenderFaction);
		Warp warp = TypeWarp.get(faction).read(this.argAt(0), sender);
		String warpDesc = Txt.parse("<h>%s <i>in <reset>%s<i>", warp.getName(), faction.describeTo(msender, false));

		// Must be valid
		if (!warp.verifyIsValid()) return;

		// Any and MPerm
		if ( ! MPerm.getPermWarp().has(msender, faction, true)) return;
		
		if ( ! MConf.get().warpsTeleportAllowedFromEnemyTerritory && msender.isInEnemyTerritory())
		{
			throw new MassiveException().addMsg("<b>You cannot teleport to %s <b>while in the territory of an enemy faction.", warp);
		}
		
		if ( ! MConf.get().warpsTeleportAllowedFromDifferentWorld && !me.getWorld().getName().equalsIgnoreCase(warp.getWorld()))
		{
			throw new MassiveException().addMsg("<b>You cannot teleport to %s <b>while in a different world.", warpDesc);
		}

		Faction factionHere = BoardColl.get().getFactionAt(PS.valueOf(me.getLocation()));
		Location locationHere = me.getLocation().clone();
		
		// if player is not in a safe zone or their own faction territory, only allow teleport if no enemies are nearby
		if
		(
			MConf.get().warpsTeleportAllowedEnemyDistance > 0
			&&
			factionHere.getFlag(MFlag.getFlagPvp())
			&&
			(
				! msender.isInOwnTerritory()
				||
				(
					msender.isInOwnTerritory()
					&&
					! MConf.get().warpsTeleportIgnoreEnemiesIfInOwnTerritory
				)
			)
		)
		{
			World w = locationHere.getWorld();
			double x = locationHere.getX();
			double y = locationHere.getY();
			double z = locationHere.getZ();

			for (Player p : w.getPlayers())
			{
				if (MUtil.isntPlayer(p)) continue;
				
				if (!p.isOnline() || p.isDead() || p == me || p.getWorld() != w) continue;

				MPlayer fp = MPlayer.get(p);
				if (msender.getRelationTo(fp) != Rel.ENEMY) continue;

				Location l = p.getLocation();
				double dx = Math.abs(x - l.getX());
				double dy = Math.abs(y - l.getY());
				double dz = Math.abs(z - l.getZ());
				double max = MConf.get().warpsTeleportAllowedEnemyDistance;

				// box-shaped distance check
				if (dx > max || dy > max || dz > max)
					continue;

				throw new MassiveException().addMsg("<b>You cannot teleport to %s <b>while an enemy is within %f blocks of you.", warpDesc, max);
			}
		}

		// Event
		EventFactionsWarpTeleport event = new EventFactionsWarpTeleport(sender, warp);
		event.run();
		if (event.isCancelled()) return;
		
		// Apply
		try
		{
			Destination destination = new DestinationSimple(warp.getLocation(), warpDesc);
			MixinTeleport.get().teleport(me, destination, sender);
		}
		catch (TeleporterException e)
		{
			String message = e.getMessage();
			MixinMessage.get().messageOne(me, message);
		}
	}
	
}
