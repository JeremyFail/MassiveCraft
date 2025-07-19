package com.massivecraft.factions;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.massivecore.ps.PS;

/**
 * This class attempts to provide minimal compatibility with Factions V1/FactionsUUID
 * by providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public class Board
{
    
    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //
    
    private static Board instance;
    public static Board getInstance() 
    { 
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage();
        
        if (instance == null) {
            instance = new Board();
        }
        return instance;
    }
    // Private constructor to prevent instantiation
    private Board() {}
    
    // -------------------------------------------- //
    // METHODS
    // -------------------------------------------- //
    
    // TODO: Add more methods from legacy Factions
    public Faction getFactionAt(FLocation location)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage();
        
        com.massivecraft.factions.entity.Board realBoard = BoardColl.get().get(location.getLocation().getWorld());
        com.massivecraft.factions.entity.Faction realFaction = realBoard.getFactionAt(PS.valueOf(location.getLocation()).getChunkCoords(true));

        if (realFaction.getId().equals(Factions.ID_NONE))
        {
            return null;
        }

        return new LegacyFaction(realFaction);
    }
    
}
