package com.failprooftech.factionschat;

import com.earth2me.essentials.Essentials;
import github.scarsz.discordsrv.DiscordSRV;

import com.massivecraft.factions.Factions;
import com.failprooftech.factionschat.commands.registrar.FactionsCommandRegistrar;
import com.failprooftech.factionschat.commands.registrar.GenericFactionsCommandRegistrar;
import com.failprooftech.factionschat.commands.registrar.MassiveFactionsCommandRegistrar;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.factions.FactionsBridge;
import com.failprooftech.factionschat.factions.MassiveFactionsBridge;
import com.failprooftech.factionschat.integrations.placeholderapi.GenericPlaceholderBridge;
import com.failprooftech.factionschat.integrations.placeholderapi.MassivePlaceholderBridge;
import com.failprooftech.factionschat.integrations.placeholderapi.PlaceholderBridge;
import com.failprooftech.factionschat.listeners.ConnectionListener;
import com.failprooftech.factionschat.listeners.DiscordSRVPaperListener;
import com.failprooftech.factionschat.listeners.DiscordSRVSpigotListener;
import com.failprooftech.factionschat.listeners.PaperFactionChatListener;
import com.failprooftech.factionschat.listeners.SpigotFactionChatListener;
import com.failprooftech.factionschat.util.DisabledChatManager;
import com.failprooftech.factionschat.util.IgnoreManager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * The FactionsChat plugin.
 */
public class FactionsChat extends JavaPlugin 
{
    /**
     * Singleton instance of the plugin.
     */
    public static FactionsChat instance;

    /**
     * While handling a single outgoing chat message, placeholders resolve against this channel when set
     * (e.g. quick-chat {@code :faction hello} with default prefix should show faction prefix for that send only).
     */
    private static final ThreadLocal<ChatMode> CHAT_MODE_PLACEHOLDER_OVERRIDE = new ThreadLocal<>();

    /**
     * Whether the chat reporting notice for Spigot servers has been logged yet.
     */
    private static final AtomicBoolean spigotChatReportingNoticeLogged = new AtomicBoolean(false);
    
    /**
     * A map of player chat modes (what mode they are currently using).
     * The key is the player's UUID, and the value is the ChatMode they are currently using.
     */
    private final Map<UUID, ChatMode> chatModes = new HashMap<>();

    
    // Active Factions bridge and command registrar (swappable per Factions implementation)
    private FactionsBridge factionsBridge;
    private FactionsCommandRegistrar commandRegistrar;

    // Plugin instances for optional integrations
    private DiscordSRV discordSrvPlugin;
    private Factions factionsPlugin;
    private Essentials essentialsPlugin;
    
    private PlaceholderBridge placeholderBridge = null;
    
    // Ignore system
    private IgnoreManager ignoreManager;
    
    // Disabled chat system
    private DisabledChatManager disabledChatManager;

    @Override
    public void onEnable() 
    {
        // Check for required dependency plugins and optional integrations
        PluginManager pm = getServer().getPluginManager();
        checkPlugins(pm);
        checkConflictingChatPlugins(pm);

        // Initialize ignore manager
        ignoreManager = new IgnoreManager(this);
        
        // Initialize disabled chat manager
        disabledChatManager = new DisabledChatManager(this);
        
        // Load any current chat modes from the chatmodes.yml file
        loadChatModesFromFile();
        
        // Save and update the main config file
        saveDefaultConfig();
        
        // Check config version and update if necessary
        updateConfig();

        // Register commands via the appropriate registrar
        if (commandRegistrar != null)
        {
            commandRegistrar.register(this);
        }

        // Initialise config
        Settings.load(getConfig(), factionsBridge);

        if (!isPaper() && !Settings.disableChatReporting && spigotChatReportingNoticeLogged.compareAndSet(false, true))
        {
            getLogger().warning("Chat reporting is only supported on Paper. No messages sent in chat will be able to be " +
                    "reported to Mojang. Set ChatSettings.DisableChatReporting to true to silence this warning.");
        }

        // Register event listener based on the server type
        if (isPaper()) 
        {
            pm.registerEvents(new PaperFactionChatListener(), this);
        } 
        else
        {
            pm.registerEvents(new SpigotFactionChatListener(), this);
        }
        
        // Register player connection listener for ignore data management
        pm.registerEvents(new ConnectionListener(), this);

        // updateManager = new UpdateManager();
        // getServer().getPluginManager().registerEvents(updateManager, this);
        // updateManager.run();
    }
    
