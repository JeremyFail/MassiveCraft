package com.failprooftech.factionschat.factions;

import cc.javajobs.factionsbridge.bridge.infrastructure.struct.FPlayer;
import cc.javajobs.factionsbridge.bridge.infrastructure.struct.Faction;
import cc.javajobs.factionsbridge.bridge.infrastructure.struct.FactionsAPI;
import cc.javajobs.factionsbridge.bridge.infrastructure.struct.Relationship;
import cc.javajobs.factionsbridge.bridge.infrastructure.struct.Role;
import com.failprooftech.factionschat.ChatMode;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * {@link FactionsBridge} backed by the standalone
 * <a href="https://github.com/CallumJohnson/FactionsBridge">FactionsBridge</a> plugin (release 1.4.0+).
 *
 * <p>Compiled against the FactionsBridge API ({@code provided}); the plugin JAR must be on the server at runtime.</p>
 */
public final class PluginFactionsBridge implements FactionsBridge
{
    private final FactionsAPI api;
    private final String providerName;

    private PluginFactionsBridge(final FactionsAPI api)
    {
        this.api = api;
        this.providerName = api.getProvider();
    }

    /**
     * Attempts to wire a bridge from the installed FactionsBridge plugin.
     *
     * @param logger optional logger for diagnostics
     * @return empty when the plugin or API is unavailable
     */
    public static Optional<PluginFactionsBridge> tryCreate(final Logger logger)
    {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("FactionsBridge");
        if (plugin == null || !plugin.isEnabled())
        {
            return Optional.empty();
        }

        try
        {
            final FactionsAPI factionsApi = cc.javajobs.factionsbridge.FactionsBridge.getFactionsAPI();
            if (factionsApi == null)
            {
                if (logger != null)
                {
                    logger.warning(
                            "FactionsBridge plugin is enabled but its API is not ready "
                                    + "(no factions provider hooked). Falling back to direct integration when available.");
                }
                return Optional.empty();
            }
            return Optional.of(new PluginFactionsBridge(factionsApi));
        }
        catch (final RuntimeException | LinkageError ex)
        {
            if (logger != null)
            {
                logger.warning("Failed to initialise FactionsBridge plugin integration: " + ex.getMessage());
            }
            return Optional.empty();
        }
    }

    /**
     * @return the hooked factions implementation name reported by FactionsBridge (e.g. {@code SaberFactions})
     */
    public String getProviderName()
    {
        return this.providerName;
    }

    // --------------------------------------------------------------------- //
    // Faction membership
    // --------------------------------------------------------------------- //

    @Override
    public boolean isInFaction(final Player player)
    {
        final FPlayer fp = safeFPlayer(player);
        if (fp == null || !fp.hasFaction())
        {
            return false;
        }
        final Faction faction = fp.getFaction();
        return faction != null && !faction.isWilderness() && !faction.isServerFaction();
    }

    @Override
    public String getFactionName(final Player player)
    {
        final Faction faction = playerFaction(player);
        return faction == null ? "" : faction.getName();
    }

    @Override
    public String getFactionNameForce(final Player player)
    {
        final Faction faction = playerFaction(player);
        if (faction != null)
        {
            return faction.getName();
        }
        try
        {
            return this.api.getWilderness().getName();
        }
        catch (final RuntimeException ignored)
        {
            return "Wilderness";
        }
    }

    // --------------------------------------------------------------------- //
    // Rank / title
    // --------------------------------------------------------------------- //

    @Override
    public String getPlayerRank(final Player player)
    {
        if (!isInFaction(player))
        {
            return "";
        }
        final FPlayer fp = safeFPlayer(player);
        return fp == null ? "" : formatRole(fp.getRole());
    }

    @Override
    public String getPlayerRankPrefix(final Player player)
    {
        return "";
    }

    @Override
    public String getPlayerRankForce(final Player player)
    {
        final FPlayer fp = safeFPlayer(player);
        if (fp == null)
        {
            return Role.NORMAL.name();
        }
        return formatRole(fp.getRole());
    }

