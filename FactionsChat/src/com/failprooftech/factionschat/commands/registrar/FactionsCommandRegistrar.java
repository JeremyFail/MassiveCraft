package com.failprooftech.factionschat.commands.registrar;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Abstraction over how FactionsChat registers its {@code /f c} subcommand.
 *
 * <p>Different Factions plugins expose their root {@code /f} command through
 * different frameworks (e.g. MassiveCore's {@code FactionsCommand} tree vs a
 * plain Bukkit {@code PluginCommand}).  Implementations of this interface handle
 * the integration point; all shared command logic lives in
 * {@link com.failprooftech.factionschat.commands.FactionsChatCommandLogic}.</p>
 */
public interface FactionsCommandRegistrar
{
    /**
     * Register the FactionsChat subcommand(s) so that {@code /f c} is handled.
     *
     * @param plugin the owning plugin instance
     */
    void register(JavaPlugin plugin);

    /**
     * Remove or restore any hooks installed during {@link #register}.
     * Called on plugin disable.
     *
     * @param plugin the owning plugin instance
     */
    void unregister(JavaPlugin plugin);
}
