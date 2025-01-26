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
    
    public Faction getFactionAt(FLocation location)
    {
        com.massivecraft.factions.entity.Board realBoard = BoardColl.get().get(location.getLocation().getWorld());
        com.massivecraft.factions.entity.Faction faction = realBoard.getFactionAt(PS.valueOf(location.getLocation()).getChunkCoords(true));
        
        return new LegacyFaction(faction);
    }
    
}
