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

/**
 * Manages player ignore lists for FactionsChat.
 * Handles loading/saving ignore data and provides methods for managing ignore relationships.
 */
public class IgnoreManager {
    
    private static final String IGNORES_FILE_NAME = "ignores.yml";
    
    // In-memory storage: Key = receiving player UUID, Value = Set of ignored player UUIDs
    private final Map<UUID, Set<UUID>> ignoreMap = new ConcurrentHashMap<>();
    
    private final FactionsChat plugin;
    
    public IgnoreManager(FactionsChat plugin)
    {
        this.plugin = plugin;
    }
    
    /**
     * Loads ignore data for a player when they join the server.
     * 
     * @param playerUuid The UUID of the player joining
     */
    public void loadPlayerIgnores(UUID playerUuid)
    {
        File ignoresFile = new File(plugin.getDataFolder(), IGNORES_FILE_NAME);
        
        // No ignore data exists yet
        if (!ignoresFile.exists())
        {
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(ignoresFile);
        List<String> ignoredUuids = config.getStringList(playerUuid.toString());
        
        if (!ignoredUuids.isEmpty())
        {
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
            ignoreMap.put(playerUuid, ignoredSet);
        }
    }
    
    /**
     * Saves ignore data for a player when they leave the server and removes them from memory.
     * 
     * @param playerUuid The UUID of the player leaving
     */
    public void saveAndUnloadPlayerIgnores(UUID playerUuid)
    {
        Set<UUID> ignoredPlayers = ignoreMap.remove(playerUuid);
        if (ignoredPlayers == null || ignoredPlayers.isEmpty())
        {
            // Remove player from file if they're not ignoring anyone
            removePlayerFromFile(playerUuid);
            return;
        }
        
        // Save to file
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
        
        // Convert Set<UUID> to List<String>
        List<String> ignoredUuidStrings = ignoredPlayers.stream()
            .map(UUID::toString)
            .toList();
        
        config.set(playerUuid.toString(), ignoredUuidStrings);
        
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
     * Removes a player's entry from the ignores file.
     */
    private void removePlayerFromFile(UUID playerUuid)
    {
        File ignoresFile = new File(plugin.getDataFolder(), IGNORES_FILE_NAME);
        if (!ignoresFile.exists())
        {
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(ignoresFile);
        if (config.contains(playerUuid.toString()))
        {
            config.set(playerUuid.toString(), null);
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
    }
    
    /**
     * Adds a player to another player's ignore list.
     * 
     * @param ignoringPlayer The player who is ignoring
     * @param ignoredPlayer The player being ignored
     */
    public void addIgnore(UUID ignoringPlayer, UUID ignoredPlayer)
    {
        ignoreMap.computeIfAbsent(ignoringPlayer, k -> new HashSet<>()).add(ignoredPlayer);
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
        Set<UUID> ignoredPlayers = ignoreMap.get(ignoringPlayer);
        if (ignoredPlayers != null)
        {
            boolean removed = ignoredPlayers.remove(ignoredPlayer);
            if (ignoredPlayers.isEmpty())
            {
                ignoreMap.remove(ignoringPlayer);
            }
            return removed;
        }
        return false;
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
        Set<UUID> ignoredPlayers = ignoreMap.get(receivingPlayer);
        return ignoredPlayers != null && ignoredPlayers.contains(sendingPlayer);
    }
    
    /**
     * Gets the list of players that a player is ignoring.
     * 
     * @param player The player to get the ignore list for
     * @return An unmodifiable set of ignored player UUIDs, or empty set if none
     */
    public Set<UUID> getIgnoredPlayers(UUID player)
    {
        Set<UUID> ignoredPlayers = ignoreMap.get(player);
        return ignoredPlayers != null ? Collections.unmodifiableSet(ignoredPlayers) : Collections.emptySet();
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
        if (ignoreMap.isEmpty())
        {
            return;
        }
        
        File ignoresFile = new File(plugin.getDataFolder(), IGNORES_FILE_NAME);
        if (!plugin.getDataFolder().exists())
        {
            plugin.getDataFolder().mkdirs();
        }
        
        YamlConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, Set<UUID>> entry : ignoreMap.entrySet())
        {
            List<String> ignoredUuidStrings = entry.getValue().stream()
                .map(UUID::toString)
                .toList();
            config.set(entry.getKey().toString(), ignoredUuidStrings);
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
}