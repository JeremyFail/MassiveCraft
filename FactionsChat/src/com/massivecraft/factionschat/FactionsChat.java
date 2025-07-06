package com.massivecraft.factionschat;

import com.earth2me.essentials.Essentials;
import com.massivecraft.factions.Factions;
import com.massivecraft.factionschat.commands.CmdFactionsChat;
import com.massivecraft.factionschat.integrations.PlaceholderFactionsChat;
import com.massivecraft.factionschat.listeners.DiscordSRVListener;
import com.massivecraft.factionschat.listeners.PaperFactionChatListener;
import com.massivecraft.factionschat.listeners.SpigotFactionChatListener;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.factions.cmd.CmdFactions;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
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
    
    private final Map<UUID, ChatMode> chatModes = new HashMap<>();
    private int localChatRange = 1000;
    private DiscordSRV discordSrvPlugin;
    private Factions factionsPlugin;
    private Essentials essentialsPlugin;
    // TODO: Reimplement?
    // private UpdateManager updateManager;
    
    private boolean papiEnabled = false;

    @Override
    public void onEnable() 
    {
        // Create chatmodes data file if it doesn't exist
        File chatmodesFile = new File(getDataFolder(), "chatmodes.yml");
        if (chatmodesFile.exists())
        {
            try (FileInputStream fileInputStream = new FileInputStream(chatmodesFile))
            {
                Yaml yaml = new Yaml();
                Map<String, String> data = yaml.load(fileInputStream);
                if (data != null)
                {
                    for (Map.Entry<String, String> entry : data.entrySet())
                    {
                        UUID id = UUID.fromString(entry.getKey());
                        chatModes.put(id, ChatMode.getChatModeByName(entry.getValue()));
                    }
                }
            } 
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
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
    public void reloadConfig()
    {
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
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("config.yml")));
            File configFile = new File(getDataFolder(), "config.yml");
            if (configFile.exists())
            {
                YamlConfiguration tmp = YamlConfiguration.loadConfiguration(configFile);
                boolean changesMade = false;
                for (String key : cfg.getKeys(true))
                {
                    if (!tmp.contains(key))
                    {
                        tmp.set(key, cfg.get(key));
                        changesMade = true;
                    }
                }

                if (changesMade)
                {
                    tmp.save(configFile);
                }
            }
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the chat prefixes and text colors from the config.
     */
    private void initializeChat()
    {
        FileConfiguration config = getConfig();
        localChatRange = config.getInt("LocalChatRange", 1000);
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
            logger.info("DiscordSRV detected.");
            DiscordSRV.api.subscribe(new DiscordSRVListener());
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
        List<String> unsupportedChatPlugins = new ArrayList<>();
        unsupportedChatPlugins.add("ChatControl");
        unsupportedChatPlugins.add("EssentialsChat");
        unsupportedChatPlugins.add("LPC");
        unsupportedChatPlugins.add("LuckPermsChat");
        unsupportedChatPlugins.add("AdvancedChat");
        unsupportedChatPlugins.add("FairyChat");
        unsupportedChatPlugins.add("LokiChat");
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