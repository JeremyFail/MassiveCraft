package com.massivecraft.factionschat.util;

import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factionschat.ChatMode;
import com.massivecraft.factionschat.TypeChatMode;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;

/**
 * Parses leading {@code :channel} / {@code :letter} prefixes in chat for 
 * quick one-off messages or mode toggles.
 */
public final class ColonChannelChatParser
{
    /**
     * Private constructor to prevent instantiation.
     */
    private ColonChannelChatParser()
    {
    }

    /**
     * Enum of parse types.
     */
    public enum ParseType
    {
        /** No colon prefix - use the player's stored chat mode and the full message text. */
        NONE,
        /** {@code :mode message} - send {@link #getMessageBody()} on {@link #getTargetMode()}. */
        QUICK_MESSAGE,
        /** {@code :mode} with no message - switch stored mode, do not send chat. */
        TOGGLE,
        /** Unrecognized channel or not allowed - cancel chat and show {@link #getInvalidReason()}. */
        INVALID
    }

    /**
     * Result of parsing a colon channel chat message.
     */
    public static final class ParseResult
    {
        private final ParseType type;
        private final ChatMode targetMode;
        private final String messageBody;
        private final String invalidReason;

        /**
         * Creates a new ParseResult with the specified type, target mode, message body, and invalid reason.
         * 
         * @param type The type of parse result.
         * @param targetMode The target mode.
         * @param messageBody The message body.
         * @param invalidReason The invalid reason.
         */
        private ParseResult(ParseType type, ChatMode targetMode, String messageBody, String invalidReason)
        {
            this.type = type;
            this.targetMode = targetMode;
            this.messageBody = messageBody;
            this.invalidReason = invalidReason;
        }

        /**
         * Retrieves the type of parse result.
         * 
         * @return The type of parse result.
         */
        public ParseType getType()
        {
            return type;
        }

        /**
         * Retrieves the target mode.
         * 
         * @return The target mode.
         */
        public ChatMode getTargetMode()
        {
            return targetMode;
        }

        /**
         * Retrieves the message body.
         * 
         * @return The message body.
         */
        public String getMessageBody()
        {
            return messageBody;
        }
        
        /**
         * Retrieves the invalid reason.
         * 
         * @return The invalid reason.
         */
        public String getInvalidReason()
        {
            return invalidReason;
        }
    }

    /**
     * Inspects the chat line and classifies it as normal chat, a colon-prefixed quick message,
     * a colon-only channel toggle, or an invalid prefix.
     *
     * <p>Syntax:</p>
     * <ul>
     *   <li>No leading {@code :} - {@link ParseType#NONE}; {@code messageBody} is the full line (use stored chat mode).</li>
     *   <li>{@code :token} with no text after - {@link ParseType#TOGGLE}; switch player to {@code targetMode}.</li>
     *   <li>{@code :token remainder} - {@link ParseType#QUICK_MESSAGE}; send {@code remainder} on {@code targetMode}.</li>
     *   <li>Malformed prefix or unknown token - {@link ParseType#INVALID}; show {@link ParseResult#getInvalidReason()}.</li>
     * </ul>
     *
     * <p>Token resolution matches {@link TypeChatMode} (full names, single-letter aliases, {@code public}/{@code p} for global).</p>
     *
     * @param player    sender; used for permission and faction checks
     * @param plainText plain text of the chat line (Paper: from {@link net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer};
     *                  Spigot: {@link org.bukkit.event.player.AsyncPlayerChatEvent#getMessage()})
     * @return a {@link ParseResult}; never {@code null}; {@code plainText} null yields {@link ParseType#NONE} with null body
     */
    public static ParseResult parse(Player player, String plainText)
    {
        // Not a channel prefix - listeners use the player's saved mode and the whole string as the message.
        if (plainText == null || !plainText.startsWith(":"))
        {
            return new ParseResult(ParseType.NONE, null, plainText, null);
        }
        // Lone ":" is not a valid channel token.
        if (plainText.length() == 1)
        {
            return invalid(Txt.parse("<b>Invalid channel prefix."));
        }

        // Everything after the first colon is either "token", "token rest", or whitespace issues.
        String rest = plainText.substring(1);
        if (rest.isEmpty())
        {
            return invalid(Txt.parse("<b>Invalid channel prefix."));
        }

        // First run of non-whitespace characters after ":" is the mode token; the rest is the optional message body.
        int firstNonToken = -1;
        for (int i = 0; i < rest.length(); i++)
        {
            if (Character.isWhitespace(rest.charAt(i)))
            {
                firstNonToken = i;
                break;
            }
        }

        String token;
        String remainder;
        if (firstNonToken < 0)
        {
            // e.g. ":faction" - toggle only, no trailing message.
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
            return invalid(Txt.parse("<b>Invalid channel prefix."));
        }

        ChatMode mode = TypeChatMode.getInstance().read(token, player);
        if (mode == null)
        {
            return invalid(Txt.parse("<b>Invalid chat mode or command: <v>") + token
                + Txt.parse("<n> Use <k>/f c help<n> for modes."));
        }

        String validationError = validateModeForPlayer(player, mode);
        if (validationError != null)
        {
            return new ParseResult(ParseType.INVALID, null, null, validationError);
        }

        if (remainder.isEmpty())
        {
            return new ParseResult(ParseType.TOGGLE, mode, "", null);
        }

        return new ParseResult(ParseType.QUICK_MESSAGE, mode, remainder, null);
    }

    /**
     * Builds an {@link ParseType#INVALID} result with a player-facing reason (includes color codes).
     *
     * @param reason message to send to the player when the line is rejected
     */
    private static ParseResult invalid(String reason)
    {
        return new ParseResult(ParseType.INVALID, null, null, reason);
    }

    /**
     * Ensures the resolved mode is usable for this player: faction-only channels require membership,
     * and {@code factions.chat.&lt;mode&gt;} must be granted.
     *
     * @param player sender
     * @param mode   non-null mode already parsed from the token
     * @return {@code null} if allowed; otherwise a colored string for {@link ParseType#INVALID}
     */
    private static String validateModeForPlayer(Player player, ChatMode mode)
    {
        MPlayer mPlayer = MPlayer.get(player);
        if (mPlayer.getFaction().isNone()
            && (mode == ChatMode.FACTION || mode == ChatMode.ALLY || mode == ChatMode.TRUCE
                || mode == ChatMode.ENEMY || mode == ChatMode.NEUTRAL))
        {
            return Txt.parse("<b>Cannot use that chat mode as you are not in a faction.");
        }
        if (!player.hasPermission("factions.chat." + mode.name().toLowerCase()))
        {
            return Txt.parse("<b>Invalid chat mode or command: <v>") + mode.name().toLowerCase();
        }
        return null;
    }
}
