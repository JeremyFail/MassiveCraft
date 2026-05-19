package com.failprooftech.factionschat;


import com.failprooftech.factionschat.commands.registrar.FactionsCommandRegistrar;
import com.failprooftech.factionschat.commands.registrar.GenericFactionsCommandRegistrar;
import com.failprooftech.factionschat.commands.registrar.MassiveFactionsCommandRegistrar;
import com.failprooftech.factionschat.commands.registrar.PvPIndexFactionsCommandRegistrar;
import com.failprooftech.factionschat.commands.registrar.PvPIndexTeamsSubcommandSupport;
import com.failprooftech.factionschat.commands.registrar.StandaloneChatCommandRegistrar;
import com.failprooftech.factionschat.commands.registrar.TeamsApiChatCommandRegistrar;
import com.failprooftech.factionschat.config.Settings;
import com.failprooftech.factionschat.factions.FactionsBridge;
import com.failprooftech.factionschat.factions.MassiveFactionsBridge;
import com.failprooftech.factionschat.factions.PvPIndexFactionsBridge;
import com.failprooftech.factionschat.integrations.discordsrv.DiscordSRVIntegration;
import com.failprooftech.factionschat.integrations.discordsrv.DiscordSRVIntegrationNoop;
import com.failprooftech.factionschat.integrations.discordsrv.DiscordSRVIntegrations;
import com.failprooftech.factionschat.integrations.essentials.EssentialsIntegration;
import com.failprooftech.factionschat.integrations.essentials.EssentialsIntegrationNoop;
import com.failprooftech.factionschat.integrations.essentials.EssentialsIntegrations;
import com.failprooftech.factionschat.integrations.placeholderapi.GenericPlaceholderBridge;
import com.failprooftech.factionschat.integrations.placeholderapi.MassivePlaceholderBridge;
import com.failprooftech.factionschat.integrations.placeholderapi.PlaceholderBridge;
import com.failprooftech.factionschat.integrations.teamsapi.TeamsIntegrationLive;
import com.failprooftech.factionschat.integrations.teamsapi.TeamsIntegrationNoop;
import com.failprooftech.factionschat.integrations.teamsapi.TeamsIntegrationRegistry;
import com.failprooftech.factionschat.listeners.ConnectionListener;
import com.failprooftech.factionschat.listeners.PaperFactionChatListener;
import com.failprooftech.factionschat.listeners.SpigotFactionChatListener;
import com.failprooftech.factionschat.metrics.FactionsChatBStats;
import com.failprooftech.factionschat.update.FactionsChatUpdate;
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
import java.util.Optional;
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

    /**
     * Help text / pager base (e.g. {@code /f c} when hooked into Factions, {@code /chat} in standalone mode).
     */
    private String chatCommandPrefix = "/f c";

    // Plugin instances for optional integrations
    /**
     * {@code true} when MassiveCraft Factions + MassiveCore are detected at startup.
     * <p>
     * Drives PlaceholderAPI ({@link MassivePlaceholderBridge}, {@code %factions_*}) and related config migration only.
     * This is intentionally <em>not</em> the same as {@code factionsBridge instanceof MassiveFactionsBridge}: chat may use
     * Teams API ({@link #factionsBridge}) while Massive Factions is still installed and owns the shared PAPI expansion.</p>
     */
    private boolean massiveCraftFactionsEnvironment;
    /** Typed Essentials wiring ({@link EssentialsIntegrationNoop} when disabled in config, Essentials is absent, or cast fails). */
    private EssentialsIntegration essentialsIntegration = EssentialsIntegrationNoop.INSTANCE;
    /** DiscordSRV wiring ({@link DiscordSRVIntegrationNoop} when disabled in config, DiscordSRV is absent, or bootstrap fails). */
    private DiscordSRVIntegration discordSRVIntegration = DiscordSRVIntegrationNoop.INSTANCE;

    private PlaceholderBridge placeholderBridge = null;
    
    // Ignore system
    private IgnoreManager ignoreManager;
    
    // Disabled chat system
    private DisabledChatManager disabledChatManager;

    @Override
    public void onLoad()
    {
        instance = this;
        //Check for Teams API and register if present
        if (teamsApiPresentOnClasspath())
        {
            TeamsIntegrationRegistry.register(new TeamsIntegrationLive());
        }
        else
        {
            TeamsIntegrationRegistry.register(TeamsIntegrationNoop.INSTANCE);
        }
    }

    @Override
    public void onEnable() 
    {
        // Check for required dependency plugins and optional integrations
        PluginManager pm = getServer().getPluginManager();
        if (!checkPlugins(pm) || !isEnabled())
        {
            return;
        }
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

        // Schedule standalone update check only when not running MassiveCraft Factions + MassiveCore
        // (in that case MassiveCore's suite checker already covers FactionsChat).
        if (!this.massiveCraftFactionsEnvironment)
        {
            FactionsChatUpdate.scheduleAfterPluginsEnabled(this);
        }

        new FactionsChatBStats(this).enable();
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

        this.discordSRVIntegration = DiscordSRVIntegrationNoop.INSTANCE;

        // Stop standalone update check tasks.
        FactionsChatUpdate.shutdown();
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
     * DiscordSRV integration for chat relay ({@link DiscordSRVIntegrationNoop} when disabled in config or DiscordSRV is absent).
     */
    public DiscordSRVIntegration getDiscordSRVIntegration()
    {
        return this.discordSRVIntegration;
    }

    /**
     * Whether MassiveCraft Factions + MassiveCore were detected (PlaceholderAPI namespace / Massive expansion).
     * Does not imply {@link #factionsBridge} is {@link MassiveFactionsBridge}.
     */
    public boolean isMassiveCraftFactionsEnvironment()
    {
        return this.massiveCraftFactionsEnvironment;
    }
    
    /**
     * Essentials integration for SocialSpy and related behaviour ({@link EssentialsIntegrationNoop} when disabled in config or Essentials is absent).
     */
    public EssentialsIntegration getEssentialsIntegration()
    {
        return this.essentialsIntegration;
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
     * If the player is still set to a faction-scoped mode but no {@link FactionsBridge} is wired (e.g. stale {@code chatmodes.yml}),
     * deliver chat as {@link ChatMode#GLOBAL} instead.
     */
    public static ChatMode resolveEffectiveChatMode(ChatMode mode)
    {
        if (mode == null)
        {
            return ChatMode.GLOBAL;
        }
        if (mode.requiresFactionData() && instance != null && instance.getFactionsBridge() == null)
        {
            return ChatMode.GLOBAL;
        }
        return mode;
    }

    /**
     * Base chat command string for help text (not necessarily a registered root command when using {@code /f c}).
     */
    public String getChatCommandPrefix()
    {
        return chatCommandPrefix;
    }

    /**
     * Retrieves the active {@link FactionsBridge} used to access Factions data.
     *
     * @return the current bridge, or {@code null} when no teams/direct bridge could be wired (for example generic factions with no Teams API)
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
     * Classpath probe for the Teams API (same approach as MassiveCraft Factions uses via {@code Class.forName} on faction entities).
     *
     * @return {@code true} when {@code com.skyblockexp.teamsapi.api.TeamsAPI} can be resolved by this plugin's class loader
     */
    private static boolean teamsApiPresentOnClasspath()
    {
        try
        {
            Class.forName("com.skyblockexp.teamsapi.api.TeamsAPI");
            return true;
        }
        catch (final ClassNotFoundException ignored)
        {
            return false;
        }
    }

    /**
     * Checks for integrated plugins and selects command registration / faction bridges.
     *
     * @param pm The plugin manager to check for plugins
     * @return {@code false} if this plugin was disabled and {@link #onEnable()} must stop immediately
     *         (do not touch the class loader after {@link PluginManager#disablePlugin(Plugin)}).
     */
    private boolean checkPlugins(PluginManager pm)
    {
        Logger logger = getLogger();

        // Factions / PvPIndex are optional: chat still runs with global/local/world/staff when no faction bridge exists.
        Plugin factions           = pm.getPlugin("Factions");
        Plugin pvpIndexFactions   = pm.getPlugin("PvPIndexFactions");

        this.massiveCraftFactionsEnvironment = false;
        this.chatCommandPrefix               = "/f c";

        final boolean factionsEnabled = factions != null && factions.isEnabled();
        final boolean pvpIndexReady   = pvpIndexFactions != null && pvpIndexFactions.isEnabled();

        boolean massiveCraft = false;
        if (factionsEnabled)
        {
            try
            {
                Class.forName("com.massivecraft.factions.entity.Faction");

                Plugin massiveCorePlugin = pm.getPlugin("MassiveCore");
                if (massiveCorePlugin == null || !massiveCorePlugin.isEnabled())
                {
                    logger.severe("MassiveCore is required when using MassiveCraft Factions, but was not found or is disabled.");
                    pm.disablePlugin(this);
                    return false;
                }
                massiveCraft = true;
                this.massiveCraftFactionsEnvironment = true;
                logger.info("MassiveCraft Factions + MassiveCore detected.");
            }
            catch (ClassNotFoundException ignored)
            {
                massiveCraft = false;
            }
        }

        // Command routing: Massive / PvPIndex / Teams API / generic command hook / standalone
        if (massiveCraft)
        {
            this.commandRegistrar = new MassiveFactionsCommandRegistrar();
            logger.info("Command integration: MassiveCraft Factions (/f chat, /f c).");
        }
        else if (pvpIndexReady)
        {
            if (PvPIndexTeamsSubcommandSupport.isTeamsSubcommandDispatchAvailable(logger))
            {
                this.commandRegistrar = new TeamsApiChatCommandRegistrar();
                this.chatCommandPrefix = "/f chat";
                logger.info("Command integration: PvPIndex Factions via TeamsAPI subcommands (/f chat, /f c).");
            }
            else
            {
                logger.info("PvPIndex TeamsAPI subcommand dispatch not active; using direct /f command hook for chat.");
                this.commandRegistrar = new PvPIndexFactionsCommandRegistrar();
                logger.info("Command integration: PvPIndex Factions (/f chat, /f c).");
            }
        }
        else if (factionsEnabled)
        {
            org.bukkit.command.PluginCommand fCmd = getServer().getPluginCommand("factions");
            if (fCmd == null)
            {
                fCmd = getServer().getPluginCommand("f");
            }
            if (fCmd != null)
            {
                this.commandRegistrar = new GenericFactionsCommandRegistrar();
                logger.warning("Generic /f command hook (non-MassiveCraft Factions). This is not fully supported; some command features may be limited. "
                        + "If you want full support, please use MassiveCraft Factions or submit an issue requesting support for your "
                        + "Factions fork.");
            }
            else
            {
                this.commandRegistrar = new StandaloneChatCommandRegistrar();
                this.chatCommandPrefix = "/chat";
                logger.info("Factions is enabled but no /f or /factions command was found; registering standalone /chat (alias /c).");
            }
        }
        else
        {
            this.commandRegistrar = new StandaloneChatCommandRegistrar();
            this.chatCommandPrefix = "/chat";
            logger.info("No Factions plugin enabled; registering standalone /chat (alias /c).");
        }

        // Chat membership / relations: prefer Teams API whenever any provider is registered.
        final Optional<FactionsBridge> teamsApiBridgeOpt = TeamsIntegrationRegistry.get().createBridge(logger);
        if (teamsApiBridgeOpt.isPresent())
        {
            this.factionsBridge = teamsApiBridgeOpt.get();
            logger.info("Faction chat data: Teams API integration.");
        }
        else if (massiveCraft)
        {
            this.factionsBridge = MassiveFactionsBridge.get();
            logger.info("Faction chat data: MassiveCraft direct integration.");
        }
        else if (pvpIndexReady)
        {
            final Optional<FactionsBridge> pvpBridgeOpt = PvPIndexFactionsBridge.tryCreateBridge(pvpIndexFactions);
            if (pvpBridgeOpt.isEmpty())
            {
                logger.severe("Failed to initialise PvPIndex Factions bridge: plugin enabled but FactionService could not be wired.");
                pm.disablePlugin(this);
                return false;
            }
            this.factionsBridge = pvpBridgeOpt.get();
            logger.info("Faction chat data: PvPIndex-Factions direct integration.");
        }
        else
        {
            this.factionsBridge = null;
            logger.warning("Factions relation channels (faction, ally, truce, neutral, enemy) are disabled - no faction or teams integration. "
                    + "Global, local, world, and staff chat remain available.");
        }

        if (getConfig().getBoolean("DiscordSRV.enabled", true))
        {
            this.discordSRVIntegration = DiscordSRVIntegrations.bootstrap(
                    pm.getPlugin("DiscordSRV"),
                    isPaper(),
                    getConfig().getString("DiscordSRV.StaffChannel", "000000000000000000"),
                    logger);
        }

        if (getConfig().getBoolean("Essentials.enabled", true))
        {
            this.essentialsIntegration = EssentialsIntegrations.bootstrap(pm.getPlugin("Essentials"));
            if (this.essentialsIntegration != EssentialsIntegrationNoop.INSTANCE)
            {
                logger.info("Essentials detected. Enabling SocialSpy support.");
            }
        }

        // PlaceholderAPI integration
        Plugin papi = pm.getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled())
        {
            // MassiveCraft Factions + MassiveCore: hook Massive placeholder expansion regardless of whether
            // chat uses Teams API or direct Massive {@link FactionsBridge}.
            if (this.massiveCraftFactionsEnvironment)
            {
                this.placeholderBridge = MassivePlaceholderBridge.get();
                logger.info("PlaceholderAPI detected. Registering MassiveCraft Factions expansion (%factions_chat_*).");
            }
            else
            {
                this.placeholderBridge = new GenericPlaceholderBridge();
                logger.info("PlaceholderAPI detected. Registering standalone expansion (%factionschat_*).");
            }
            this.placeholderBridge.activate();
            migrateChatFormatPlaceholderNamespace();
        }
        else
        {
            logger.info("PlaceholderAPI not found. Using internal tag parser for chat formatting.");
        }

        return true;
    }

    /**
     * After the PAPI bridge is chosen, checks whether the configured chat format uses
     * placeholders from the wrong namespace and migrates them automatically.
     *
     * <ul>
     *   <li>MassiveCraft Factions + MassiveCore present ({@link #isMassiveCraftFactionsEnvironment()} {@code true}), placeholders should use {@code %factions_*}.
     *       If {@code %factionschat_*} is found, replace and warn.</li>
     *   <li>Otherwise, placeholders should use {@code %factionschat_*}.
     *       If {@code %factions_*} is found, replace and warn.</li>
     * </ul>
     *
     * <p>When replacements are made the config value is updated on disk so the migration
     * is permanent and does not repeat on every restart.</p>
     */
    private void migrateChatFormatPlaceholderNamespace()
    {
        Logger logger = getLogger();
        String format = getConfig().getString("ChatSettings.ChatFormat", "");
        if (format == null || format.isEmpty()) return;

        String migrated;
        String from;
        String to;
        String fromRel;
        String toRel;

        if (this.massiveCraftFactionsEnvironment)
        {
            from = "%factionschat_";
            to   = "%factions_";
            fromRel = "%rel_factionschat_";
            toRel   = "%rel_factions_";
        }
        else
        {
            from = "%factions_";
            to   = "%factionschat_";
            fromRel = "%rel_factions_";
            toRel   = "%rel_factionschat_";
        }

        if (!format.contains(from) && !format.contains(fromRel)) return;

        migrated = format.replace(from, to).replace(fromRel, toRel);

        logger.warning("ChatFormat contains placeholders using the wrong namespace for the active Factions plugin.");
        logger.warning("Automatically migrating: \"" + from + "\" to \"" + to + "\" and \"" + fromRel + "\" to \"" + toRel + 
                "\" in ChatSettings.ChatFormat.");

        getConfig().set("ChatSettings.ChatFormat", migrated);
        saveConfig();

        // Reload Settings so in-memory value reflects the migrated format
        Settings.load(getConfig(), this.factionsBridge);
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