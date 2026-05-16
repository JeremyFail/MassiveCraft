package com.failprooftech.factionschat.commands;

import com.failprooftech.factionschat.FactionsChat;
import com.failprooftech.factionschat.util.ChatTxt;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /f c unignore <player>} - removes a player from the sender's ignore list.
 *
 * <p>Admin variant: {@code /f c unignore <managedPlayer> <targetPlayer>} requires
 * {@code factions.chat.ignore.admin}.</p>
 */
public final class CmdFactionsChatUnignore implements FactionsChatSubcommand
{
    private static final Set<String> ALIASES = Collections.unmodifiableSet(
        new LinkedHashSet<>(Arrays.asList("unignore")));

    @Override
    public Set<String> getAliases() { return ALIASES; }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if (sender instanceof Player && !sender.hasPermission("factions.chat.ignore"))
        {
            sender.sendMessage(ChatTxt.parse("<b>You don't have permission to use chat ignore commands."));
            return;
        }

        if (args.length == 0)
        {
            sender.sendMessage(ChatTxt.parse("<b>You must specify a player to unignore."));
            return;
        }

        String firstArg  = args[0];
        String secondArg = args.length > 1 ? args[1] : null;

        UUID    ignoringUuid;
        String  targetName;
        boolean admin = false;

        if (secondArg != null)
        {
            // Admin form: /f c unignore <managedPlayer> <playerToUnignore>
            if (sender instanceof Player && !sender.hasPermission("factions.chat.ignore.admin"))
            {
                sender.sendMessage(ChatTxt.parse("<b>You don't have permission to manage the ignore list of other players."));
                return;
            }
            OfflinePlayer managed = FactionsChat.instance.getIgnoreManager().getPlayerByNameOrUuid(firstArg);
            if (managed == null || (!managed.hasPlayedBefore() && !managed.isOnline()))
            {
                sender.sendMessage(ChatTxt.parse("<b>Player not found: <v>" + firstArg));
                return;
            }
            admin        = true;
            ignoringUuid = managed.getUniqueId();
            targetName   = secondArg;
        }
        else
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatTxt.parse("<b>You cannot unignore players as the console, only manage other players' ignore lists."));
                return;
            }
            ignoringUuid = ((Player) sender).getUniqueId();
            targetName   = firstArg;
        }

        OfflinePlayer target = FactionsChat.instance.getIgnoreManager().getPlayerByNameOrUuid(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline()))
        {
            sender.sendMessage(ChatTxt.parse("<b>Player not found: <v>" + targetName));
            return;
        }

        if (!FactionsChat.instance.getIgnoreManager().isIgnoring(ignoringUuid, target.getUniqueId()))
        {
            sender.sendMessage(admin
                ? ChatTxt.parse("<i>") + firstArg + ChatTxt.parse("<i> is not ignoring ") + target.getName() + ChatTxt.parse("<i>.")
                : ChatTxt.parse("<i>You are not ignoring ") + target.getName() + ChatTxt.parse("<i>."));
            return;
        }

        boolean removed = FactionsChat.instance.getIgnoreManager().removeIgnore(ignoringUuid, target.getUniqueId());
        if (removed)
        {
            sender.sendMessage(admin
                ? ChatTxt.parse("<g>Removed ") + target.getName() + ChatTxt.parse("<g> from ") + firstArg + ChatTxt.parse("<g>'s ignore list.")
                : ChatTxt.parse("<g>You are no longer ignoring ") + target.getName() + ChatTxt.parse("<g>."));
        }
        else
        {
            sender.sendMessage(ChatTxt.parse("<b>Failed to remove player from ignore list."));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        List<String> completions = new ArrayList<>();
        String input = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

        if (args.length == 1)
        {
            // Prioritise already-ignored players so they appear first
            if (sender instanceof Player)
            {
                UUID uid = ((Player) sender).getUniqueId();
                for (UUID ignoredUuid : FactionsChat.instance.getIgnoreManager().getIgnoredPlayers(uid))
                {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(ignoredUuid);
                    if (op.getName() != null && op.getName().toLowerCase().startsWith(input))
                        completions.add(op.getName());
                }
            }
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (!completions.contains(p.getName()) && p.getName().toLowerCase().startsWith(input))
                    completions.add(p.getName());
            }
        }
        else if (args.length == 2 && sender.hasPermission("factions.chat.ignore.admin"))
        {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(input)) completions.add(p.getName());
        }

        return completions;
    }
}
