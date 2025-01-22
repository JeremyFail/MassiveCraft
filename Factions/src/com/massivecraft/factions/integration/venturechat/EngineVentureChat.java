package com.massivecraft.factions.integration.venturechat;

import com.massivecraft.factions.engine.EngineChat;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.Engine;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import mineverse.Aust1n46.chat.channel.ChatChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.function.Predicate;

public class EngineVentureChat extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static EngineVentureChat i = new EngineVentureChat();
	public static EngineVentureChat get() { return i; }

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void setActiveInner(boolean active)
	{

	}
	
	// -------------------------------------------- //
	// LISTENER
	// -------------------------------------------- //

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void filterFaction(AsyncPlayerChatEvent event)
	{
		// Get player
		Player player = event.getPlayer();
		MineverseChatPlayer chatPlayer = MineverseChatAPI.getMineverseChatPlayer(player);
		ChatChannel channel = chatPlayer.getCurrentChannel();
		String channelName = channel.getName();

		// If the channel is the Factions channel
		boolean factionChat = channelName.equalsIgnoreCase(MConf.get().ventureChatFactionChannelName);
		boolean allyChat = channelName.equalsIgnoreCase(MConf.get().ventureChatAllyChannelName);
		if (!(factionChat || allyChat)) return;

		MPlayer mplayer = MPlayer.get(player);
		Faction faction = mplayer.getFaction();

		// Wilderness check
		if ( !  MConf.get().ventureChatAllowFactionchatBetweenFactionless && faction.isNone())
		{
			mplayer.msg("<b>Factionless can't use faction chat.");
			event.setCancelled(true);
		}

		Predicate<Player> predicateChannel = factionChat ? EngineChat.getPredicateIsInFaction(faction) : EngineChat.getPredicateIsAlly(faction);
		Predicate<Player> isSpy = recipient -> MineverseChatAPI.getMineverseChatPlayer(recipient).isSpy();
		Predicate<Player> predicate = isSpy.or(predicateChannel);

		EngineChat.filterToPredicate(event, predicate);
	}
	
}
