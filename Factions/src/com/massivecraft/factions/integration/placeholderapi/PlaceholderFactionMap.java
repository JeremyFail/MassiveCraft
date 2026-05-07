package com.massivecraft.factions.integration.placeholderapi;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Board;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.util.RelationUtil;
import com.massivecraft.massivecore.ps.PS;

/**
 * Builds chunk-based territory map lines for PlaceholderAPI (%factions_faction_map_row_N%).
 * Each cell is a colored block (█ - \u2588) using legacy § color codes from {@link MConf} 
 * placeholder map settings.
 */
public final class PlaceholderFactionMap
{
    private static final char BLOCK = '\u2588';

    private PlaceholderFactionMap() {}

    /**
     * Rounds a requested width/height: minimum 3, always odd (round even values up). No upper cap.
     */
    public static int roundMapDimension(int n)
    {
        if (n < 3) n = 3;
        if ((n & 1) == 0) n++;
        return n;
    }

    /**
     * Returns the string representation of a single row of the territory map for the given location.
     * 
     * @param mplayer The player to get the map row for.
     * @param playerLocationPs The location of the player to get the map row for.
     * @param rowOneBased The row to get (1 = top row, matching {@code faction_map_row_1}).
     * @return The string representation of the map row.
     */
    public static String getMapRow(MPlayer mplayer, PS playerLocationPs, int rowOneBased)
    {
        if (playerLocationPs == null || mplayer == null) return "";
        if (rowOneBased < 1) return "";

        MConf conf = MConf.get();
        int width = roundMapDimension(conf.placeholderMapWidth);
        int height = roundMapDimension(conf.placeholderMapHeight);
        if (rowOneBased > height) return "";

        // Match FactionScoreboard FactionsV2: world for Board, chunk-only PS for iteration (plusChunkCoords).
        String worldName = playerLocationPs.getWorld();
        if (worldName == null) return "";

        Board board = BoardColl.get().get(worldName);
        if (board == null) return "";

        PS cp = playerLocationPs.getChunkCoords(true);
        if (cp.getChunkX() == null || cp.getChunkZ() == null) return "";

        Faction pf = mplayer.getFaction();

        int halfWidth = width / 2;
        int halfHeight = height / 2;
        PS topLeft = cp.plusChunkCoords(-halfWidth, -halfHeight);

        int dz = rowOneBased - 1;
        StringBuilder row = new StringBuilder();

        // Loop through the width of the map
        for (int dx = 0; dx < width; dx++)
        {
            boolean isYou = dx == halfWidth && dz == halfHeight;
            PS here = topLeft.plusChunkCoords(dx, dz);
            Faction hf = board.getFactionAt(here);

            String colorCode;
            if (isYou)
            {
                colorCode = firstColorCode(conf.placeholderMapColorYou);
            }
            else if (hf.isNone())
            {
                colorCode = firstColorCode(conf.placeholderMapColorWilderness);
            }
            else if (hf.isWarZone())
            {
                colorCode = firstColorCode(conf.placeholderMapColorWarzone);
            }
            else if (hf.isSafeZone())
            {
                colorCode = firstColorCode(conf.placeholderMapColorSafezone);
            }
            else if (hf.getId().equals(pf.getId()))
            {
                colorCode = firstColorCode(conf.placeholderMapColorPlayerFaction);
            }
            else
            {
                Rel rel = RelationUtil.getRelationOfThatToMe(mplayer, hf);
                colorCode = relToColorCode(conf, rel);
            }

            row.append('\u00a7').append(colorCode).append(BLOCK);
        }

        return row.toString();
    }

    /**
     * Returns the color code for the given relation.
     * 
     * @param conf The configuration for the placeholder map.
     * @param rel The relation to get the color code for.
     * @return The color code for the given relation.
     */
    private static String relToColorCode(MConf conf, Rel rel)
    {
        switch (rel)
        {
            case ENEMY:
                return firstColorCode(conf.placeholderMapColorEnemy);
            case NEUTRAL:
                return firstColorCode(conf.placeholderMapColorNeutral);
            case TRUCE:
                return firstColorCode(conf.placeholderMapColorTruce);
            case ALLY:
                return firstColorCode(conf.placeholderMapColorAlly);
            case FACTION:
                return firstColorCode(conf.placeholderMapColorPlayerFaction);
            default:
                return firstColorCode(conf.placeholderMapColorNeutral);
        }
    }

    /**
     * Returns the first color code for the given configured color code.
     * 
     * @param configured The configured color code.
     * @return The first color code for the given configured color code.
     */
    private static String firstColorCode(String configured)
    {
        // If the configured color code is null or empty, return the default color code.
        if (configured == null || configured.isEmpty()) return "f";

        // Get the first character of the configured color code
        char c = configured.charAt(0);

        // If the first character is a §, return the second character if it exists, otherwise return the default color code.
        if (c == '\u00a7') 
        {
            return configured.length() > 1 ? String.valueOf(configured.charAt(1)) : "f";
        }
        return String.valueOf(c);
    }
}
