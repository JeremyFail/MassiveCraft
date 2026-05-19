package com.failprooftech.factionschat.commands.teamsapi;

import com.failprooftech.factionschat.commands.FactionsChatDispatcher;

import com.skyblockexp.teamsapi.api.AbstractTeamsSubcommand;

import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * TeamsAPI {@link com.skyblockexp.teamsapi.api.TeamsSubcommand} that forwards to {@link FactionsChatDispatcher}.
 * Registered under names {@code chat} and {@code c} so PvPIndex-Factions can expose {@code /f chat …} / {@code /f c …}
 * via its TeamsAPI dispatch hook (see ez-plugins TeamsAPI consumer subcommands guide).
 */
public final class FactionsChatTeamsSubcommand extends AbstractTeamsSubcommand
{
    public FactionsChatTeamsSubcommand(final String name, final String description)
    {
        super(name, description, null);
    }

    @Override
    public String getUsage()
    {
        return getName() + " [subcommand|mode] [args]";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args)
    {
        final String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        FactionsChatDispatcher.dispatch(sender, subArgs);
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String[] args)
    {
        final String[] inner = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        return FactionsChatDispatcher.tabComplete(sender, inner);
    }
}
