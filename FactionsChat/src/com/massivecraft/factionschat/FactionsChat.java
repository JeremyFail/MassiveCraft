package com.massivecraft.factionschat;

import com.earth2me.essentials.Essentials;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factionschat.commands.CmdFactionsChat;
import com.massivecraft.factionschat.commands.CmdQuickMessage;
import com.massivecraft.factionschat.listeners.DiscordSRVListener;
import com.massivecraft.factionschat.listeners.FactionChatListener;
import com.massivecraft.factions.cmd.CmdFactions;
import github.scarsz.discordsrv.DiscordSRV;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class FactionsChat extends JavaPlugin 
{
    private final Map<UUID, ChatMode> chatModes = new HashMap<>();
    private int localChatRange = 1000;
    private DiscordSRV discordSrvPlugin;
    private Factions factionsPlugin;
    private Essentials essentialsPlugin;
    private UpdateManager updateManager;
    
    @Override
    public void onEnable() 
    {
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

        saveDefaultConfig();
        updateConfig();

        CmdFactions.get().addChild(new CmdFactionsChat());
        CmdFactions.get().addChild(new CmdQuickMessage());

        localChatRange = getConfig().getInt("LocalChatRange", 1000);

        setupChatPrefixes();

        checkPlugins();

        updateManager = new UpdateManager();

        getServer().getPluginManager().registerEvents(new FactionChatListener(), this);
        getServer().getPluginManager().registerEvents(updateManager, this);
        updateManager.run();
    }
    
    public Map<UUID, ChatMode> getPlayerChatModes()
    {
        return chatModes;
    }

    public int getLocalChatRange() 
    {
        return this.localChatRange;
    }

    public DiscordSRV getDiscordSrvPlugin()
    {
        return this.discordSrvPlugin;
    }
    public Factions getFactionsPlugin() 
    {
        return this.factionsPlugin;
    }
        
    public Essentials getEssentialsPlugin()
    {
        return this.essentialsPlugin;
    }

    private void setupChatPrefixes() 
    {
        ConfigurationSection chatPrefix = getConfig().getConfigurationSection("ChatPrefixes");
        if (chatPrefix != null) 
        {
            ChatPrefixes.ALLY = chatPrefix.getString("Ally", "§e[<fcolor>ALLY§e]§r ")
                    .replace("<fcolor>", MConf.get().colorAlly.toString());
            ChatPrefixes.TRUCE = chatPrefix.getString("Truce", "§e[<fcolor>TRUCE§e]§r ")
                    .replace("<fcolor>", MConf.get().colorTruce.toString());
            ChatPrefixes.FACTION = chatPrefix.getString("Faction", "§e[<fcolor>FACTION§e]§r ")
                    .replace("<fcolor>", MConf.get().colorMember.toString());
            ChatPrefixes.ENEMY = chatPrefix.getString("Enemy", "§e[<fcolor>ENEMY§e]§r ")
                    .replace("<fcolor>", MConf.get().colorEnemy.toString());
            ChatPrefixes.NEUTRAL = chatPrefix.getString("Neutral", "§e[<fcolor>NEUTRAL§e]§r ")
                    .replace("<fcolor>", MConf.get().colorNeutral.toString());
            ChatPrefixes.LOCAL = chatPrefix.getString("Local", "§e[§rLOCAL§e]§r ");
            ChatPrefixes.GLOBAL = chatPrefix.getString("Global", "§e[§6GLOBAL§e]§r ");
            ChatPrefixes.STAFF = chatPrefix.getString("Staff", "§e[§4STAFF§e]§r ");
            ChatPrefixes.WORLD = chatPrefix.getString("World", "§e[§3WORLD§e]§r ");
        }
        TextColors.initialize(getConfig().getConfigurationSection("TextColors"));
    }

    private void checkPlugins() 
    {
        Logger logger = getLogger();
        factionsPlugin = (Factions) getServer().getPluginManager().getPlugin("Factions");
        if (factionsPlugin == null || !factionsPlugin.isEnabled()) 
        {
            logger.severe("Factions is required, but was not found or is disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        logger.info("Factions detected");

        discordSrvPlugin = (DiscordSRV) getServer().getPluginManager().getPlugin("DiscordSRV");
        if (discordSrvPlugin != null) 
        {
            logger.info("DiscordSRV detected");
            DiscordSRV.api.subscribe(new DiscordSRVListener());
        }

        essentialsPlugin = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
        if (essentialsPlugin != null) 
        {
            logger.info("Essentials detected");
        }
    }

    @Override
    public void onLoad() 
    {
        instance = this;
    }

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

    public static FactionsChat instance;
}