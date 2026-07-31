package com.failprooftech.factionschat.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies loading chat settings from Bukkit YAML, including defaults and
 * normalization of explicitly configured values.
 */
class SettingsLoadTest
{
    /**
     * Verifies that a minimal chat configuration loads explicit values and fills optional defaults.
     */
    @Test
    void loadsDefaultsFromMinimalYaml()
    {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("ChatSettings.ChatFormat", Settings.DEFAULT_CHAT_FORMAT);
        yaml.set("ChatSettings.AllowColorCodes", true);
        yaml.set("ChatSettings.AllowClickableLinks", true);
        yaml.set("ChatSettings.AllowClickableLinksUnderline", true);
        yaml.set("ChatSettings.LocalChatRange", 1000);
        yaml.set("ChatSettings.DisableChatReporting", false);
        yaml.set("ChatSettings.PreserveUpstreamChatComponents", true);

        Settings.load(yaml, null);

        assertTrue(Settings.allowColorCodes);
        assertTrue(Settings.allowUrl);
        assertTrue(Settings.allowUrlUnderline);
        assertEquals(1000, Settings.localChatRange);
        assertFalse(Settings.disableChatReporting);
        assertTrue(Settings.preserveUpstreamChatComponents);
        assertEquals(":", Settings.QuickChat.prefix);
        assertFalse(Settings.QuickChat.errorOnInvalidMode);
        assertFalse(Settings.blacklistedMiniMessageCommands.isEmpty());
    }

    /**
     * Verifies that an explicitly empty command blacklist is not replaced by defaults.
     */
    @Test
    void emptyBlacklistListIsHonored()
    {
        YamlConfiguration yaml = baseChatSettings();
        yaml.set("ChatSettings.BlacklistedMiniMessageCommands", java.util.List.of());

        Settings.load(yaml, null);

        assertTrue(Settings.blacklistedMiniMessageCommands.isEmpty());
    }

    /**
     * Verifies that a blank quick-chat prefix is normalized to the default prefix.
     */
    @Test
    void quickChatPrefixFallsBackWhenBlank()
    {
        YamlConfiguration yaml = baseChatSettings();
        yaml.set("ChatSettings.QuickChat.Prefix", "   ");

        Settings.load(yaml, null);

        assertEquals(":", Settings.QuickChat.prefix);
    }

    /**
     * Creates the smallest reusable configuration containing required chat settings.
     *
     * @return a configuration suitable for focused settings overrides
     */
    private static YamlConfiguration baseChatSettings()
    {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("ChatSettings.ChatFormat", "%MESSAGE%");
        yaml.set("ChatSettings.AllowColorCodes", true);
        yaml.set("ChatSettings.AllowClickableLinks", true);
        yaml.set("ChatSettings.AllowClickableLinksUnderline", true);
        yaml.set("ChatSettings.LocalChatRange", 50);
        return yaml;
    }
}
