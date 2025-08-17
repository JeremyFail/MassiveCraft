package com.massivecraft.factionschat;

import com.earth2me.essentials.Essentials;
import github.scarsz.discordsrv.DiscordSRV;

import com.massivecraft.factions.Factions;
import com.massivecraft.factionschat.commands.CmdFactionsChat;
import com.massivecraft.factionschat.integrations.PlaceholderFactionsChat;
import com.massivecraft.factionschat.listeners.DiscordSRVListener;
import com.massivecraft.factionschat.listeners.PaperFactionChatListener;
import com.massivecraft.factionschat.listeners.SpigotFactionChatListener;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.factions.cmd.CmdFactions;

import org.bukkit.configuration.file.FileConfiguration;
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
     * A map of players in quick message mode.
     * The key is the player's UUID, and the value is the ChatMode they are sending a quick message to.
     */
    public static final Map<UUID, ChatMode> qmPlayers = new HashMap<>();
    
    /**
     * A map of player chat modes (what mode they are currently using).
     * The key is the player's UUID, and the value is the ChatMode they are currently using.
     */
    private final Map<UUID, ChatMode> chatModes = new HashMap<>();

    /**
     * The default chat format used by FactionsChat.
     */
    private static final String DEFAULT_CHAT_FORMAT = "%factions_chat_prefix|rp%&r<%rel_factions_relation_color%%factions_player_rankprefix%%factions_faction_name|rp%&r%DISPLAYNAME%> %factions_chat_color%%MESSAGE%";
    
    // Chat config settings
    // TODO: should these be stored in a static config class?
    private String chatFormat = DEFAULT_CHAT_FORMAT;
    private boolean allowColorCodes = true;
    private boolean allowUrl = true;
    private boolean allowUrlUnderline = true;
    private int localChatRange = 1000;
    
    // Plugin instances for optional integrations
    private DiscordSRV discordSrvPlugin;
    private Factions factionsPlugin;
    private Essentials essentialsPlugin;
    // TODO: Reimplement?
    // private UpdateManager updateManager;
    
    private boolean papiEnabled = false;

    @Override
    public void onEnable() 
    {
        // Load any current chat modes from the chatmodes.yml file
        loadChatModesFromFile();
        
        // Save and update the main config file
        saveDefaultConfig();
        // TODO: Check config version and update if necessary
        updateConfig();

        // Register commands
        CmdFactions.get().addChild(new CmdFactionsChat());

        // Initilize chat config
        initializeChat();

        // Check for required dependency plugins and optional integrations
        PluginManager pm = getServer().getPluginManager();
        checkPlugins(pm);
        checkConflictingChatPlugins(pm);

        // Register event listener based on the server type
        if (MUtil.isPaper()) 
        {
            pm.registerEvents(new PaperFactionChatListener(), this);
        } 
        else
        {
            pm.registerEvents(new SpigotFactionChatListener(), this);
        }

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
        // Save chat modes to file on disable
        saveChatModesFile();
    }

    @Override
    public void reloadConfig()
    {
        saveDefaultConfig();
        super.reloadConfig();
        
        // Reinitilize chat config
        initializeChat();
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
     * Retrieves the chat format string used for formatting chat messages.
     * 
     * @return The chat format string.
     */
    public String getChatFormat() 
    {
        return this.chatFormat;
    }

    /**
     * Retrieves the config setting for whether color/format codes are allowed in chat messages.
     * 
     * @return True if color/format codes are allowed, false otherwise.
     */
    public boolean getAllowColorCodes() 
    {
        return this.allowColorCodes;
    }

    /**
     * Retrieves the config setting for whether clickable URLs are allowed in chat messages.
     * 
     * @return True if clickable URLs are allowed, false otherwise.
     */
    public boolean getAllowUrl() 
    {
        return this.allowUrl;
    }

    /**
     * Retrieves the config setting for whether URLs in chat messages should be underlined.
     * 
     * @return True if URLs should be underlined, false otherwise.
     */
    public boolean getAllowUrlUnderline() 
    {
        return this.allowUrlUnderline;
    }

    /**
     * Retrieves the local chat range, which is the distance in blocks
     * within which players can hear each other in local chat.
     *
     * @return The local chat range in blocks.
     */
    public int getLocalChatRange() 
    {
        return this.localChatRange;
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
    public boolean isPapiEnabled() {
        return this.papiEnabled;
    }
    
    // - - - - - PUBLIC METHODS - - - - -
    
    /**
     * Loads the <code>chatmodes.yml</code> file, which contains a list of players and what
     * chat mode they're currently using. This stores the chat modes in the
     * {@link #chatModes} map, where the key is the player's UUID and the value is the 
     * {@link ChatMode} they are using.
     */
    public void loadChatModesFromFile()
    {
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
        File chatmodesFile = new File(getDataFolder(), "chatmodes.yml");
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
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists())
            {
                saveDefaultConfig();
                getLogger().info("Generated config.yml with default settings.");
                return;
            }

            // TODO: Implement better version handling
            // Compare the current config with the default config and update if necessary
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("config.yml")));
            YamlConfiguration serverConfig = YamlConfiguration.loadConfiguration(configFile);

            String defaultVersion = defaultConfig.getString("version", "1");
            String serverVersion = serverConfig.getString("version", "2");
            if (!defaultVersion.equals(serverVersion))
            {
                boolean changed = mergeConfigSections(defaultConfig, serverConfig, "");
                serverConfig.set("version", defaultVersion); // Always update version
                if (changed)
                {
                    serverConfig.save(configFile);
                    getLogger().info("Upgraded config.yml to version " + defaultVersion + ". New settings have been added where missing. Please review your config.");
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Recursively merges missing keys from source into target, preserving nested structure.
     * Returns true if any changes were made.
     */
    private boolean mergeConfigSections(YamlConfiguration source, YamlConfiguration target, String path)
    {
        boolean changed = false;
        for (String key : source.getConfigurationSection(path.isEmpty() ? "" : path).getKeys(false))
        {
            String fullKey = path.isEmpty() ? key : path + "." + key;
            if (source.isConfigurationSection(fullKey))
            {
                if (!target.isConfigurationSection(fullKey))
                {
                    target.createSection(fullKey);
                    changed = true;
                }
                if (mergeConfigSections(source, target, fullKey))
                {
                    changed = true;
                }
            }
            else
            {
                if (!target.contains(fullKey))
                {
                    target.set(fullKey, source.get(fullKey));
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Initializes the chat prefixes and text colors from the config.
     */
    private void initializeChat()
    {
        FileConfiguration config = getConfig();
        chatFormat = config.getString("ChatSettings.ChatFormat", DEFAULT_CHAT_FORMAT);
        allowColorCodes = config.getBoolean("ChatSettings.AllowColorCodes", true);
        allowUrl = config.getBoolean("ChatSettings.AllowClickableLinks", true);
        allowUrlUnderline = config.getBoolean("ChatSettings.AllowClickableLinksUnderline", true);
        localChatRange = config.getInt("ChatSettings.LocalChatRange", 1000);
        ChatPrefixes.initialize(config.getConfigurationSection("ChatPrefixes"));
        TextColors.initialize(config.getConfigurationSection("TextColors"));
    }
    
    /**
     * Checks for required or integrated plugins.
     */
    private void checkPlugins(PluginManager pm) 
    {
        Logger logger = getLogger();

        // - - - - - - - - - REQUIRED PLUGINS - - - - - - - - -
        factionsPlugin = (Factions) pm.getPlugin("Factions");
        if (factionsPlugin == null || !factionsPlugin.isEnabled()) 
        {
            logger.severe("Factions is required, but was not found or is disabled.");
            pm.disablePlugin(this);
            return;
        }
        logger.info("Factions detected.");

        Plugin massiveCorePlugin = pm.getPlugin("MassiveCore");
        if (massiveCorePlugin == null || !massiveCorePlugin.isEnabled()) 
        {
            logger.severe("MassiveCore is required, but was not found or is disabled.");
            pm.disablePlugin(this);
            return;
        }
        logger.info("MassiveCore detected.");

        // - - - - - - - - - OPTIONAL PLUGINS - - - - - - - - -
        discordSrvPlugin = (DiscordSRV) pm.getPlugin("DiscordSRV");
        if (discordSrvPlugin != null) 
        {
            DiscordSRV.api.subscribe(new DiscordSRVListener());
            logger.info("DiscordSRV detected - integration enabled.");
        }

        essentialsPlugin = (Essentials) pm.getPlugin("Essentials");
        if (essentialsPlugin != null) 
        {
            logger.info("Essentials detected.");
        }

        // PlaceholderAPI integration
        if (pm.getPlugin("PlaceholderAPI") != null) 
        {
            papiEnabled = true;
            logger.info("PlaceholderAPI detected.");
            new PlaceholderFactionsChat().register();
        } 
        else 
        {
            logger.info("PlaceholderAPI not found. Using internal tag parser for chat formatting.");
        }
    }

    /**
     * Checks for conflicting chat plugins that may interfere with FactionsChat.
     * If any unsupported chat plugins are found, FactionsChat will be disabled.
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
        unsupportedChatPlugins.add("VentureChat");
        
        // Check for unsupported/conflicting chat plugins
        for (String pluginName : unsupportedChatPlugins) 
        {
            if (pm.getPlugin(pluginName) != null) 
            {
                getLogger().warning(pluginName + " detected, which is not compatible with FactionsChat. Disabling FactionsChat.");
                pm.disablePlugin(this);
                return;
            }
        }
    }
}