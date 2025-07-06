package com.massivecraft.factions.integration.placeholderapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.MPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;

import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class PlaceholderFactions extends PlaceholderExpansion implements Relational
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
        return "Ymerejliaf";
    }

    @Override
    public String getVersion()
    {
        return Factions.get().getDescription().getVersion();
    }

    @Override
    public boolean persist()
    {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player1, Player player2, String placeholder)
    {
        if (player1 == null || player2 == null || placeholder == null) return null;

        MPlayer mplayer1 = MPlayer.get(player1);
        MPlayer mplayer2 = MPlayer.get(player2);

        switch (placeholder)
        {
            case "relation":
                return mplayer1.getRelationTo(mplayer2).getName();
            
            case "relation_lowercase":
            case "relation_lower":
                return mplayer1.getRelationTo(mplayer2).getName().toLowerCase();
            
            case "relation_color":
            case "relcolor":
                return mplayer1.getRelationTo(mplayer2).getColor().toString();
        }

        return null;
    }

    @Override
    public String onPlaceholderRequest(Player player, String placeholder)
    {
        if (player == null || placeholder == null) return null;

        MPlayer mplayer = MPlayer.get(player);
        DecimalFormat df = new DecimalFormat("#.##");

        switch (placeholder)
        {
            // - - - - - FACTION PLACEHOLDERS - - - - -
            // If player is not in a faction, don't return faction name
            case "faction_name":
            case "faction":
                if (mplayer.hasFaction())
                {
                    return mplayer.getFaction().getName();
                }
                return "";
            
            // Return a faction name even if the player is not in a faction
            case "faction_nameforce":
            case "factionforce":
                return mplayer.getFaction().getName();
            
            case "faction_power":
            case "factionpower":
                return df.format(mplayer.getFaction().getPower());
            
            case "faction_powermax":
            case "faction_maxpower":
            case "factionpowermax":
            case "factionmaxpower":
                return df.format(mplayer.getFaction().getPowerMax());
            
            case "faction_claims":
            case "claims":
                return Long.toString(BoardColl.get().getAll().stream().mapToInt(board -> board.getCount(mplayer.getFaction())).sum());
            
            case "faction_onlinemembers":
            case "onlinemembers":
                return Integer.toString(mplayer.getFaction().getMPlayersWhereOnlineTo(mplayer).size());
            
            case "faction_allmembers":
            case "allmembers":
                return Integer.toString(mplayer.getFaction().getMPlayers().size());

            // - - - - - PLAYER PLACEHOLDERS - - - - -
            case "player_name":
                return mplayer.getName();

            case "player_power":
            case "power":
                return df.format(mplayer.getPower());
            
            case "player_powermax":
            case "player_maxpower":
            case "powermax":
            case "maxpower":
                return df.format(mplayer.getPowerMax());
            
            case "player_title":
            case "title":
                return mplayer.getTitle();
            
            case "player_role":
            case "player_rank":
            case "role":
            case "rank":
                if (!mplayer.hasFaction())
                {
                    return "";
                }
                return mplayer.getRank().getName();
            
            // return a player rank even if the player is not in a faction
            case "player_roleforce":
            case "player_rankforce":
            case "roleforce":
            case "rankforce":
                return mplayer.getRank().getName();
            
            case "player_roleprefix":
            case "player_rankprefix":
            case "roleprefix":
            case "rankprefix":
                if (!mplayer.hasFaction())
                {
                    return "";
                }
                return mplayer.getRank().getPrefix();
            
            // Return a player rank prefix even if the player is not in a faction
            case "player_roleprefixforce":
            case "player_rankprefixforce":
            case "roleprefixforce":
            case "rankprefixforce":
                return mplayer.getRank().getPrefix();
        }

        return null;
    }

}
