package com.failprooftech.factionschat.factions;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.MPlayer;
import com.failprooftech.factionschat.ChatMode;

import org.bukkit.entity.Player;

/**
 * Creates a bridge between MassiveCraft Factions and FactionsChat.
 * 
 * @see FactionsBridge
 * @see MassiveFactionsBridge
 */
public class MassiveFactionsBridge implements FactionsBridge
{
    private static final MassiveFactionsBridge INSTANCE = new MassiveFactionsBridge();

    private MassiveFactionsBridge() {}

    /**
     * Gets the MassiveFactionsBridge instance.
     * 
     * @return The MassiveFactionsBridge instance.
     */
    public static MassiveFactionsBridge get() 
    { 
        return INSTANCE; 
    }

    // --------------------------------------------------------------------- //
    // Faction membership
    // --------------------------------------------------------------------- //

    @Override
    public boolean isInFaction(Player player)
    {
        return !MPlayer.get(player).getFaction().isNone();
    }

    @Override
    public String getFactionName(Player player)
    {
        MPlayer mp = MPlayer.get(player);
        if (mp.getFaction().isNone()) return "";
        return mp.getFaction().getName();
    }

    @Override
    public String getFactionNameForce(Player player)
    {
        return MPlayer.get(player).getFaction().getName();
    }

    // --------------------------------------------------------------------- //
    // Rank / title
    // --------------------------------------------------------------------- //

    @Override
    public String getPlayerRank(Player player)
    {
        MPlayer mp = MPlayer.get(player);
        if (mp.getFaction().isNone()) return "";
        return mp.getRank().getName();
    }

    @Override
    public String getPlayerRankPrefix(Player player)
    {
        MPlayer mp = MPlayer.get(player);
        if (mp.getFaction().isNone()) return "";
        return mp.getRank().getPrefix();
    }

    @Override
    public String getPlayerRankForce(Player player)
    {
        return MPlayer.get(player).getRank().getName();
    }

    @Override
    public String getPlayerRankPrefixForce(Player player)
    {
        return MPlayer.get(player).getRank().getPrefix();
    }

    @Override
    public String getPlayerTitle(Player player)
    {
        String title = MPlayer.get(player).getTitle();
        return title == null ? "" : title;
    }

    // --------------------------------------------------------------------- //
    // Relations
    // --------------------------------------------------------------------- //

    @Override
    public String getRelationColor(Player sender, Player recipient)
    {
        Rel rel = MPlayer.get(sender).getRelationTo(MPlayer.get(recipient));
        return rel.getColor().toString();
    }

    @Override
    public String getRelationName(Player sender, Player recipient)
    {
        return MPlayer.get(sender).getRelationTo(MPlayer.get(recipient)).name();
    }

    @Override
    public String getRelationNameLowercase(Player sender, Player recipient)
    {
        return getRelationName(sender, recipient).toLowerCase();
    }

    // --------------------------------------------------------------------- //
    // Chat filtering
    // --------------------------------------------------------------------- //

    @Override
    public boolean shouldExcludeByFactionRelation(ChatMode chatMode, Player sender, Player recipient)
    {
        MPlayer mSender    = MPlayer.get(sender);
        MPlayer mRecipient = MPlayer.get(recipient);

        switch (chatMode)
        {
            case FACTION:
                // Recipient must be in the same faction
                return !mSender.getFaction().equals(mRecipient.getFaction());

            case ALLY:
                // Recipient must be in the same faction OR an allied faction
            {
                Rel rel = mSender.getRelationTo(mRecipient);
                return rel != Rel.FACTION && rel != Rel.ALLY;
            }

            case TRUCE:
                // Recipient must be in the same faction, allied, or truced
            {
                Rel rel = mSender.getRelationTo(mRecipient);
                return rel != Rel.FACTION && rel != Rel.ALLY && rel != Rel.TRUCE;
            }

            case ENEMY:
                // Recipient must be an enemy of the sender
            {
                Rel rel = mSender.getRelationTo(mRecipient);
                return rel != Rel.ENEMY;
            }

            default:
                // Non-relational modes (GLOBAL, LOCAL, STAFF, WORLD) - no faction filtering
                return false;
        }
    }

    // --------------------------------------------------------------------- //
    // Default relation colors
    // --------------------------------------------------------------------- //

    @Override
    public String getDefaultAllyColor()
    {
        try
        {
            return com.massivecraft.factions.entity.MConf.get().colorAlly.toString();
        }
        catch (Exception e)
        {
            return "§b"; // aqua fallback
        }
    }

    @Override
    public String getDefaultTruceColor()
    {
        try
        {
            return com.massivecraft.factions.entity.MConf.get().colorTruce.toString();
        }
        catch (Exception e)
        {
            return "§3"; // dark aqua fallback
        }
    }

    @Override
    public String getDefaultMemberColor()
    {
        try
        {
            return com.massivecraft.factions.entity.MConf.get().colorMember.toString();
        }
        catch (Exception e)
        {
            return "§a"; // green fallback
        }
    }

    @Override
    public String getDefaultEnemyColor()
    {
        try
        {
            return com.massivecraft.factions.entity.MConf.get().colorEnemy.toString();
        }
        catch (Exception e)
        {
            return "§c"; // red fallback
        }
    }

    @Override
    public String getDefaultNeutralColor()
    {
        try
        {
            return com.massivecraft.factions.entity.MConf.get().colorNeutral.toString();
        }
        catch (Exception e)
        {
            return "§f"; // white fallback
        }
    }
}
