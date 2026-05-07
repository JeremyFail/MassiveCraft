package com.massivecraft.factionschat;

import com.massivecraft.massivecore.command.type.TypeAbstract;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A MassiveCraft {@link TypeAbstract} that represents the supported ChatModes.
 */
public class TypeChatMode extends TypeAbstract<ChatMode> 
{
    private static final TypeChatMode INSTANCE = new TypeChatMode();
    
    public TypeChatMode()
    {
        super(ChatMode.class);
    }
    
    public static TypeChatMode getInstance() 
    {
        return INSTANCE;
    }

    @Override
    public Collection<String> getTabList(CommandSender sender, String input) 
    {
        Collection<String> args = new ArrayList<>();
        if (sender == null || !(sender instanceof Player)) return args;
        Player player = (Player) sender;
        
        for (ChatMode chatMode : ChatMode.getAvailableChatModes(player))
        {
            // Add the full mode name and the alias to the tab list
            if (input != null && input.length() > 0)
            {
                String arg = chatMode.name().toLowerCase();
                if (arg.startsWith(input))
                {
                    args.add(arg);
                }
                String alias = chatMode.getAlias();
                if (alias.startsWith(input))
                {
                    args.add(alias);
                }
            }
            else
            {
                args.add(chatMode.name().toLowerCase());
                args.add(chatMode.getAlias());
            }

            // Add the "public" alias for global chat
            if (ChatMode.GLOBAL.equals(chatMode))
            {
                if (input != null && input.length() > 0)
                {
                    String arg = "public";
                    if (arg.startsWith(input))
                    {
                        args.add(arg);
                    }
                    String alias = "p";
                    if (alias.startsWith(input))
                    {
                        args.add(alias);
                    }
                }
                else
                {
                    args.add("public");
                    args.add("p");
                }
            }
        }
        return args;
    }

    @Override
    public ChatMode read(String input, CommandSender sender) 
    {
        if (input == null) 
        {
            return null;
        } 
        else if (input.equalsIgnoreCase("public") || input.equalsIgnoreCase("p"))
        {
            return ChatMode.GLOBAL;
        }
        return ChatMode.getChatModeByName(input.toUpperCase());
    }
}
