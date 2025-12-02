package dev.kitteh.factions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.factions.LegacyApiWarningManager;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.FactionColl;


/**
 * This class attempts to provide minimal compatibility with FactionsUUID by
 * providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public class Factions
{
    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //

    private static Factions instance;
    public static Factions factions() 
    { 
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        if (instance == null) {
            instance = new Factions();
        }
        return instance;
    }
    // Private constructor to prevent instantiation
    private Factions() {};

    // -------------------------------------------- //
    // METHODS
    // -------------------------------------------- //

    public Faction get(int id)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        return new LegacyFaction(com.massivecraft.factions.entity.FactionColl.get().get(String.valueOf(id)));
    }

    public Faction get(String tag)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);

        return new LegacyFaction(com.massivecraft.factions.entity.FactionColl.get().getByName(tag));
    }

    public Faction getAt(Location location)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);

        BoardColl.get().getFactionAt(PS.valueOf(location));
        return new FLocation(location).faction();
    }

    public Faction getAt(FLocation flocation)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);

        return flocation.faction();
    }

    // TODO: Implement?
    // public Faction create(String tag) {
    //     return this.create(null, tag);
    // }

    // public Faction create(@Nullable FPlayer sender, String tag);

    // public void remove(Faction faction);

    public List<Faction> all()
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);

		List<Faction> legacyFactions = new ArrayList<>();
		for (com.massivecraft.factions.entity.Faction faction : FactionColl.get().getAll())
		{
			legacyFactions.add(new LegacyFaction(faction));
		}
		return legacyFactions;
    }

    public Faction wilderness()
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);

        return new LegacyFaction(FactionColl.get().getNone());
    }

    public Faction safeZone()
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        return new LegacyFaction(FactionColl.get().getSafezone());
    }

    public Faction warZone()
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);

        return new LegacyFaction(FactionColl.get().getWarzone());
    }
    
}
