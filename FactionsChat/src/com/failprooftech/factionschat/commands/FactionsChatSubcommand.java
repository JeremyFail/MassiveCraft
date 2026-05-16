package com.failprooftech.factionschat.commands;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;

/**
 * Contract for a single {@code /f c} subcommand.
 *
 * <p>Each subcommand is self-contained: it knows its own aliases, performs its
 * own permission checks, and produces its own tab-completion candidates.
 * {@link FactionsChatDispatcher} routes incoming tokens to the correct
 * implementation.</p>
 *
 * <p>{@code args} in both methods is everything <em>after</em> the subcommand
 * token itself - e.g. for {@code /f c ignore SomePlayer} the ignore command
 * receives {@code ["SomePlayer"]}.</p>
 */
public interface FactionsChatSubcommand
{
    /**
     * Lowercase alias tokens that trigger this subcommand
     * (e.g. {@code {"help", "h", "?"}}).
     */
    Set<String> getAliases();

    /**
     * Execute the subcommand.
     *
     * @param sender the command sender
     * @param args   tokens after the subcommand token; may be empty
     */
    void execute(CommandSender sender, String[] args);

    /**
     * Produce tab-completion candidates.
     *
     * @param sender the command sender
     * @param args   tokens after the subcommand token; last element is the partial
     *               input the player is currently typing
     * @return mutable list of candidates; never {@code null}
     */
    List<String> tabComplete(CommandSender sender, String[] args);
}
