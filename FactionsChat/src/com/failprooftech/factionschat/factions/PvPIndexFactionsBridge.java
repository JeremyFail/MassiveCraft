package com.failprooftech.factionschat.factions;

import com.failprooftech.factionschat.ChatMode;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.FactionServiceImpl;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link FactionsBridge} implementation backed by PvPIndex Factions.
 *
 * <p>Construct via {@link #create(FactionService)} once {@code PvPIndexFactions}
 * has been confirmed present and enabled. The bridge reads relations directly
 * from the faction model's stored JSON, mirroring the logic in
 * {@code FactionServiceImpl}.</p>
 */
public class PvPIndexFactionsBridge implements FactionsBridge
{
    private final FactionService factionService;

    /**
     * {@code FactionServiceImpl} cast, used only for {@link #getPlayerTitle}
     * which requires repository access not exposed on the public interface.
     * May be {@code null} if the cast fails (future-proofing).
     */
    private final FactionServiceImpl factionServiceImpl;

    private PvPIndexFactionsBridge(FactionService factionService)
    {
        this.factionService = factionService;
        this.factionServiceImpl = (factionService instanceof FactionServiceImpl impl) ? impl : null;
    }

    public static PvPIndexFactionsBridge create(FactionService factionService)
    {
        if (factionService == null) throw new NullPointerException("factionService");
        return new PvPIndexFactionsBridge(factionService);
    }

    // --------------------------------------------------------------------- //
    // Faction membership
    // --------------------------------------------------------------------- //

    @Override
    public boolean isInFaction(Player player)
    {
        return factionService.isInFaction(player.getUniqueId());
    }

    @Override
    public String getFactionName(Player player)
    {
        return factionService.getFactionByPlayer(player.getUniqueId())
                .map(FactionModel::getName)
                .orElse("");
    }

    @Override
    public String getFactionNameForce(Player player)
    {
        return factionService.getFactionByPlayer(player.getUniqueId())
                .map(FactionModel::getName)
                .orElse("Wilderness");
    }

    // --------------------------------------------------------------------- //
    // Rank / title
    // --------------------------------------------------------------------- //

    @Override
    public String getPlayerRank(Player player)
    {
        if (!factionService.isInFaction(player.getUniqueId())) return "";
        return factionService.getRankByPlayer(player.getUniqueId())
                .map(RankModel::getName)
                .orElse("");
    }

    @Override
    public String getPlayerRankPrefix(Player player)
    {
        if (!factionService.isInFaction(player.getUniqueId())) return "";
        return factionService.getRankByPlayer(player.getUniqueId())
                .map(r -> r.getPrefix() != null ? r.getPrefix() : "")
                .orElse("");
    }

    @Override
    public String getPlayerRankForce(Player player)
    {
        return factionService.getRankByPlayer(player.getUniqueId())
                .map(RankModel::getName)
                .orElse(RankModel.RANK_MEMBER);
    }

    @Override
    public String getPlayerRankPrefixForce(Player player)
    {
        return factionService.getRankByPlayer(player.getUniqueId())
                .map(r -> r.getPrefix() != null ? r.getPrefix() : "")
                .orElse("");
    }

    @Override
    public String getPlayerTitle(Player player)
    {
        if (factionServiceImpl == null) return "";
        try
        {
            return factionServiceImpl.getRepos()
                    .players()
                    .find(player.getUniqueId().toString())
                    .map(pm -> {
                        String t = pm.getTitle();
                        return t != null ? t : "";
                    })
                    .orElse("");
        }
        catch (Exception e)
        {
            return "";
        }
    }

    // --------------------------------------------------------------------- //
    // Relations
    // --------------------------------------------------------------------- //

    @Override
    public String getRelationColor(Player sender, Player recipient)
    {
        return relationToLegacyColor(getRelationBetween(sender, recipient));
    }

    @Override
    public String getRelationName(Player sender, Player recipient)
    {
        return getRelationBetween(sender, recipient).name();
    }

    @Override
    public String getRelationNameLowercase(Player sender, Player recipient)
    {
        return getRelationBetween(sender, recipient).name().toLowerCase();
    }

    // --------------------------------------------------------------------- //
    // Chat filtering
    // --------------------------------------------------------------------- //

    @Override
    public boolean shouldExcludeByFactionRelation(ChatMode chatMode, Player sender, Player recipient)
    {
        Relation rel = getRelationBetween(sender, recipient);

        switch (chatMode)
        {
            case FACTION:
                return rel != Relation.MEMBER;

            case ALLY:
                return rel != Relation.MEMBER && rel != Relation.ALLY;

            case TRUCE:
                return rel != Relation.MEMBER && rel != Relation.ALLY && rel != Relation.TRUCE;

            case ENEMY:
                return rel != Relation.ENEMY;

            default:
                // GLOBAL, LOCAL, STAFF, WORLD - no faction-based filtering
                return false;
        }
    }

    // --------------------------------------------------------------------- //
    // Default relation colors
    // --------------------------------------------------------------------- //

    @Override
    public String getDefaultAllyColor()    { return "§b"; }

    @Override
    public String getDefaultTruceColor()   { return "§e"; }

    @Override
    public String getDefaultMemberColor()  { return "§a"; }

    @Override
    public String getDefaultEnemyColor()   { return "§c"; }

    @Override
    public String getDefaultNeutralColor() { return "§7"; }

    // --------------------------------------------------------------------- //
    // Internal helpers
    // --------------------------------------------------------------------- //

    /**
     * Returns the effective {@link Relation} from {@code sender}'s faction toward
     * {@code recipient}'s faction.
     *
     * <ul>
     *   <li>Same faction → {@link Relation#MEMBER}</li>
     *   <li>Entry in sender's {@code relationsJson} → that relation</li>
     *   <li>Otherwise → {@link Relation#NEUTRAL}</li>
     * </ul>
     */
    private Relation getRelationBetween(Player sender, Player recipient)
    {
        Optional<FactionModel> senderFactionOpt    = factionService.getFactionByPlayer(sender.getUniqueId());
        Optional<FactionModel> recipientFactionOpt = factionService.getFactionByPlayer(recipient.getUniqueId());

        // Either player has no faction → treat as neutral
        if (senderFactionOpt.isEmpty() || recipientFactionOpt.isEmpty())
        {
            return Relation.NEUTRAL;
        }

        FactionModel senderFaction    = senderFactionOpt.get();
        FactionModel recipientFaction = recipientFactionOpt.get();

        // Same faction
        if (senderFaction.getId().equals(recipientFaction.getId()))
        {
            return Relation.MEMBER;
        }

        // Look up stored relation from sender's faction toward recipient's faction
        Map<String, Relation> relations = parseRelationsJson(senderFaction.getRelationsJson());
        return relations.getOrDefault(recipientFaction.getId(), Relation.NEUTRAL);
    }

    /**
     * Parses a {@code relationsJson} string into a {@code Map<factionId, Relation>}.
     *
     * <p>Uses the same lightweight parser as {@code FactionServiceImpl} - no external
     * JSON library required.</p>
     *
     * <p>Format: {@code {"uuid":"ALLY","uuid2":"ENEMY"}}</p>
     */
    private static Map<String, Relation> parseRelationsJson(String json)
    {
        Map<String, Relation> out = new HashMap<>();
        if (json == null) return out;
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return out;
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) return out;

        for (String rawEntry : body.split(","))
        {
            String[] kv = rawEntry.split(":", 2);
            if (kv.length != 2) continue;
            String key   = stripQuotes(kv[0].trim());
            String value = stripQuotes(kv[1].trim());
            try
            {
                out.put(key, Relation.valueOf(value));
            }
            catch (IllegalArgumentException ignored) {}
        }
        return out;
    }

    private static String stripQuotes(String s)
    {
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\""))   s = s.substring(0, s.length() - 1);
        return s;
    }

    /**
     * Maps a PvPIndex {@link Relation} to a legacy Minecraft color code string.
     * Colors match those returned by {@link Relation#colorTag()} (MiniMessage).
     */
    private static String relationToLegacyColor(Relation relation)
    {
        return switch (relation)
        {
            case MEMBER  -> "§a"; // green
            case ALLY    -> "§b"; // aqua
            case TRUCE   -> "§e"; // yellow
            case NEUTRAL -> "§7"; // gray
            case ENEMY   -> "§c"; // red
        };
    }
}
