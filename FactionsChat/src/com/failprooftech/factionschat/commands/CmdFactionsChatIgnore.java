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
 * {@code /f c ignore <player>} - adds a player to the sender's ignore list so
 * their faction-chat messages are hidden.
 *
 * <p>Admin variant: {@code /f c ignore <managedPlayer> <targetPlayer>} requires
 * {@code factions.chat.ignore.admin}.</p>
 */
public final class CmdFactionsChatIgnore implements FactionsChatSubcommand
{
    private static final Set<String> ALIASES = Collections.unmodifiableSet(
        new LinkedHashSet<>(Arrays.asList("ignore")));

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
            sender.sendMessage(ChatTxt.parse("<b>You must specify a player to ignore."));
            return;
        }

        String firstArg  = args[0];
        String secondArg = args.length > 1 ? args[1] : null;

        UUID    ignoringUuid;
        String  targetName;
        boolean admin = false;

        if (secondArg != null)
        {
            // Admin form: /f c ignore <managedPlayer> <playerToIgnore>
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
                sender.sendMessage(ChatTxt.parse("<b>You cannot ignore players from the console, only manage other players' ignore lists."));
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

        if (ignoringUuid.equals(target.getUniqueId()))
        {
            sender.sendMessage(admin
                ? ChatTxt.parse("<b>A player cannot ignore themselves.")
                : ChatTxt.parse("<b>You cannot ignore yourself."));
            return;
        }

        if (target.isOnline() && target.getPlayer().hasPermission("factions.chat.ignore.bypass"))
        {
            sender.sendMessage(admin
                ? ChatTxt.parse("<b>You cannot add ") + target.getName() + ChatTxt.parse("<b> to the ignore list because they have ignore bypass permission.")
                : ChatTxt.parse("<b>You cannot ignore ") + target.getName() + ChatTxt.parse("<b>."));
            return;
        }

        if (FactionsChat.instance.getIgnoreManager().isIgnoring(ignoringUuid, target.getUniqueId()))
        {
            sender.sendMessage(admin
                ? ChatTxt.parse("<b>") + firstArg + ChatTxt.parse("<b> is already ignoring ") + target.getName() + ChatTxt.parse("<b>.")
                : ChatTxt.parse("<b>You are already ignoring ") + target.getName() + ChatTxt.parse("<b>."));
            return;
        }

        FactionsChat.instance.getIgnoreManager().addIgnore(ignoringUuid, target.getUniqueId());
        sender.sendMessage(admin
            ? ChatTxt.parse("<g>Added ") + target.getName() + ChatTxt.parse("<g> to ") + firstArg + ChatTxt.parse("<g>'s ignore list.")
            : ChatTxt.parse("<g>You are now ignoring ") + target.getName() + ChatTxt.parse("<g>."));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        List<String> completions = new ArrayList<>();
        String input = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

        if (args.length == 1)
        {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(input)) completions.add(p.getName());
        }
        else if (args.length == 2 && sender.hasPermission("factions.chat.ignore.admin"))
        {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(input)) completions.add(p.getName());
        }

        return completions;
    }
}
