package com.massivecraft.factionschat.integrations;

import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.ChatPrefixes;
import com.massivecraft.factionschat.FactionsChat;
import com.massivecraft.factionschat.TextColors;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderFactionsChat extends PlaceholderExpansion
{
    @Override
    public String getIdentifier()
    {
        return "factions_chat";
    }

    @Override
    public String getAuthor()
    {
        return "Ymerejliaf";
    }

    @Override
    public String getVersion()
    {
        return FactionsChat.instance.getDescription().getVersion();
    }

    @Override
    public boolean persist()
    {
        return true;
    }

    @Override
    public boolean canRegister()
    {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String placeholder)
    {
        if (player == null || placeholder == null) return null;

        ChatMode playerChatMode;
        switch (placeholder.toLowerCase())
        {
            // The prefix for the chat mode the player is currently using
            case "prefix":
                playerChatMode = FactionsChat.instance.getPlayerChatModes().get(player.getUniqueId());
                if (playerChatMode != null)
                {
                    return ChatPrefixes.getPrefix(playerChatMode);
                }
                return "";

            // The color for the chat mode the player is currently using
            case "color":
                playerChatMode = FactionsChat.instance.getPlayerChatModes().get(player.getUniqueId());
                if (playerChatMode != null)
                {
                    return TextColors.getColor(playerChatMode);
                }
                return "";
            
            default:
                return null;
        }
    }
}
