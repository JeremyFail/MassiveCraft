package com.massivecraft.factionschat.util;

import com.massivecraft.factionschat.FactionsChat;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages player ignore lists for FactionsChat.
 * Handles loading/saving ignore data and provides methods for managing ignore relationships.
 * Features smart caching with lazy loading and automatic cleanup of inactive entries.
 */
public class IgnoreManager
{
    
    private static final String IGNORES_FILE_NAME = "ignores.yml";
    private static final long CACHE_CLEANUP_INTERVAL_MINUTES = 2; // How often to run cleanup
    private static final long CACHE_EXPIRY_MINUTES = 5; // How long to keep inactive entries
    
    /**
     * Wrapper class to track ignore data with last access time for cleanup purposes.
     */
    private static class CachedIgnoreData 
    {
        private final Set<UUID> ignoredPlayers;
        private volatile long lastAccessTime;
        private volatile boolean isDirty; // True if data has been modified and needs saving
        
        /**
         * Creates a new CachedIgnoreData instance.
         * 
         * @param ignoredPlayers The set of players that are ignored.
         */
        public CachedIgnoreData(Set<UUID> ignoredPlayers)
        {
            this.ignoredPlayers = ignoredPlayers;
            this.lastAccessTime = System.currentTimeMillis();
            this.isDirty = false;
        }
        
        /**
         * Gets the set of ignored players.
         * 
         * @return The set of ignored player UUIDs.
         */
        public Set<UUID> getIgnoredPlayers()
        {
            this.lastAccessTime = System.currentTimeMillis();
            return this.ignoredPlayers;
        }
        
        /**
         * Marks this data as dirty (modified).
         */
        public void markDirty()
        {
            this.isDirty = true;
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        /**
         * Checks if this data is dirty (modified).
         * 
         * @return True if the data has been modified since last save.
         */
        public boolean isDirty()
        {
            return this.isDirty;
        }
        
        /**
         * Marks this data as clean (saved).
         */
        public void markClean()
        {
            this.isDirty = false;
        }
        
        /**
         * Checks if this data has expired based on last access time.
         * 
         * @param currentTime The current system time in milliseconds.
         * @return True if the data has expired and should be cleaned up.
         */
        public boolean isExpired(long currentTime)
        {
            return (currentTime - this.lastAccessTime) > (CACHE_EXPIRY_MINUTES * 60 * 1000);
        }
    }
    
    // Smart cache: Key = player UUID, Value = cached ignore data with metadata
    private final Map<UUID, CachedIgnoreData> ignoreCache = new ConcurrentHashMap<>();
    
    // Cleanup task scheduler
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    
    private final FactionsChat plugin;
    
