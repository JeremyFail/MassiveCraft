package com.massivecraft.factionschat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

// TODO: This file needs to be updated to reference new URLs for this fork. It is currently unused.
public class UpdateManager extends BukkitRunnable implements Listener
{
    @Override
    public void run()
    {
        new BukkitRunnable() 
        {
            @Override
            public void run() 
            {
                String newVersion = getNewVersion();
                try
                {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                
                if (newVersion == null)
                {
                    FactionsChat.instance.getLogger().info("You are up to date");
                } 
                else
                {
                    FactionsChat.instance.getLogger().warning("There is a new update available at https://dev.bukkit.org/projects/factions3chat/files");
                }
            }
        }.runTaskAsynchronously(FactionsChat.instance);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) 
    {
        if (!event.getPlayer().hasPermission("factions.chat.update"))
        {
            return;
        }

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (getNewVersion() != null)
                {
                    event.getPlayer().sendMessage("There is a new update of Factions3Chat available at https://dev.bukkit.org/projects/factions3chat/files");
                }
            }
        }.runTaskLaterAsynchronously(FactionsChat.instance, 6);
    }

    public String getNewVersion() 
    {
        try
        {
            URL url = new URL("https://servermods.forgesvc.net/servermods/files?projectids=359203");
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("User-Agent", "FactionsChat Update Checker");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = reader.readLine();
            reader.close();

            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(response, JsonArray.class);
            int currentVersion = Integer.parseInt(FactionsChat.instance.getDescription().getVersion().replace("v", "").replace(".", ""));

            if (jsonArray.size() == 0) 
            {
                FactionsChat.instance.getLogger().warning("No files found, or feed URL is bad");
                return null;
            }

            JsonObject latestVersionObject = jsonArray.get(jsonArray.size() - 1).getAsJsonObject();
            String latestVersion = latestVersionObject.get("name").getAsString();
            latestVersion = latestVersion.substring(latestVersion.lastIndexOf("v") + 1);
            int newVersion = Integer.parseInt(latestVersion.replace("v", "").replace(".", ""));

            return newVersion > currentVersion ? latestVersion : null;
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
            return null;
        }
    }
}
