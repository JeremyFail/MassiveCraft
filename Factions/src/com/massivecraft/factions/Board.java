package com.massivecraft.factions;

import java.util.HashSet;
import java.util.Set;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.FactionColl;
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

    public void setFactionAt(FLocation location, Faction faction)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage();
        
        com.massivecraft.factions.entity.Board realBoard = BoardColl.get().get(location.getLocation().getWorld());
        com.massivecraft.factions.entity.Faction realFaction = (faction == null) ? FactionColl.get().getNone() : FactionColl.get().get(faction.getId());
        
        realBoard.setFactionAt(PS.valueOf(location.getLocation()).getChunkCoords(true), realFaction);
    }

    public void removeAt(FLocation location)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage();

        setFactionAt(location, null);
    }

    public Set<FLocation> getAllClaims(String factionId)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage();
        
        Set<FLocation> ret = new HashSet<>();

        com.massivecraft.factions.entity.Faction realFaction = FactionColl.get().get(factionId);
        if (realFaction == null || realFaction.getId().equals(Factions.ID_NONE))
        {
            return ret;
        }

        for (com.massivecraft.factions.entity.Board board : BoardColl.get().getAll())
        {
            for (PS chunk : board.getChunks(realFaction))
            {
                ret.add(new FLocation(chunk.getLocation().asBukkitLocation()));
            }
        }
        return ret;
    }

    public Set<FLocation> getAllClaims(Faction faction)
    {
        return getAllClaims(faction.getId());
    }

    // TODO: Add more methods from legacy Factions
    
    // public void unclaimAll(String factionId);

    // public void unclaimAllInWorld(String factionId, World world);

    // Is this coord NOT completely surrounded by coords claimed by the same faction?
    // Simpler: Is there any nearby coord with a faction other than the faction here?
    // public boolean isBorderLocation(FLocation flocation)
    // {
    //     Faction faction = factionAt(flocation);
    //     FLocation a = flocation.relative(1, 0);
    //     FLocation b = flocation.relative(-1, 0);
    //     FLocation c = flocation.relative(0, 1);
    //     FLocation d = flocation.relative(0, -1);
    //     return faction != factionAt(a) || faction != factionAt(b) || faction != factionAt(c) || faction != factionAt(d);
    // }

    // Is this coord connected to any coord claimed by the specified faction?
    // public boolean isConnectedLocation(FLocation flocation, Faction faction);

    // public boolean hasFactionWithin(FLocation flocation, Faction faction, int radius);

    //----------------------------------------------//
    // Cleaner. Remove orphaned foreign keys
    //----------------------------------------------//

    // public void clean();

    //----------------------------------------------//
    // Coord count
    //----------------------------------------------//

    // public int getFactionCoordCount(String factionId);

    // public int getFactionCoordCount(Faction faction);

    // public int getFactionCoordCountInWorld(Faction faction, String worldName);

    //----------------------------------------------//
    // Map generation
    //----------------------------------------------//

    /**
     * The map is relative to a coord and a faction north is in the direction of decreasing x east is in the direction
     * of decreasing z
     */
    // public List<Component> getMap(FPlayer fPlayer, FLocation flocation, double inDegrees);

    // public void forceSave();

    // public void forceSave(boolean sync);

    // public int load();

}
