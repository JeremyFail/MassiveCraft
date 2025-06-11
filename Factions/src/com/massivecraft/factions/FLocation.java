package com.massivecraft.factions;

import org.bukkit.Location;

/**
 * This class attempts to provide minimal compatibility with Factions V1/FactionsUUID
 * by providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public class FLocation
{
    
    // TODO: Add more methods from legacy factions?

    private Location location;
    
    public FLocation(Location location)
    {
        this.location = location;
    }
    
    public Location getLocation()
    {
        return this.location;
    }
    
}
