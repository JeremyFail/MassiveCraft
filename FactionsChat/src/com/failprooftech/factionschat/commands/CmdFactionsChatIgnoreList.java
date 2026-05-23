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
 * {@code /f c ignorelist [page]} - shows the sender's ignore list with optional
 * pagination.
 *
 * <p>Admin variant: {@code /f c ignorelist <player> [page]} requires
 * {@code factions.chat.ignore.admin} and shows another player's list.</p>
 */
public final class CmdFactionsChatIgnoreList implements FactionsChatSubcommand
{
    private static final Set<String> ALIASES = Collections.unmodifiableSet(
        new LinkedHashSet<>(Arrays.asList("ignorelist", "ignored")));

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

        String firstArg  = args.length > 0 ? args[0] : null;
        String secondArg = args.length > 1 ? args[1] : null;

        UUID    targetUuid;
        String  targetName;
        boolean admin = false;
        int     page  = 1;

        if (firstArg != null)
        {
            // Is the first arg a page number?
            try
            {
                page = Integer.parseInt(firstArg);
                if (!(sender instanceof Player))
                {
                    sender.sendMessage(ChatTxt.parse("<b>You cannot view your own ignore list from the console, only other players' ignore lists."));
                    return;
                }
                targetUuid = ((Player) sender).getUniqueId();
                targetName = ((Player) sender).getName();
            }
            catch (NumberFormatException e)
            {
                // firstArg is a player name - admin viewing someone else's list
                if (sender instanceof Player && !sender.hasPermission("factions.chat.ignore.admin"))
                {
                    sender.sendMessage(ChatTxt.parse("<b>You don't have permission to view the ignore list of other players."));
                    return;
                }
                OfflinePlayer target = FactionsChat.instance.getIgnoreManager().getPlayerByNameOrUuid(firstArg);
                if (target == null || (!target.hasPlayedBefore() && !target.isOnline()))
                {
                    sender.sendMessage(ChatTxt.parse("<b>Player not found: <v>" + firstArg));
                    return;
                }
                admin      = true;
                targetUuid = target.getUniqueId();
                targetName = target.getName();

                if (secondArg != null)
                {
                    try   { page = Integer.parseInt(secondArg); }
                    catch (NumberFormatException e2)
                    {
                        sender.sendMessage(ChatTxt.parse("<b>\"<v>" + secondArg + "<b>\" is not a valid page number."));
                        return;
                    }
                }
            }
        }
        else
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatTxt.parse("<b>You cannot view your own ignore list from the console, only other players' ignore lists."));
                return;
            }
            targetUuid = ((Player) sender).getUniqueId();
            targetName = ((Player) sender).getName();
        }

        final UUID    finalUuid  = targetUuid;
        final String  finalName  = targetName;
        final boolean finalAdmin = admin;
        final int     finalPage  = page;

        // Fetch ignored-player set off the main thread then display on the main thread
        Bukkit.getScheduler().runTaskAsynchronously(FactionsChat.instance, () ->
        {
            Set<UUID> ignored = FactionsChat.instance.getIgnoreManager().getIgnoredPlayers(finalUuid);

            Bukkit.getScheduler().runTask(FactionsChat.instance, () ->
            {
                if (ignored.isEmpty())
                {
                    sender.sendMessage(finalAdmin
                        ? ChatTxt.parse("<i>") + finalName + ChatTxt.parse("<i> is not ignoring anyone.")
                        : ChatTxt.parse("<i>You are not ignoring anyone."));
                    return;
                }

                List<String> lines = new ArrayList<>();
                for (UUID uuid : ignored)
                {
                    OfflinePlayer op  = Bukkit.getOfflinePlayer(uuid);
                    String name       = op.getName() != null ? op.getName() : uuid.toString();
                    String status     = op.isOnline()
                        ? ChatTxt.parse("<g>Online")
                        : ChatTxt.parse("<n>Offline");
                    lines.add(ChatTxt.parse("<i>") + name + ChatTxt.parse("<i> - ") + status);
                }

                String title = finalAdmin ? finalName + "'s Ignore List" : "Your Ignore List";
                String pageCmd = FactionsChat.instance.getChatCommandPrefix() + " ignorelist" + (finalAdmin ? " " + finalName : "");
                FactionsChatDispatcher.sendPage(sender, lines, finalPage, title, pageCmd);
            });
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("factions.chat.ignore.admin"))
        {
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(input)) completions.add(p.getName());
        }
        return completions;
    }
}
