package com.massivecraft.factions;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * This class attempts to provide minimal compatibility with Factions V1/FactionsUUID
 * by providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public class FPlayers
{
    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //
    
    private static FPlayers instance;
    public static FPlayers getInstance() 
    { 
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage();
        
        if (instance == null) {
            instance = new FPlayers();
        }
        return instance;
    }
    // Private constructor to prevent instantiation
    private FPlayers() {}
    
    // -------------------------------------------- //
    // METHODS
    // -------------------------------------------- //

    public Collection<FPlayer> getOnlinePlayers()
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        Collection<FPlayer> onlinePlayers = new ArrayList<>();
        for (Player player : Bukkit.getServer().getOnlinePlayers())
        {
            onlinePlayers.add(getByPlayer(player));
        }
        return onlinePlayers;
    }

    public Collection<FPlayer> getAllFPlayers()
    {
        return getOnlinePlayers();
    }

    public FPlayer getByPlayer(Player player)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        return new LegacyFPlayer(player);
    }

    public FPlayer getByOfflinePlayer(OfflinePlayer player)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        return new LegacyFPlayer(player);
    }
    
}
