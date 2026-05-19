package com.failprooftech.factionschat.commands.registrar;

import com.massivecraft.factions.cmd.CmdFactions;
import com.massivecraft.factions.cmd.FactionsCommand;
import com.failprooftech.factionschat.commands.FactionsChatDispatcher;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Registers the {@code /f c} subcommand using MassiveCore's {@link FactionsCommand}
 * framework, which automatically hooks it into the {@code /f} command tree.
 *
 * <p>This registrar is only instantiated when MassiveCraft Factions is detected at
 * runtime.  All actual command logic is delegated to {@link FactionsChatCommandLogic}
 * so that it remains shared with the generic registrar.</p>
 */
public class MassiveFactionsCommandRegistrar implements FactionsCommandRegistrar
{
    /** The command instance we registered so we can reference it later if needed. */
    private FactionsCommand registeredCmd;

    @Override
    public void register(JavaPlugin plugin)
    {
        registeredCmd = new FactionsCommand()
        {
            {
                setDesc("Manage faction chat channels and settings");
                addAliases("chat", "c");
                // Subcommands consume extra tokens; disable strict overflow checking
                // so the parent command does not reject valid subcommand invocations
                setOverflowSensitive(false);
            }

            @Override
            public void perform()
            {
                // Convert MassiveCore's arg list to a plain String[] and delegate
                List<String> massiveArgs = getArgs();
                String[] args = massiveArgs.toArray(new String[0]);
                FactionsChatDispatcher.dispatch(this.sender, args);
            }

            @Override
            public List<String> getTabCompletions(List<String> args, CommandSender tabSender)
            {
                return FactionsChatDispatcher.tabComplete(tabSender, args.toArray(new String[0]));
            }
        };

        CmdFactions.get().addChild(registeredCmd);
    }

    @Override
    public void unregister(JavaPlugin plugin)
    {
        // MassiveCore does not expose a public removeChild() - the command is
        // automatically cleaned up when the plugin is disabled by Bukkit.
    }
}
