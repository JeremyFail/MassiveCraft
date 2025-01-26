package com.massivecraft.factions;

import org.bukkit.OfflinePlayer;

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
    
    public FPlayer getByOfflinePlayer(OfflinePlayer player)
    {
        return new LegacyFPlayer(player);
    }
    
}
