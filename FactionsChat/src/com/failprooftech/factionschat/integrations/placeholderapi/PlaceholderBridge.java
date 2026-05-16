package com.failprooftech.factionschat.integrations.placeholderapi;

/**
 * Common contract for PlaceholderAPI integration bridges.
 *
 * <p>FactionsChat selects the appropriate implementation at startup based on the
 * Factions plugin that is installed:</p>
 * <ul>
 *   <li>{@link MassivePlaceholderBridge} – hooks into the existing {@code factions}
 *       PAPI expansion via MassiveCraft's {@code PlaceholderExpander} API, so
 *       chat placeholders share the {@code %factions_*} namespace.</li>
 *   <li>{@link GenericPlaceholderBridge} – registers a standalone expansion under
 *       the {@code factionschat} identifier ({@code %factionschat_*}).</li>
 * </ul>
 */
public interface PlaceholderBridge
{
    /** Register this bridge with PlaceholderAPI. Called when FactionsChat enables. */
    void activate();

    /** Unregister this bridge from PlaceholderAPI. Called when FactionsChat disables. */
    void deactivate();
}
