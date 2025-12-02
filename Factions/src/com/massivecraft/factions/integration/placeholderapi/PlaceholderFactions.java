package com.massivecraft.factions.integration.placeholderapi;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.massivecore.ps.PS;
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
        if (player1 == null || player2 == null) return "";

        // Use PlaceholderProcessor to handle modifiers like |rp, |lp, etc.
        return PlaceholderProcessor.parsePlaceholderWithModifiers(placeholder, basePlaceholder -> {
            switch (basePlaceholder)
            {
                case "relation":
                    return MPlayer.get(player1).getRelationTo(MPlayer.get(player2)).getName();

                case "relation_lowercase":
                case "relation_lower":
                    return MPlayer.get(player1).getRelationTo(MPlayer.get(player2)).getName().toLowerCase();

                case "relation_color":
                case "relcolor":
                    return MPlayer.get(player1).getRelationTo(MPlayer.get(player2)).getColor().toString();

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
        if (player == null) return "";

        // Get the MPlayer object for the player
        MPlayer mPlayer = MPlayer.get(player);

        // If the MPlayer is null, return an empty string
        if (mPlayer == null) return "";

        // Format for decimal values
        DecimalFormat df = new DecimalFormat("#.##");

        // Use PlaceholderProcessor to handle modifiers like |rp, |lp, etc.
        return PlaceholderProcessor.parsePlaceholderWithModifiers(placeholder, basePlaceholder -> {
            // Variable to hold the faction at the player's location for territory placeholders
            Faction factionAtLocation = basePlaceholder.startsWith("faction_territory_") ? BoardColl.get().getFactionAt(PS.valueOf(player.getLocation())) : null;

            switch (basePlaceholder)
            {
                // - - - - - FACTION PLACEHOLDERS - - - - -
                case "faction_internal_id":
                    return mPlayer.hasFaction() ? mPlayer.getFaction().getId() : "";

                // If player is not in a faction, don't return faction name
                case "faction_name":
                case "faction":
                    if (mPlayer.hasFaction())
                    {
                        return mPlayer.getFaction().getName();
                    }
                    return "";
                
                // Return a faction name even if the player is not in a faction
                case "faction_nameforce":
                case "factionforce":
                    return mPlayer.getFaction().getName();

                case "faction_description":
                    return mPlayer.hasFaction() ? mPlayer.getFaction().getDescription() : "";

                // Return a faction description even if the player is not in a faction
                case "faction_descriptionforce":
                    return mPlayer.getFaction().getDescription();

                case "faction_power":
                case "factionpower":
                    return df.format(mPlayer.getFaction().getPower());

                case "faction_powermax":
                case "faction_maxpower":
                case "factionpowermax":
                case "factionmaxpower":
                    return df.format(mPlayer.getFaction().getPowerMax());

                case "faction_powerboost":
                    return df.format(mPlayer.getFaction().getPowerBoost());

                case "faction_money_balance":
                case "faction_bank_balance":
                    if (mPlayer.hasFaction() && Econ.isEnabled())
                    {
                        return df.format(Econ.getMoney(mPlayer.getFaction()));
                    }
                    return "";

                case "faction_leader":
                    if (mPlayer.hasFaction())
                    {
                        return mPlayer.getFaction().getLeader().getName();
                    }
                    return "";

                case "faction_founded":
                case "faction_created":
                    if (mPlayer.hasFaction())
                    {
                        return mPlayer.getFaction().getCreatedDateString();
                    }
                    return "";

                case "faction_peaceful":
                    if (mPlayer.hasFaction())
                    {
                        return String.valueOf(mPlayer.getFaction().getFlag(MFlag.ID_PEACEFUL));
                    }
                    return "";
                
                case "faction_warps":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getWarps().size());
                    }
                    return "";
                
                // The following four placeholders return information about the player's faction home warp
                case "faction_home_formatted":
                    if (mPlayer.hasFaction())
                    {
                        Warp homeWarp = mPlayer.getFaction().getWarp(MConf.get().warpsHomeName);
                        if (homeWarp != null && homeWarp.getLocation() != null)
                        {
                            return String.format("%s (%d, %d, %d)", 
                                    homeWarp.getLocation().getWorld(), 
                                    homeWarp.getLocation().getBlockX(), 
                                    homeWarp.getLocation().getBlockY(), 
                                    homeWarp.getLocation().getBlockZ());
                        }
                    }

                case "faction_home_world":
                    if (mPlayer.hasFaction())
                    {
                        Warp homeWarp = mPlayer.getFaction().getWarp(MConf.get().warpsHomeName);
                        if (homeWarp != null && homeWarp.getLocation() != null)
                        {
                            return homeWarp.getLocation().getWorld();
                        }
                    }
                    return "";
                
                case "faction_home_x":
                    if (mPlayer.hasFaction())
                    {
                        Warp homeWarp = mPlayer.getFaction().getWarp(MConf.get().warpsHomeName);
                        if (homeWarp != null && homeWarp.getLocation() != null)
                        {
                            return Integer.toString(homeWarp.getLocation().getBlockX());
                        }
                    }
                    return "";
                
                case "faction_home_y":
                    if (mPlayer.hasFaction())
                    {
                        Warp homeWarp = mPlayer.getFaction().getWarp(MConf.get().warpsHomeName);
                        if (homeWarp != null && homeWarp.getLocation() != null)
                        {
                            return Integer.toString(homeWarp.getLocation().getBlockY());
                        }
                    }
                    return "";

                case "faction_home_z":
                    if (mPlayer.hasFaction())
                    {
                        Warp homeWarp = mPlayer.getFaction().getWarp(MConf.get().warpsHomeName);
                        if (homeWarp != null && homeWarp.getLocation() != null)
                        {
                            return Integer.toString(homeWarp.getLocation().getBlockZ());
                        }
                    }
                    return "";
                
                case "faction_claims":
                case "claims":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getLandCount());
                    }

                case "faction_onlinemembers":
                case "onlinemembers":
                    return Integer.toString(mPlayer.getFaction().getMPlayersWhereOnlineTo(mPlayer).size());

                case "faction_offlinemembers":
                case "offlinemembers":
                    return Integer.toString(mPlayer.getFaction().getMPlayers().size() - mPlayer.getFaction().getMPlayersWhereOnlineTo(mPlayer).size());

                case "faction_allmembers":
                case "allmembers":
                case "faction_size":
                    return Integer.toString(mPlayer.getFaction().getMPlayers().size());

                case "faction_allies":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ALLY).size());
                    }
                    return "";
                
                case "faction_allies_players":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ALLY).stream()
                                .mapToInt(faction -> faction.getMPlayers().size())
                                .sum());
                    }
                    return "";
                
                case "faction_allies_players_online":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ALLY).stream()
                                .mapToInt(faction -> faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                case "faction_allies_players_offline":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ALLY).stream()
                                .mapToInt(faction -> faction.getMPlayers().size() - faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                case "faction_enemies":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ENEMY).size());
                    }
                    return "";

                case "faction_enemies_players":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ENEMY).stream()
                                .mapToInt(faction -> faction.getMPlayers().size())
                                .sum());
                    }
                    return "";

                case "faction_enemies_players_online":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ENEMY).stream()
                                .mapToInt(faction -> faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";
                
                case "faction_enemies_players_offline":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ENEMY).stream()
                                .mapToInt(faction -> faction.getMPlayers().size() - faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                case "faction_truces":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.TRUCE).size());
                    }
                    return "";

                case "faction_truces_players":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.TRUCE).stream()
                                .mapToInt(faction -> faction.getMPlayers().size())
                                .sum());
                    }
                    return "";

                case "faction_truces_players_online":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.TRUCE).stream()
                                .mapToInt(faction -> faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                case "faction_truces_players_offline":
                    if (mPlayer.hasFaction())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.TRUCE).stream()
                                .mapToInt(faction -> faction.getMPlayers().size() - faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                // - - - - - FACTION TERRITORY PLACEHOLDERS - - - - -
                // These return the faction at the player's current location
                case "faction_territory_internal_id":
                    return factionAtLocation.getId();

                case "faction_territory_name":
                    return factionAtLocation.getName();

                case "faction_territory_description":
                    return factionAtLocation.getDescription();

                case "faction_territory_power":
                    return df.format(factionAtLocation.getPower());

                case "faction_territory_powermax":
                case "faction_territory_maxpower":
                    return df.format(factionAtLocation.getPowerMax());

                case "faction_territory_powerboost":
                    return df.format(factionAtLocation.getPowerBoost());

                case "faction_territory_money_balance":
                case "faction_territory_bank_balance":
                    if (Econ.isEnabled())
                    {
                        return df.format(Econ.getMoney(factionAtLocation));
                    }
                    return "";

                case "faction_territory_leader":
                    if (!factionAtLocation.isNone())
                    {
                        return factionAtLocation.getLeader().getName();
                    }
                    return "";

                case "faction_territory_founded":
                case "faction_territory_created":
                    if (factionAtLocation.isNormal())
                    {
                        return factionAtLocation.getCreatedDateString();
                    }
                    return "";
                
                case "faction_territory_peaceful":
                    return String.valueOf(factionAtLocation.getFlag(MFlag.ID_PEACEFUL));
                
                case "faction_territory_warps":
                    return Integer.toString(factionAtLocation.getWarps().size());

                case "faction_territory_relation_color":
                case "faction_territory_relcolor":
                    return mPlayer.getRelationTo(factionAtLocation).getColor().toString();

                case "faction_territory_claims":
                    return Integer.toString(factionAtLocation.getLandCount());

                case "faction_territory_onlinemembers":
                    return Integer.toString(factionAtLocation.getMPlayersWhereOnlineTo(mPlayer).size());

                case "faction_territory_offlinemembers":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(factionAtLocation.getMPlayers().size() - factionAtLocation.getMPlayersWhereOnlineTo(mPlayer).size());
                    }
                    return "0";

                case "faction_territory_allmembers":
                case "faction_territory_size":
                    return Integer.toString(factionAtLocation.getMPlayers().size());

                case "faction_territory_allies":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ALLY).size());
                    }
                    return "";
                
                case "faction_territory_allies_players":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ALLY).stream()
                                .mapToInt(faction -> faction.getMPlayers().size())
                                .sum());
                    }
                    return "";
                
                case "faction_territory_allies_players_online":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ALLY).stream()
                                .mapToInt(faction -> faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                case "faction_territory_allies_players_offline":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ALLY).stream()
                                .mapToInt(faction -> faction.getMPlayers().size() - faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                case "faction_territory_enemies":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ENEMY).size());
                    }
                    return "";

                case "faction_territory_enemies_players":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ENEMY).stream()
                                .mapToInt(faction -> faction.getMPlayers().size())
                                .sum());
                    }
                    return "";

                case "faction_territory_enemies_players_online":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ENEMY).stream()
                                .mapToInt(faction -> faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";
                
                case "faction_territory_enemies_players_offline":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.ENEMY).stream()
                                .mapToInt(faction -> faction.getMPlayers().size() - faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                case "faction_territory_truces":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.TRUCE).size());
                    }
                    return "";

                case "faction_territory_truces_players":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.TRUCE).stream()
                                .mapToInt(faction -> faction.getMPlayers().size())
                                .sum());
                    }
                    return "";

                case "faction_territory_truces_players_online":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.TRUCE).stream()
                                .mapToInt(faction -> faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                case "faction_territory_truces_players_offline":
                    if (!factionAtLocation.isNone())
                    {
                        return Integer.toString(mPlayer.getFaction().getRelatedFactions(Rel.TRUCE).stream()
                                .mapToInt(faction -> faction.getMPlayers().size() - faction.getMPlayersWhereOnlineTo(mPlayer).size())
                                .sum());
                    }
                    return "";

                // - - - - - PLAYER PLACEHOLDERS - - - - -
                case "player_name":
                    return mPlayer.getName();

                case "player_power":
                case "power":
                    return df.format(mPlayer.getPower());

                case "player_powermax":
                case "player_maxpower":
                case "powermax":
                case "maxpower":
                    return df.format(mPlayer.getPowerMax());

                case "player_title":
                case "title":
                    return mPlayer.getTitle();

                case "player_rank":
                case "player_role":
                case "rank":
                case "role":
                    if (mPlayer.hasFaction())
                    {
                        return mPlayer.getRank().getName();
                    }
                    return "";

                // Return a player rank even if the player is not in a faction
                case "player_rankforce":
                case "player_roleforce":
                case "rankforce":
                case "roleforce":
                    return MPlayer.get(player).getRank().getName();

                case "player_rankprefix":
                case "player_roleprefix":
                case "rankprefix":
                case "roleprefix":
                    if (mPlayer.hasFaction())
                    {
                        return mPlayer.getRank().getPrefix();
                    }
                    return "";

                // Return a player rank prefix even if the player is not in a faction
                case "player_rankprefixforce":
                case "player_roleprefixforce":
                case "rankprefixforce":
                case "roleprefixforce":
                    return mPlayer.getRank().getPrefix();

                // Unknown placeholder
                default:
                    return null;
            }
        });
    }

}
