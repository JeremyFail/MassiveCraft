package com.massivecraft.factions.integration.placeholderapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.MPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class PlaceholderFactions extends PlaceholderExpansion
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static PlaceholderFactions i = new PlaceholderFactions();
	public static PlaceholderFactions get() { return i; }

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public String getIdentifier()
	{
		return "factions";
	}

	@Override
	public String getAuthor()
	{
		return "Madus";
	}

	@Override
	public String getVersion()
	{
		return Factions.get().getDescription().getVersion();
	}

	@Override
	public String onPlaceholderRequest(Player player, String params)
	{
		if (player == null) return null;

		MPlayer mplayer = MPlayer.get(player);
		if ("role".equals(params)) params = "rank";
		DecimalFormat df = new DecimalFormat("#.##");

		switch (params)
		{
			case "faction": return mplayer.getFaction().getName();
			case "power": return df.format(mplayer.getPower());
			case "powermax": return df.format(mplayer.getPowerMax());
			case "factionpower": return df.format(mplayer.getFaction().getPower());
			case "factionpowermax": return df.format(mplayer.getFaction().getPowerMax());
			case "title": return mplayer.getTitle();
			case "rank": return mplayer.getRank().getName();
			case "claims": return Long.toString(BoardColl.get().getAll().stream().mapToInt(board -> board.getCount(mplayer.getFaction())).sum());
			case "onlinemembers": return Integer.toString(mplayer.getFaction().getMPlayersWhereOnlineTo(mplayer).size());
			case "allmembers": return Integer.toString(mplayer.getFaction().getMPlayers().size());
		}
        return null;
    }

}
