package com.failprooftech.factionschat.testsupport;

import com.failprooftech.factionschat.config.Settings;

/**
 * Saves and restores mutable {@link Settings} fields so MockBukkit chat tests do not leak
 * global configuration across methods or classes.
 *
 * <p>Typical usage: {@code SettingsSnapshot snap = SettingsSnapshot.capture();} in
 * {@code @BeforeEach}, then {@code snap.restore();} in {@code @AfterEach}.</p>
 *
 * <p>Covered fields: chat format, color/URL toggles, local range, chat-reporting /
 * upstream-component flags, and {@link Settings.QuickChat} prefix / invalid-mode error.</p>
 */
public final class SettingsSnapshot
{
    private final String chatFormat;
    private final boolean allowColorCodes;
    private final boolean allowUrl;
    private final boolean allowUrlUnderline;
    private final int localChatRange;
    private final boolean disableChatReporting;
    private final boolean preserveUpstreamChatComponents;
    private final String quickChatPrefix;
    private final boolean quickChatErrorOnInvalidMode;

    /** Captures the current values of the tracked {@link Settings} fields. */
    private SettingsSnapshot()
    {
        this.chatFormat = Settings.chatFormat;
        this.allowColorCodes = Settings.allowColorCodes;
        this.allowUrl = Settings.allowUrl;
        this.allowUrlUnderline = Settings.allowUrlUnderline;
        this.localChatRange = Settings.localChatRange;
        this.disableChatReporting = Settings.disableChatReporting;
        this.preserveUpstreamChatComponents = Settings.preserveUpstreamChatComponents;
        this.quickChatPrefix = Settings.QuickChat.prefix;
        this.quickChatErrorOnInvalidMode = Settings.QuickChat.errorOnInvalidMode;
    }

    /**
     * Creates a snapshot of the current mutable {@link Settings} values of interest to chat tests.
     *
     * @return a new snapshot that can later {@link #restore()} those values
     */
    public static SettingsSnapshot capture()
    {
        return new SettingsSnapshot();
    }

    /**
     * Writes the captured values back onto the static {@link Settings} fields.
     *
     * <p>Call from teardown after tests mutate settings; does not touch fields outside the
     * snapshot set.</p>
     */
    public void restore()
    {
        Settings.chatFormat = chatFormat;
        Settings.allowColorCodes = allowColorCodes;
        Settings.allowUrl = allowUrl;
        Settings.allowUrlUnderline = allowUrlUnderline;
        Settings.localChatRange = localChatRange;
        Settings.disableChatReporting = disableChatReporting;
        Settings.preserveUpstreamChatComponents = preserveUpstreamChatComponents;
        Settings.QuickChat.prefix = quickChatPrefix;
        Settings.QuickChat.errorOnInvalidMode = quickChatErrorOnInvalidMode;
    }
}
