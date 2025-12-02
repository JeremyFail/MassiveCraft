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
            String arg = chatMode.name().toLowerCase();
            if (input == null || !arg.startsWith(input)) 
            {
                continue;
            }
            args.add(arg);
        }
        
        // Add the public chat mode option
        args.add("public");
        args.add("p");

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