    @Override
    public void onLoad() 
    {
        instance = this;
    }

    @Override
    public void onDisable()
    {
        // Restore any command hooks installed by the registrar
        if (commandRegistrar != null)
        {
            commandRegistrar.unregister(this);
        }

        // Unregister our PlaceholderAPI bridge
        if (placeholderBridge != null)
        {
            placeholderBridge.deactivate();
        }
        
        // Save chat modes to file on disable
        saveChatModesFile();
        
        // Save all ignore data and shutdown ignore manager
        if (ignoreManager != null)
        {
            ignoreManager.saveAllIgnoreData();
            ignoreManager.shutdown();
        }
        
        // Save all disabled chat data and shutdown disabled chat manager
        if (disabledChatManager != null)
        {
            disabledChatManager.saveAllDisabledChatData();
            disabledChatManager.shutdown();
        }
    }

    @Override
    public void reloadConfig()
    {
        saveDefaultConfig();
        super.reloadConfig();
        
        // Reinitialise chat config
        Settings.load(getConfig(), factionsBridge);
        saveChatModesFile(); // Save chat modes after reloading config
    }
    
    // - - - - - GETTERS - - - - -
    /**
     * Retrieves the current map of player chat modes, where the key is the
     * player's UUID and the value is the {@link ChatMode} they are using.
     *
     * @return The current map of player chat modes.
     */
    public Map<UUID, ChatMode> getPlayerChatModes()
    {
        return this.chatModes;
    }

    /**
     * Retrieves the DiscordSRV plugin instance if it is available.
     *
     * @return The DiscordSRV plugin instance, or null if not found.
     */
    public DiscordSRV getDiscordSrvPlugin()
    {
        return this.discordSrvPlugin;
    }

    /**
     * Retrieves the Factions plugin instance.
     *
     * @return The Factions plugin instance.
     */
    public Factions getFactionsPlugin() 
    {
        return this.factionsPlugin;
    }
    
    /**
     * Retrieves the Essentials plugin instance if it is available.
     *
     * @return The Essentials plugin instance, or null if not found.
     */
    public Essentials getEssentialsPlugin()
    {
        return this.essentialsPlugin;
    }

    /**
     * Retrieves whether PlaceholderAPI is enabled and available for use.
     * 
     * @return True if PlaceholderAPI is enabled, false otherwise.
     */
    public boolean isPapiEnabled()
    {
        return this.placeholderBridge != null;
    }
    
    /**
     * Retrieves the ignore manager instance.
     * 
     * @return The IgnoreManager instance.
     */
    public IgnoreManager getIgnoreManager()
    {
        return this.ignoreManager;
    }
    
    /**
     * Retrieves the disabled chat manager instance.
     * 
     * @return The DisabledChatManager instance.
     */
    public DisabledChatManager getDisabledChatManager()
    {
        return this.disabledChatManager;
    }

    /**
     * Sets the chat mode placeholder override for the current thread.
     * 
     * Used while building one chat send so {@link ChatMode#getChatModeForPlayer} matches a quick channel (e.g. {@code :f}).
     * @param mode The chat mode to override the placeholder for.
     */
    public static void setChatModePlaceholderOverride(ChatMode mode)
    {
        if (mode == null)
        {
            CHAT_MODE_PLACEHOLDER_OVERRIDE.remove();
        }
        else
        {
            CHAT_MODE_PLACEHOLDER_OVERRIDE.set(mode);
        }
    }

    /**
     * Clears the chat mode placeholder override for the current thread.
     */
    public static void clearChatModePlaceholderOverride()
    {
        CHAT_MODE_PLACEHOLDER_OVERRIDE.remove();
    }

    /**
     * Retrieves the chat mode placeholder override for the current thread.
     * 
     * @return The chat mode placeholder override for the current thread, or null if no override is set.
     */
    public static ChatMode getChatModePlaceholderOverride()
    {
        return CHAT_MODE_PLACEHOLDER_OVERRIDE.get();
    }

