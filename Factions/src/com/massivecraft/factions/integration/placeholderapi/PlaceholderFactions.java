package com.massivecraft.factions.integration.placeholderapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.util.PlaceholderProcessor;
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
        // Invalid placeholder
        if (placeholder == null) return null;

        // If either player is null, we will return an empty string for relational placeholders
        boolean isNull = player1 == null || player2 == null;

        // Use PlaceholderProcessor to handle modifiers like |rp, |lp, etc.
        return PlaceholderProcessor.parsePlaceholderWithModifiers(placeholder, basePlaceholder -> {
            switch (basePlaceholder)
            {
                case "relation":
                    return !isNull ? MPlayer.get(player1).getRelationTo(MPlayer.get(player2)).getName() : "";

                case "relation_lowercase":
                case "relation_lower":
                    return !isNull ? MPlayer.get(player1).getRelationTo(MPlayer.get(player2)).getName().toLowerCase() : "";

                case "relation_color":
                case "relcolor":
                    return !isNull ? MPlayer.get(player1).getRelationTo(MPlayer.get(player2)).getColor().toString() : "";

                default:
                    return null; // Unknown placeholder
            }
        });
    }

    @Override
    public String onPlaceholderRequest(Player player, String placeholder)
    {
        // Invalid placeholder
        if (placeholder == null) return null;

        // If player is null, we will return an empty string for faction placeholders
        boolean isNull = player == null;

        // Format for power values
        DecimalFormat df = new DecimalFormat("#.##");

        // Use PlaceholderProcessor to handle modifiers like |rp, |lp, etc.
        return PlaceholderProcessor.parsePlaceholderWithModifiers(placeholder, basePlaceholder -> {
            MPlayer mPlayer;
            switch (basePlaceholder)
            {
                // - - - - - FACTION PLACEHOLDERS - - - - -
                // If player is not in a faction, don't return faction name
                case "faction_name":
                case "faction":
                    if (!isNull && MPlayer.get(player).hasFaction())
                    {
                        return MPlayer.get(player).getFaction().getName();
                    }
                    return "";
                
                // Return a faction name even if the player is not in a faction
                case "faction_nameforce":
                case "factionforce":
                    return !isNull ? MPlayer.get(player).getFaction().getName() : "";

                case "faction_power":
                case "factionpower":
                    return !isNull ? df.format(MPlayer.get(player).getFaction().getPower()) : "";

                case "faction_powermax":
                case "faction_maxpower":
                case "factionpowermax":
                case "factionmaxpower":
                    return !isNull ? df.format(MPlayer.get(player).getFaction().getPowerMax()) : "";
                
                case "faction_claims":
                case "claims":
                    return !isNull 
                        ? Long.toString(BoardColl.get().getAll().stream().mapToInt(board -> board.getCount(MPlayer.get(player).getFaction())).sum()) 
                        : "";

                case "faction_onlinemembers":
                case "onlinemembers":
                    return !isNull ? Integer.toString(MPlayer.get(player).getFaction().getMPlayersWhereOnlineTo(MPlayer.get(player)).size()) : "";

                case "faction_allmembers":
                case "allmembers":
                    return !isNull ? Integer.toString(MPlayer.get(player).getFaction().getMPlayers().size()) : "";

                // - - - - - PLAYER PLACEHOLDERS - - - - -
                case "player_name":
                    return !isNull ? MPlayer.get(player).getName() : "";

                case "player_power":
                case "power":
                    return !isNull ? df.format(MPlayer.get(player).getPower()) : "";

                case "player_powermax":
                case "player_maxpower":
                case "powermax":
                case "maxpower":
                    return !isNull ? df.format(MPlayer.get(player).getPowerMax()) : "";
                
                case "player_title":
                case "title":
                    return !isNull ? MPlayer.get(player).getTitle() : "";

                case "player_rank":
                case "player_role":
                case "rank":
                case "role":
                    if (isNull) return "";
                    mPlayer = MPlayer.get(player);
                    if (!mPlayer.hasFaction())
                    {
                        return "";
                    }
                    return mPlayer.getRank().getName();
                
                // Return a player rank even if the player is not in a faction
                case "player_rankforce":
                case "player_roleforce":
                case "rankforce":
                case "roleforce":
                    return !isNull ? MPlayer.get(player).getRank().getName() : "";

                case "player_rankprefix":
                case "player_roleprefix":
                case "rankprefix":
                case "roleprefix":
                    if (isNull) return "";
                    mPlayer = MPlayer.get(player);
                    if (!mPlayer.hasFaction())
                    {
                        return "";
                    }
                    return mPlayer.getRank().getPrefix();
                
                // Return a player rank prefix even if the player is not in a faction
                case "player_rankprefixforce":
                case "player_roleprefixforce":
                case "rankprefixforce":
                case "roleprefixforce":
                    return !isNull ? MPlayer.get(player).getRank().getPrefix() : "";

                default:
                    return null; // Unknown placeholder
            }
        });
    }

}