    @Override
    public String getPlayerRankPrefixForce(final Player player)
    {
        return "";
    }

    @Override
    public String getPlayerTitle(final Player player)
    {
        final FPlayer fp = safeFPlayer(player);
        if (fp == null)
        {
            return "";
        }
        final String title = fp.getTitle();
        return title == null ? "" : title;
    }

    // --------------------------------------------------------------------- //
    // Relations
    // --------------------------------------------------------------------- //

    @Override
    public String getRelationColor(final Player sender, final Player recipient)
    {
        return relationshipToLegacyColor(relationshipBetween(sender, recipient));
    }

    @Override
    public String getRelationName(final Player sender, final Player recipient)
    {
        return relationshipBetween(sender, recipient).name();
    }

    @Override
    public String getRelationNameLowercase(final Player sender, final Player recipient)
    {
        return getRelationName(sender, recipient).toLowerCase();
    }

    // --------------------------------------------------------------------- //
    // Chat filtering
    // --------------------------------------------------------------------- //

    @Override
    public boolean shouldExcludeByFactionRelation(final ChatMode chatMode, final Player sender, final Player recipient)
    {
        final Relationship rel = relationshipBetween(sender, recipient);

        return switch (chatMode)
        {
            case FACTION -> rel != Relationship.MEMBER;

            case ALLY -> rel != Relationship.MEMBER && rel != Relationship.ALLY;

            case TRUCE -> rel != Relationship.MEMBER && rel != Relationship.ALLY && rel != Relationship.TRUCE;

            case ENEMY -> rel != Relationship.ENEMY;

            default -> false;
        };
    }

    // --------------------------------------------------------------------- //
    // Default relation colors
    // --------------------------------------------------------------------- //

    @Override
    public String getDefaultAllyColor()
    {
        return relationshipToLegacyColor(Relationship.ALLY);
    }

    @Override
    public String getDefaultTruceColor()
    {
        return relationshipToLegacyColor(Relationship.TRUCE);
    }

    @Override
    public String getDefaultMemberColor()
    {
        return relationshipToLegacyColor(Relationship.MEMBER);
    }

    @Override
    public String getDefaultEnemyColor()
    {
        return relationshipToLegacyColor(Relationship.ENEMY);
    }

    @Override
    public String getDefaultNeutralColor()
    {
        return relationshipToLegacyColor(Relationship.NONE);
    }

    // --------------------------------------------------------------------- //
    // Internal helpers
    // --------------------------------------------------------------------- //

    private FPlayer safeFPlayer(final Player player)
    {
        try
        {
            return this.api.getFPlayer(player);
        }
        catch (final RuntimeException ignored)
        {
            return null;
        }
    }

    private Faction playerFaction(final Player player)
    {
        final FPlayer fp = safeFPlayer(player);
        if (fp == null || !fp.hasFaction())
        {
            return null;
        }
        final Faction faction = fp.getFaction();
        if (faction == null || faction.isWilderness() || faction.isServerFaction())
        {
            return null;
        }
        return faction;
    }

    private Relationship relationshipBetween(final Player sender, final Player recipient)
    {
        final FPlayer fSender = safeFPlayer(sender);
        final FPlayer fRecipient = safeFPlayer(recipient);
        if (fSender == null || fRecipient == null)
        {
            return Relationship.NONE;
        }
        try
        {
            return fSender.getRelationshipTo(fRecipient);
        }
        catch (final RuntimeException ignored)
        {
            return Relationship.NONE;
        }
    }

    private static String formatRole(final Role role)
    {
        if (role == null)
        {
            return Role.NORMAL.name();
        }
        return role.name();
    }

    private static String relationshipToLegacyColor(final Relationship relationship)
    {
        return switch (relationship)
        {
            case MEMBER -> "§a";
            case ALLY   -> "§b";
            case TRUCE  -> "§e";
            case ENEMY  -> "§c";
            case NONE   -> "§7";
        };
    }
}
