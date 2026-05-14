package com.massivecraft.factionschat.util;

import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.TypeChatMode;
import com.massivecraft.factionschat.config.Settings;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;

/**
 * Parses leading quick-chat prefixes (see {@link Settings.QuickChat}) for one-off messages or mode toggles.
 */
public final class ColonChannelChatParser
{
    private ColonChannelChatParser()
    {
    }

    public enum ParseType
    {
        /** No quick-chat prefix - use the player's stored chat mode and the full message text. */
        NONE,
        /** {@code prefix + mode + body} - send {@link #getMessageBody()} on {@link #getTargetMode()}. */
        QUICK_MESSAGE,
        /** {@code prefix + mode} with no body - switch stored mode, do not send chat. */
        TOGGLE,
        /** Quick-chat rejected - cancel chat and show {@link ParseResult#getInvalidReason()}. */
        INVALID
    }

    public static final class ParseResult
    {
        private final ParseType type;
        private final ChatMode targetMode;
        private final String messageBody;
        private final String invalidReason;

        private ParseResult(ParseType type, ChatMode targetMode, String messageBody, String invalidReason)
        {
            this.type = type;
            this.targetMode = targetMode;
            this.messageBody = messageBody;
            this.invalidReason = invalidReason;
        }

        public ParseType getType()
        {
            return type;
        }

        public ChatMode getTargetMode()
        {
            return targetMode;
        }

        public String getMessageBody()
        {
            return messageBody;
        }

        public String getInvalidReason()
        {
            return invalidReason;
        }
    }

    /**
     * Classifies the line using {@link Settings.QuickChat#prefix} and {@link Settings.QuickChat#errorOnInvalidMode}.
     *
     * <p>Syntax (default prefix {@code :}):</p>
     * <ul>
     *   <li>No leading prefix - {@link ParseType#NONE}; body is the full line.</li>
     *   <li>{@code prefix + token} only - {@link ParseType#TOGGLE}.</li>
     *   <li>{@code prefix + token + remainder} - {@link ParseType#QUICK_MESSAGE}.</li>
     *   <li>Malformed quick-chat or unknown/disallowed token - {@link ParseType#INVALID} if
     *       {@link Settings.QuickChat#errorOnInvalidMode}; else {@link ParseType#NONE} with the full line.</li>
     *   <li>Token that is not letter/digit/underscore (e.g. emoticons with {@code :}) - always {@link ParseType#NONE}.</li>
     * </ul>
     */
    public static ParseResult parse(Player player, String plainText)
    {
        final String prefix = Settings.QuickChat.prefix;
        final boolean strictErrors = Settings.QuickChat.errorOnInvalidMode;

        if (plainText == null || !plainText.startsWith(prefix))
        {
            return new ParseResult(ParseType.NONE, null, plainText, null);
        }

        if (plainText.length() == prefix.length())
        {
            return strictErrors
                ? invalidQuickChatGeneric()
                : new ParseResult(ParseType.NONE, null, plainText, null);
        }

        String rest = plainText.substring(prefix.length());
        if (rest.isEmpty())
        {
            return strictErrors
                ? invalidQuickChatGeneric()
                : new ParseResult(ParseType.NONE, null, plainText, null);
        }

        int firstNonToken = -1;
        for (int i = 0; i < rest.length(); i++)
        {
            if (Character.isWhitespace(rest.charAt(i)))
            {
                firstNonToken = i;
                break;
            }
        }

        final String token;
        final String remainder;
        if (firstNonToken < 0)
        {
            token = rest;
            remainder = "";
        }
        else
        {
            token = rest.substring(0, firstNonToken);
            remainder = rest.substring(firstNonToken + 1).trim();
        }

        if (token.isEmpty())
        {
            return strictErrors
                ? invalidQuickChatGeneric()
                : new ParseResult(ParseType.NONE, null, plainText, null);
        }

        if (!looksLikeChatModeToken(token))
        {
            return new ParseResult(ParseType.NONE, null, plainText, null);
        }

        ChatMode mode = TypeChatMode.getInstance().read(token, player);
        if (mode == null)
        {
            return strictErrors
                ? invalidQuickChatGeneric()
                : new ParseResult(ParseType.NONE, null, plainText, null);
        }

        if (validateModeForPlayer(player, mode) != null)
        {
            return strictErrors
                ? invalidQuickChatGeneric()
                : new ParseResult(ParseType.NONE, null, plainText, null);
        }

        if (remainder.isEmpty())
        {
            return new ParseResult(ParseType.TOGGLE, mode, "", null);
        }

        return new ParseResult(ParseType.QUICK_MESSAGE, mode, remainder, null);
    }

    /**
     * Returns a ParseResult with type INVALID, null target mode, null message body, and the invalid reason.
     * @return A ParseResult with type INVALID, null target mode, null message body, and the invalid reason.
     */
    private static ParseResult invalidQuickChatGeneric()
    {
        return new ParseResult(ParseType.INVALID, null, null, Txt.parse("<b>Invalid chat mode or command."));
    }

    /**
     * Checks if the given token looks like a chat mode token.
     * 
     * @param token The token to check.
     * @return True if the token looks like a chat mode token, false otherwise.
     */
    private static boolean looksLikeChatModeToken(String token)
    {
        return token.codePoints().allMatch(cp ->
            Character.isLetterOrDigit(cp) || cp == '_');
    }

    /**
     * Validates the given chat mode for the given player.
     * 
     * @param player The player to validate the chat mode for.
     * @param mode The chat mode to validate.
     * @return {@code null} if allowed; otherwise a detail string (unused when {@link Settings.QuickChat#errorOnInvalidMode} is true).
     */
    private static String validateModeForPlayer(Player player, ChatMode mode)
    {
        if (!player.hasPermission("factions.chat." + mode.name().toLowerCase()))
        {
            return Txt.parse("<b>Invalid chat mode or command: <v>") + mode.name().toLowerCase();
        }

        MPlayer mPlayer = MPlayer.get(player);
        if (mPlayer.getFaction().isNone()
            && (mode == ChatMode.FACTION || mode == ChatMode.ALLY || mode == ChatMode.TRUCE
                || mode == ChatMode.ENEMY || mode == ChatMode.NEUTRAL))
        {
            return Txt.parse("<b>Cannot use that chat mode as you are not in a faction.");
        }
        return null;
    }
}