    /**
     * Retrieves the active {@link FactionsBridge} used to access Factions data.
     *
     * @return the current bridge, or {@code null} if no MassiveCraft bridge is available
     */
    public FactionsBridge getFactionsBridge()
    {
        return this.factionsBridge;
    }

    /**
     * Replaces the active {@link FactionsBridge}.
     *
     * @param bridge the new bridge (must not be {@code null})
     */
    public void setFactionsBridge(FactionsBridge bridge)
    {
        if (bridge == null) throw new NullPointerException("bridge");
        this.factionsBridge = bridge;
    }

    /**
     * Returns {@code true} when the server is running Paper (or a Paper fork).
     * Detected via the presence of {@code io.papermc.paper.event.player.AsyncChatEvent}.
     */
    private static boolean isPaper()
    {
        try
        {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }
    
    // - - - - - PUBLIC METHODS - - - - -
    
    // TODO: this needs to be reworked - we should only be keeping chat modes in memory when players are online
    //       so we need to load/save on player join/quit instead of loading all at once - only save the single
    //       player's chat mode on quit instead of the whole file. Save the whole file on disable?
    /**
     * Loads the <code>chatmodes.yml</code> file, which contains a list of players and what
     * chat mode they're currently using. This stores the chat modes in the
     * {@link #chatModes} map, where the key is the player's UUID and the value is the 
     * {@link ChatMode} they are using.
     */
    public void loadChatModesFromFile()
    {
        // Ensure the plugin data folder exists
        if (!getDataFolder().exists()) 
        {
            getDataFolder().mkdirs();
        }
        
        File chatmodesFile = new File(getDataFolder(), "chatmodes.yml");
        if (!chatmodesFile.exists()) 
        {
            // If the file doesn't exist, create it with an empty configuration
            try 
            {
                chatmodesFile.createNewFile();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        }

        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(chatmodesFile);
        for (String key : yamlConfig.getKeys(false)) 
        {
            UUID playerUUID = UUID.fromString(key);
            String chatModeName = yamlConfig.getString(key);
            ChatMode chatMode = ChatMode.getChatModeByName(chatModeName);
            if (chatMode != null) 
            {
                chatModes.put(playerUUID, chatMode);
            }
        }
    }

    /**
     * Saves the <code>chatmodes.yml</code> file, which contains a list of players and what
     * chat mode they're currently using.
     */
    public void saveChatModesFile()
    {
        // Ensure the plugin data folder exists
        if (!getDataFolder().exists()) 
        {
            getDataFolder().mkdirs();
        }
        
        File chatmodesFile = new File(getDataFolder(), "chatmodes.yml");
        if (!chatmodesFile.exists()) 
        {
            // If the file doesn't exist, create it with an empty configuration
            try 
            {
                chatmodesFile.createNewFile();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        }
        
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(chatmodesFile);
        for (Map.Entry<UUID, ChatMode> entry : chatModes.entrySet())
        {
            yamlConfig.set(entry.getKey().toString(), entry.getValue().name());
        }

        try 
        {
            yamlConfig.save(chatmodesFile);
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    
    // - - - - - PRIVATE METHODS - - - - -
    /**
     * Updates the main <code>config.yml</code> file for FactionsChat. 
     */
    private void updateConfig()
    {
        try
        {
            File configFile = new File(getDataFolder(), Settings.CONFIG_FILE_NAME);
            if (!configFile.exists())
            {
                saveDefaultConfig();
                getLogger().info("Generated " + Settings.CONFIG_FILE_NAME + " with default settings.");
                return;
            }

            // TODO: This works fine, but could be improved to better handle any structural migrations
            // Compare the current config with the default config and update if necessary
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource(Settings.CONFIG_FILE_NAME)));
            YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);

            // Get versions to check if an update is needed
            int defaultVersion = defaultConfig.getInt("version");
            // If no version, we have an invalid config, set to 1 to force update
            int currentVersion = currentConfig.getInt("version", 1); 

            // If default version could not be determined, log error and exit
            if (defaultVersion <= 0)
            {
                getLogger().severe("Default config could not be loaded during config update. Please restart your server. "
                    + "If the issue persists, please log an issue on Github.");
                return;
            }

            // If versions differ, update config
            if (defaultVersion != currentVersion)
            {
                // If a backup file already exists, delete it
                File backupFile = new File(getDataFolder(), Settings.BACKUP_CONFIG_FILE_NAME);
                if (backupFile.exists())
                {
                    boolean deleted = backupFile.delete();
                    if (!deleted)
                    {
                        getLogger().severe("Could not delete old " + Settings.BACKUP_CONFIG_FILE_NAME + " during config update. "
                            + "Update aborted to prevent data loss. Please check file permissions and restart your server. If the issue persists, "
                            + "please log an issue on Github.");
                        return;
                    }
                }

                // Create new backup of current config
                try 
                {
                    currentConfig.save(backupFile);
                } 
                catch (IllegalArgumentException | IOException e) 
                {
                    getLogger().severe("Could not create backup of " + Settings.BACKUP_CONFIG_FILE_NAME + " during config update. "
                        + "Update aborted to prevent data loss. Please check file permissions and restart your server. If the issue persists, "
                        + "please log an issue on Github with the following error:");
                    e.printStackTrace();
                    return;
                }

                // Now perform update - merge user values into default config
                mergeUserValuesIntoDefault(defaultConfig, currentConfig, "");
                defaultConfig.set("version", defaultVersion); // Always update version as last step
                
                // Save the merged config (updated config with previous values where possible)
                defaultConfig.save(configFile);

                getLogger().info("Upgraded " + Settings.CONFIG_FILE_NAME + " to version " + defaultVersion + ". The config has been regenerated with " +
                    "new comments and structure while preserving your original settings. Please review your config to ensure everything still looks correct. " +
                    "For safety, a backup named " + Settings.BACKUP_CONFIG_FILE_NAME + " has been created which you can reference if needed.");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Recursively merges user values from userConfig into defaultConfig, preserving comments and structure.
     * This approach uses the default config as the base (preserving comments) and overwrites values
     * with user customizations where they exist.
     * 
     * @param defaultConfig The default configuration (target - will be modified).
     * @param userConfig The user's current configuration (source of custom values).
     * @param path The current path being processed.
     */
    private void mergeUserValuesIntoDefault(YamlConfiguration defaultConfig, YamlConfiguration userConfig, String path)
    {
        for (String key : defaultConfig.getConfigurationSection(path.isEmpty() ? "" : path).getKeys(false))
        {
            String fullKey = path.isEmpty() ? key : path + "." + key;

            // Recursively process subsections
            if (defaultConfig.isConfigurationSection(fullKey))
            {
                mergeUserValuesIntoDefault(defaultConfig, userConfig, fullKey);
            }
            else
            {
                // If user config has this key, use the user's value
                // Otherwise, keep the default value (already in defaultConfig)
                if (userConfig.contains(fullKey))
                {
                    defaultConfig.set(fullKey, userConfig.get(fullKey));
                }
            }
        }
    }
    
    /**
     * Checks for required or integrated plugins.
     * 
     * @param pm The plugin manager to check for plugins
     */
    private void checkPlugins(PluginManager pm) 
    {
        Logger logger = getLogger();

        // - - - - - - - - - REQUIRED PLUGINS - - - - - - - - -
        Plugin factions = pm.getPlugin("Factions");
        if (factions == null || !factions.isEnabled()) 
        {
            logger.severe("A Factions plugin is required, but was not found or is disabled.");
            pm.disablePlugin(this);
            return;
        }

        // Detect which Factions implementation is installed and wire the appropriate
        // bridge + command registrar. Class loading is lazy in HotSpot, so
        // MassiveFactionsCommandRegistrar (which references FactionsCommand) is only
        // loaded when we actually instantiate it inside the try block.
        try
        {
            Class<?> massiveClass = Class.forName("com.massivecraft.factions.Factions");
            if (massiveClass.isInstance(factions))
            {
                // MassiveCraft Factions - also needs MassiveCore
                Plugin massiveCorePlugin = pm.getPlugin("MassiveCore");
                if (massiveCorePlugin == null || !massiveCorePlugin.isEnabled())
                {
                    logger.severe("MassiveCore is required when using MassiveCraft Factions, but was not found or is disabled.");
                    pm.disablePlugin(this);
                    return;
                }
                this.factionsPlugin   = (Factions) factions;
                this.factionsBridge   = MassiveFactionsBridge.get();
                this.commandRegistrar = new MassiveFactionsCommandRegistrar();
                logger.info("MassiveCraft Factions + MassiveCore detected.");
            }
            else
            {
                // Different Factions fork installed
                this.factionsBridge   = null; // No bridge for non-MassiveCraft Factions yet
                this.commandRegistrar = new GenericFactionsCommandRegistrar();
                logger.info("Non-MassiveCraft Factions plugin detected. Using generic command integration.");
            }
        }
        catch (ClassNotFoundException e)
        {
            // MassiveCraft Factions classes not on classpath
            this.factionsBridge   = null;
            this.commandRegistrar = new GenericFactionsCommandRegistrar();
            logger.info("Non-MassiveCraft Factions plugin detected. Using generic command integration.");
        }

        // - - - - - - - - - OPTIONAL PLUGINS - - - - - - - - -
        Plugin discordSrv = pm.getPlugin("DiscordSRV");
        if (discordSrv != null && discordSrv.isEnabled()) 
        {
            this.discordSrvPlugin = (DiscordSRV) discordSrv;
            DiscordSRV.api.subscribe(isPaper() ? new DiscordSRVPaperListener() : new DiscordSRVSpigotListener());
            logger.info("DiscordSRV detected - integration enabled.");
        }

        Plugin essentials = pm.getPlugin("Essentials");
        if (essentials != null && essentials.isEnabled()) 
        {
            this.essentialsPlugin = (Essentials) essentials;
            logger.info("Essentials detected.");
        }

        // PlaceholderAPI integration
        Plugin papi = pm.getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled())
        {
            // Use the MassiveCraft expander hook when MassiveFactions is active so chat
            // placeholders share the existing %factions_* namespace; otherwise register
            // our own expansion under the %factionschat_* identifier.
            if (this.factionsBridge instanceof MassiveFactionsBridge)
            {
                this.placeholderBridge = MassivePlaceholderBridge.get();
                logger.info("PlaceholderAPI detected. Injecting chat placeholders into the Factions expansion (%factions_chat_*).");
            }
            else
            {
                this.placeholderBridge = new GenericPlaceholderBridge();
                logger.info("PlaceholderAPI detected. Registering standalone expansion (%factionschat_*).");
            }
            this.placeholderBridge.activate();
        }
        else
        {
            logger.info("PlaceholderAPI not found. Using internal tag parser for chat formatting.");
        }
    }

    /**
     * Checks for conflicting chat plugins that may interfere with FactionsChat.
     * If any unsupported chat plugins are found, display a warning message. We
     * will still attempt to run alongside them, but users may experience issues 
     * and are advised to remove the conflicting plugin for best results.
     */
    private void checkConflictingChatPlugins(PluginManager pm) 
    {
        // Even if a plugin is not in this list, we do not support any chat plugins 
        // that modify the chat format or handle chat events in a way that conflicts 
        // with FactionsChat. Only one chat plugin should be active at a time.
        // TODO: Make this dynamic? Can we see if other plugins are hooking into chat events?
        List<String> unsupportedChatPlugins = new ArrayList<>();
        unsupportedChatPlugins.add("AdvancedChat");
        unsupportedChatPlugins.add("ChatChat");
        unsupportedChatPlugins.add("ChatControl");
        unsupportedChatPlugins.add("ChatControlRed");
        unsupportedChatPlugins.add("ChatEx");
        unsupportedChatPlugins.add("ChatManager");
        unsupportedChatPlugins.add("ChatSentry");
        unsupportedChatPlugins.add("EssentialsChat");
        unsupportedChatPlugins.add("FairyChat");
        unsupportedChatPlugins.add("HeroChat");
        unsupportedChatPlugins.add("LokiChat");
        unsupportedChatPlugins.add("LuckPermsChat");
        unsupportedChatPlugins.add("LPC");
        unsupportedChatPlugins.add("PartyChat");
        unsupportedChatPlugins.add("VentureChat");
        
        // Check for unsupported/conflicting chat plugins
        for (String pluginName : unsupportedChatPlugins) 
        {
            if (pm.getPlugin(pluginName) != null) 
            {
                getLogger().warning(pluginName + " detected, which is not compatible with FactionsChat. You may experience issues. Please "
                    + "remove " + pluginName + " to ensure proper functionality of FactionsChat. Issues reported with both FactionsChat and " 
                    + pluginName + " installed will not be able to be investigated.");
                return;
            }
        }
    }
}