    public IgnoreManager(FactionsChat plugin)
    {
        this.plugin = plugin;
        
        // Start cleanup task to periodically remove expired cache entries
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 
            CACHE_CLEANUP_INTERVAL_MINUTES, CACHE_CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * Shuts down the cleanup scheduler. Should be called on plugin disable.
     */
    public void shutdown()
    {
        cleanupScheduler.shutdown();
        try
        {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS))
            {
                cleanupScheduler.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Gets ignore data for a player, loading from disk if not in cache.
     * This is the smart wrapper that ensures we always have the most current data.
     * 
     * @param playerUuid The UUID of the player
     * @return CachedIgnoreData for the player, never null
     */
    private CachedIgnoreData getOrLoadIgnoreData(UUID playerUuid)
    {
        CachedIgnoreData cached = ignoreCache.get(playerUuid);
        if (cached != null)
        {
            return cached;
        }
        
        // Not in cache, load from disk
        Set<UUID> ignoredSet = loadIgnoreDataFromDisk(playerUuid);
        cached = new CachedIgnoreData(ignoredSet);
        ignoreCache.put(playerUuid, cached);
        
        return cached;
    }
    
    /**
     * Loads ignore data from disk for a specific player.
     * 
     * @param playerUuid The UUID of the player
     * @return Set of ignored player UUIDs, empty set if none
     */
    private Set<UUID> loadIgnoreDataFromDisk(UUID playerUuid)
    {
        File ignoresFile = new File(plugin.getDataFolder(), IGNORES_FILE_NAME);
        
        if (!ignoresFile.exists())
        {
            return new HashSet<>();
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(ignoresFile);
        List<String> ignoredUuids = config.getStringList(playerUuid.toString());
        
        Set<UUID> ignoredSet = new HashSet<>();
        for (String uuidString : ignoredUuids)
        {
            try
            {
                ignoredSet.add(UUID.fromString(uuidString));
            }
            catch (IllegalArgumentException e)
            {
                plugin.getLogger().warning("Invalid UUID in ignores.yml: " + uuidString);
            }
        }
        
        return ignoredSet;
    }
    
    /**
     * Saves a specific player's ignore data to disk.
     * 
     * @param playerUuid The player's UUID
     * @param ignoredPlayers The set of ignored players
     */
    private void saveIgnoreDataToDisk(UUID playerUuid, Set<UUID> ignoredPlayers)
    {
        File ignoresFile = new File(plugin.getDataFolder(), IGNORES_FILE_NAME);
        if (!plugin.getDataFolder().exists())
        {
            plugin.getDataFolder().mkdirs();
        }
        
        YamlConfiguration config;
        if (ignoresFile.exists())
        {
            config = YamlConfiguration.loadConfiguration(ignoresFile);
        }
        else
        {
            config = new YamlConfiguration();
        }
        
        if (ignoredPlayers.isEmpty())
        {
            // Remove empty entries
            config.set(playerUuid.toString(), null);
        }
        else
        {
            List<String> ignoredUuidStrings = ignoredPlayers.stream()
                .map(UUID::toString)
                .toList();
            config.set(playerUuid.toString(), ignoredUuidStrings);
        }
        
        try
        {
            config.save(ignoresFile);
        }
        catch (IOException e)
        {
            plugin.getLogger().severe("Could not save ignores.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cleanup task that removes expired entries from cache and saves dirty data.
     */
    private void cleanupExpiredEntries()
    {
        long currentTime = System.currentTimeMillis();
        
        ignoreCache.entrySet().removeIf(entry -> {
            UUID playerUuid = entry.getKey();
            CachedIgnoreData cached = entry.getValue();
            
            if (cached.isExpired(currentTime))
            {
                // Save dirty data before removing from cache
                if (cached.isDirty())
                {
                    saveIgnoreDataToDisk(playerUuid, cached.ignoredPlayers);
                }
                return true; // Remove from cache
            }
            return false; // Keep in cache
        });
    }
    
    /**
     * Loads ignore data for a player when they join the server.
     * This now just ensures the data is loaded into cache - the heavy lifting is done by getOrLoadIgnoreData.
     * 
     * @param playerUuid The UUID of the player joining
     */
    public void loadPlayerIgnores(UUID playerUuid)
    {
        // Simply accessing the data will load it into cache if needed
        getOrLoadIgnoreData(playerUuid);
    }
    
    /**
     * Saves ignore data for a player when they leave the server and removes them from memory.
     * With the new cache system, we save immediately and remove from cache.
     * 
     * @param playerUuid The UUID of the player leaving
     */
    public void saveAndUnloadPlayerIgnores(UUID playerUuid)
    {
        CachedIgnoreData cached = ignoreCache.remove(playerUuid);
        if (cached != null)
        {
            // Always save when player leaves, regardless of dirty flag
            saveIgnoreDataToDisk(playerUuid, cached.ignoredPlayers);
        }
    }
    
    /**
     * Adds a player to another player's ignore list.
     * 
     * @param ignoringPlayer The player who is ignoring
     * @param ignoredPlayer The player being ignored
     */
    public void addIgnore(UUID ignoringPlayer, UUID ignoredPlayer)
    {
        CachedIgnoreData cached = getOrLoadIgnoreData(ignoringPlayer);
        cached.getIgnoredPlayers().add(ignoredPlayer);
        cached.markDirty();
    }
    
    /**
     * Removes a player from another player's ignore list.
     * 
     * @param ignoringPlayer The player who is ignoring
     * @param ignoredPlayer The player to unignore
     * @return true if the player was removed, false if they weren't on the ignore list
     */
    public boolean removeIgnore(UUID ignoringPlayer, UUID ignoredPlayer)
    {
        CachedIgnoreData cached = getOrLoadIgnoreData(ignoringPlayer);
        boolean removed = cached.getIgnoredPlayers().remove(ignoredPlayer);
        if (removed)
        {
            cached.markDirty();
        }
        return removed;
    }
    
    /**
     * Checks if a player is ignoring another player.
     * 
     * @param receivingPlayer The player who might be ignoring
     * @param sendingPlayer The player who might be ignored
     * @return true if receivingPlayer is ignoring sendingPlayer
     */
    public boolean isIgnoring(UUID receivingPlayer, UUID sendingPlayer)
    {
        CachedIgnoreData cached = getOrLoadIgnoreData(receivingPlayer);
        return cached.getIgnoredPlayers().contains(sendingPlayer);
    }
    
    /**
     * Gets the list of players that a player is ignoring.
     * This will load the data from disk if not in cache, making it safe for admin operations on offline players.
     * 
     * @param player The player to get the ignore list for
     * @return An unmodifiable set of ignored player UUIDs, or empty set if none
     */
    public Set<UUID> getIgnoredPlayers(UUID player)
    {
        CachedIgnoreData cached = getOrLoadIgnoreData(player);
        return Collections.unmodifiableSet(cached.getIgnoredPlayers());
    }
    
    /**
     * Gets a player by name or UUID string, handling both online and offline players.
     * 
     * @param nameOrUuid Player name or UUID string
     * @return OfflinePlayer instance, or null if not found
     */
    @SuppressWarnings("deprecation")
    public OfflinePlayer getPlayerByNameOrUuid(String nameOrUuid)
    {
        // Try UUID first
        try
        {
            UUID uuid = UUID.fromString(nameOrUuid);
            return Bukkit.getOfflinePlayer(uuid);
        }
        catch (IllegalArgumentException e)
        {
            // Not a UUID, try as name
            return Bukkit.getOfflinePlayer(nameOrUuid);
        }
    }
    
    /**
     * Saves all current ignore data to file.
     * Used for plugin shutdown or manual saves.
     */
    public void saveAllIgnoreData()
    {
        for (Map.Entry<UUID, CachedIgnoreData> entry : ignoreCache.entrySet())
        {
            UUID playerUuid = entry.getKey();
            CachedIgnoreData cached = entry.getValue();
            
            // Save all cached data, marking it as clean
            saveIgnoreDataToDisk(playerUuid, cached.ignoredPlayers);
            cached.markClean();
        }
    }
}