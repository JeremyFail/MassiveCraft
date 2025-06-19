package com.massivecraft.factionschat;

import com.massivecraft.massivecore.command.type.TypeAbstract;
import org.bukkit.command.CommandSender;
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
        
        for (ChatMode chatMode : ChatMode.values())
        {
            String arg = chatMode.name().toLowerCase();
            if (input != null && !arg.startsWith(input)) 
            {
                continue;
            }
            if (sender == null || !sender.hasPermission("factionschat." + chatMode.name().toLowerCase()))
            {
                continue;
            }
            args.add(arg);
        }
        
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
