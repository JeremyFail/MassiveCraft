package dev.kitteh.factions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.massivecraft.factions.LegacyApiWarningManager;

/**
 * This class attempts to provide minimal compatibility with FactionsUUID by
 * providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public class FPlayers
{
    // TODO: Remove methods that were removed from the legacy API

    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //
    
    private static FPlayers instance;
    public static FPlayers getInstance() 
    { 
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        if (instance == null) {
            instance = new FPlayers();
        }
        return instance;
    }

    public static FPlayers fPlayers()
    {
        return getInstance();
    }

    // Private constructor to prevent instantiation
    private FPlayers() {}
    
    // -------------------------------------------- //
    // METHODS
    // -------------------------------------------- //

    public FPlayer getByPlayer(Player player)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        return new LegacyFPlayer(player);
    }

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

    public Collection<FPlayer> online()
    {
        return getOnlinePlayers();
    }

    public Collection<FPlayer> all()
    {
        return online();
    }

    public FPlayer get(OfflinePlayer player)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        return new LegacyFPlayer(player);
    }

    public FPlayer get(UUID uuid)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        return new LegacyFPlayer(Bukkit.getPlayer(uuid));
    }

    public boolean has(UUID uuid)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        return new LegacyFPlayer(Bukkit.getPlayer(uuid)) != null;
    }
    
    public FPlayer getByOfflinePlayer(OfflinePlayer player)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        return new LegacyFPlayer(player);
    }
    
}
