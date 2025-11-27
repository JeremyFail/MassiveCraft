package com.massivecraft.factionschat.util;

import com.massivecraft.factionschat.ChatMode;
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
 * Manages disabled chat modes for players in FactionsChat.
 * Handles loading/saving disabled chat mode data and provides methods for managing disabled chat relationships.
 * Features smart caching with lazy loading and automatic cleanup of inactive entries.
 */
public class DisabledChatManager
{
    
    private static final String DISABLED_CHATS_FILE_NAME = "disabled-chats.yml";
    private static final long CACHE_CLEANUP_INTERVAL_MINUTES = 2; // How often to run cleanup
    private static final long CACHE_EXPIRY_MINUTES = 5; // How long to keep inactive entries
    
    /**
     * Wrapper class to track disabled chat data with last access time for cleanup purposes.
     */
    private static class CachedDisabledChatData
    {
        private final Set<ChatMode> disabledChatModes;
        private volatile long lastAccessTime;
        private volatile boolean isDirty; // True if data has been modified and needs saving
        
        public CachedDisabledChatData(Set<ChatMode> disabledChatModes)
        {
            this.disabledChatModes = disabledChatModes;
            this.lastAccessTime = System.currentTimeMillis();
            this.isDirty = false;
        }
        
        public Set<ChatMode> getDisabledChatModes()
        {
            this.lastAccessTime = System.currentTimeMillis();
            return this.disabledChatModes;
        }
        
        public void markDirty()
        {
            this.isDirty = true;
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public boolean isDirty()
        {
            return this.isDirty;
        }
        
        public void markClean()
        {
            this.isDirty = false;
        }
        
        public boolean isExpired(long currentTime)
        {
            return (currentTime - this.lastAccessTime) > (CACHE_EXPIRY_MINUTES * 60 * 1000);
        }
    }
    
    // Smart cache: Key = player UUID, Value = cached disabled chat data with metadata
    private final Map<UUID, CachedDisabledChatData> disabledChatCache = new ConcurrentHashMap<>();
    
    // Cleanup task scheduler
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    
    private final FactionsChat plugin;
    
    public DisabledChatManager(FactionsChat plugin)
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
     * Gets disabled chat data for a player, loading from disk if not in cache.
     * This is the smart wrapper that ensures we always have the most current data.
     * 
     * @param playerUuid The UUID of the player
     * @return CachedDisabledChatData for the player, never null
     */
    private CachedDisabledChatData getOrLoadDisabledChatData(UUID playerUuid)
    {
        CachedDisabledChatData cached = disabledChatCache.get(playerUuid);
        if (cached != null)
        {
            return cached;
        }
        
        // Not in cache, load from disk
        Set<ChatMode> disabledSet = loadDisabledChatDataFromDisk(playerUuid);
        cached = new CachedDisabledChatData(disabledSet);
        disabledChatCache.put(playerUuid, cached);
        
        return cached;
    }
    
