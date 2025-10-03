package com.massivecraft.factions.engine;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.Couple;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.List;
import java.util.Map.Entry;

public class EngineDenyTeleport extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static EngineDenyTeleport i = new EngineDenyTeleport();
	public static EngineDenyTeleport get() { return i; }
	
	// -------------------------------------------- //
	// CAN COMBAT DAMAGE HAPPEN
	// -------------------------------------------- //

	private enum TerritoryType
	{
		ENEMY,
		WILDERNESS,
		OWN,
		OTHER
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void canTeleportHappen(PlayerTeleportEvent event)
	{
		Entry<TeleportCause, TerritoryType> entry = shouldBeCancelled(event);
		if (entry == null) return;

		event.setCancelled(true);

		TeleportCause cause = entry.getKey();
		TerritoryType deny = entry.getValue();

		String teleportDesc = Txt.getNicedEnum(cause);
		String denyDesc = "";
		if (deny == TerritoryType.ENEMY) denyDesc = "enemy";
		if (deny == TerritoryType.WILDERNESS) denyDesc = "wilderness";
		if (deny == TerritoryType.OWN) denyDesc = "your own";
		if (deny == TerritoryType.OTHER) denyDesc = "other faction's";

		Player player = event.getPlayer();
		MixinMessage.get().msgOne(player, "<b>Teleportation with %s is not allowed in %s territory.", teleportDesc, denyDesc);
	}

	private Entry<TeleportCause, TerritoryType> shouldBeCancelled(PlayerTeleportEvent event)
	{
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return null;

		MPlayer mplayer = MPlayer.get(player);

		PS from = PS.valueOf(event.getFrom());
		PS to = PS.valueOf(event.getTo());

		TerritoryType typeFrom = getTerritoryType(mplayer, from);
		TerritoryType typeTo = getTerritoryType(mplayer, to);
		List<TerritoryType> types = MUtil.list(typeFrom, typeTo);

		TeleportCause cause = event.getCause();
		MConf mconf = MConf.get();

		if (cause == TeleportCause.CHORUS_FRUIT)
		{
			if (!mconf.allowChorusFruitInEnemyTerritory && types.contains(TerritoryType.ENEMY))
				return Couple.valueOf(cause, TerritoryType.ENEMY);

			if (!mconf.allowChorusFruitInWildernessTerritory && types.contains(TerritoryType.WILDERNESS))
				return Couple.valueOf(cause, TerritoryType.WILDERNESS);

			if (!mconf.allowChorusFruitInOwnTerritory && types.contains(TerritoryType.OWN))
				return Couple.valueOf(cause, TerritoryType.OWN);

			if (!mconf.allowChorusFruitInOtherTerritory && types.contains(TerritoryType.OTHER))
				return Couple.valueOf(cause, TerritoryType.OTHER);
		}
		else if (cause == TeleportCause.ENDER_PEARL)
		{
			if (!mconf.allowEnderPearlInEnemyTerritory && types.contains(TerritoryType.ENEMY))
				return Couple.valueOf(cause, TerritoryType.ENEMY);

			if (!mconf.allowEnderPearlInWildernessTerritory && types.contains(TerritoryType.WILDERNESS))
				return Couple.valueOf(cause, TerritoryType.WILDERNESS);

			if (!mconf.allowEnderPearlInOwnTerritory && types.contains(TerritoryType.OWN))
				return Couple.valueOf(cause, TerritoryType.OWN);

			if (!mconf.allowEnderPearlInOtherTerritory && types.contains(TerritoryType.OTHER))
				return Couple.valueOf(cause, TerritoryType.OTHER);
		}
		else
		{
			// Don't cancel other kinds of teleports
		}

		return null;
	}

	private TerritoryType getTerritoryType(MPlayer mplayer, PS territory)
	{
		Faction territoryFaction = BoardColl.get().getFactionAt(territory);
		Rel relation = territoryFaction.getRelationTo(mplayer);

		if (territoryFaction.isNone()) return TerritoryType.WILDERNESS;
		if (relation == Rel.ENEMY) return TerritoryType.ENEMY;
		if (relation == Rel.FACTION) return TerritoryType.OWN;
		return TerritoryType.OTHER;
	}

}