    /**
     * Loads disabled chat data from disk for a specific player.
     * 
     * @param playerUuid The UUID of the player
     * @return Set of disabled ChatModes, empty set if none
     */
    private Set<ChatMode> loadDisabledChatDataFromDisk(UUID playerUuid)
    {
        File disabledChatsFile = new File(plugin.getDataFolder(), DISABLED_CHATS_FILE_NAME);
        
        if (!disabledChatsFile.exists())
        {
            return new HashSet<>();
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(disabledChatsFile);
        List<String> disabledChatNames = config.getStringList(playerUuid.toString());
        
        Set<ChatMode> disabledSet = new HashSet<>();
        for (String chatModeName : disabledChatNames)
        {
            try
            {
                ChatMode chatMode = ChatMode.getChatModeByName(chatModeName);
                if (chatMode != null)
                {
                    disabledSet.add(chatMode);
                }
                else
                {
                    plugin.getLogger().warning("Invalid ChatMode in disabled-chats.yml: " + chatModeName);
                }
            }
            catch (IllegalArgumentException e)
            {
                plugin.getLogger().warning("Invalid ChatMode in disabled-chats.yml: " + chatModeName);
            }
        }
        
        return disabledSet;
    }
    
    /**
     * Saves a specific player's disabled chat data to disk.
     * 
     * @param playerUuid The player's UUID
     * @param disabledChatModes The set of disabled chat modes
     */
    private void saveDisabledChatDataToDisk(UUID playerUuid, Set<ChatMode> disabledChatModes)
    {
        File disabledChatsFile = new File(plugin.getDataFolder(), DISABLED_CHATS_FILE_NAME);
        if (!plugin.getDataFolder().exists())
        {
            plugin.getDataFolder().mkdirs();
        }
        
        YamlConfiguration config;
        if (disabledChatsFile.exists())
        {
            config = YamlConfiguration.loadConfiguration(disabledChatsFile);
        }
        else
        {
            config = new YamlConfiguration();
        }
        
        if (disabledChatModes.isEmpty())
        {
            // Remove empty entries
            config.set(playerUuid.toString(), null);
        }
        else
        {
            List<String> disabledChatModeNames = disabledChatModes.stream()
                .map(ChatMode::name)
                .toList();
            config.set(playerUuid.toString(), disabledChatModeNames);
        }
        
        try
        {
            config.save(disabledChatsFile);
        }
        catch (IOException e)
        {
            plugin.getLogger().severe("Could not save disabled-chats.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cleanup task that removes expired entries from cache and saves dirty data.
     */
    private void cleanupExpiredEntries()
    {
        long currentTime = System.currentTimeMillis();
        
        disabledChatCache.entrySet().removeIf(entry -> {
            UUID playerUuid = entry.getKey();
            CachedDisabledChatData cached = entry.getValue();
            
            if (cached.isExpired(currentTime))
            {
                // Save dirty data before removing from cache
                if (cached.isDirty())
                {
                    saveDisabledChatDataToDisk(playerUuid, cached.disabledChatModes);
                }
                return true; // Remove from cache
            }
            return false; // Keep in cache
        });
    }
    
    /**
     * Loads disabled chat data for a player when they join the server.
     * This now just ensures the data is loaded into cache - the heavy lifting is done by getOrLoadDisabledChatData.
     * 
     * @param playerUuid The UUID of the player joining
     */
    public void loadPlayerDisabledChats(UUID playerUuid)
    {
        // Simply accessing the data will load it into cache if needed
        getOrLoadDisabledChatData(playerUuid);
    }
    
    /**
     * Saves disabled chat data for a player when they leave the server and removes them from memory.
     * With the new cache system, we save immediately and remove from cache.
     * 
     * @param playerUuid The UUID of the player leaving
     */
    public void saveAndUnloadPlayerDisabledChats(UUID playerUuid)
    {
        CachedDisabledChatData cached = disabledChatCache.remove(playerUuid);
        if (cached != null)
        {
            // Always save when player leaves, regardless of dirty flag
            saveDisabledChatDataToDisk(playerUuid, cached.disabledChatModes);
        }
    }
    
    /**
     * Toggles a chat mode for a player (enables if disabled, disables if enabled).
     * 
     * @param playerUuid The player whose chat mode to toggle
     * @param chatMode The chat mode to toggle
     * @return true if the chat mode is now disabled, false if now enabled
     */
    public boolean toggleChatMode(UUID playerUuid, ChatMode chatMode)
    {
        CachedDisabledChatData cached = getOrLoadDisabledChatData(playerUuid);
        Set<ChatMode> disabledModes = cached.getDisabledChatModes();
        
        boolean isNowDisabled;
        if (disabledModes.contains(chatMode))
        {
            disabledModes.remove(chatMode);
            isNowDisabled = false;
        }
        else
        {
            disabledModes.add(chatMode);
            isNowDisabled = true;
        }
        
        cached.markDirty();
        return isNowDisabled;
    }
    
    /**
     * Checks if a player has disabled a specific chat mode.
     * 
     * @param playerUuid The player to check
     * @param chatMode The chat mode to check
     * @return true if the chat mode is disabled for this player
     */
    public boolean isChatModeDisabled(UUID playerUuid, ChatMode chatMode)
    {
        CachedDisabledChatData cached = getOrLoadDisabledChatData(playerUuid);
        return cached.getDisabledChatModes().contains(chatMode);
    }
    
    /**
     * Gets the list of disabled chat modes for a player.
     * This will load the data from disk if not in cache, making it safe for admin operations on offline players.
     * 
     * @param playerUuid The player to get the disabled chat modes for
     * @return An unmodifiable set of disabled ChatModes, or empty set if none
     */
    public Set<ChatMode> getDisabledChatModes(UUID playerUuid)
    {
        CachedDisabledChatData cached = getOrLoadDisabledChatData(playerUuid);
        return Collections.unmodifiableSet(cached.getDisabledChatModes());
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
     * Saves all current disabled chat data to file.
     * Used for plugin shutdown or manual saves.
     */
    public void saveAllDisabledChatData()
    {
        for (Map.Entry<UUID, CachedDisabledChatData> entry : disabledChatCache.entrySet())
        {
            UUID playerUuid = entry.getKey();
            CachedDisabledChatData cached = entry.getValue();
            
            // Save all cached data, marking it as clean
            saveDisabledChatDataToDisk(playerUuid, cached.disabledChatModes);
            cached.markClean();
        }
    }
}