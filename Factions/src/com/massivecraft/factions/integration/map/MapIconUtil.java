package com.massivecraft.factions.integration.map;

import com.massivecraft.factions.entity.MConf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Shared utilities for generating marker icons used by map integrations (Pl3xMap, SquareMap).
 *
 * <p>
 * Map plugins that require icon keys to be registered (so markers display) can use this class to:
 * <ul>
 * <li>Obtain the set of icon keys configured for warp markers ({@link #getConfiguredWarpIconKeys})</li>
 * <li>Generate a consistent color for any icon key ({@link #colorForIconKey})</li>
 * <li>Create a marker image (circle with border) for a given color or key ({@link #createMarkerImage}, {@link #createMarkerImageForKey})</li>
 * </ul>
 * Each integration registers these keys with its own API (Pl3xMap IconImage, SquareMap BufferedImage)
 * so that configured icons survive plugin startup (unregistered images are removed by the map plugin).
 * </p>
 *
 * <p>
 * Known keys "redflag" and "greenflag" use fixed colors for backwards compatibility; any other
 * key gets a consistent color derived from the key hash so admins can set mapWarpHomeIcon /
 * mapWarpOtherIcon to any name and get a visible marker.
 * </p>
 */
public final class MapIconUtil
{
	private MapIconUtil() {}

	public static final int DEFAULT_MARKER_SIZE = 32;

	/**
	 * Transparent padding (in pixels) on each side of the marker circle. The circle is drawn inset
	 * by this amount so the image remains {@link #DEFAULT_MARKER_SIZE} (e.g. for SquareMap's fixed
	 * 32px display) while the visible marker appears smaller. Effective circle size is
	 * {@code DEFAULT_MARKER_SIZE - (2 * DEFAULT_MARKER_PADDING)} (e.g. 20 when padding is 6).
	 */
	public static final int DEFAULT_MARKER_PADDING = 6;

	private static final String ICON_REDFLAG = "redflag";
	private static final String ICON_GREENFLAG = "greenflag";

	/**
	 * Returns the set of icon keys that should be registered for warp markers, from config.
	 * Unique, non-empty, normalized (trim, lowercase, spaces to underscores) values of
	 * {@link MConf#mapWarpHomeIcon} and {@link MConf#mapWarpOtherIcon}.
	 *
	 * @param conf Factions config (e.g. {@link MConf#get()})
	 * @return Set of icon keys to register (may be empty)
	 */
	public static Set<String> getConfiguredWarpIconKeys(MConf conf)
	{
		Set<String> keys = new LinkedHashSet<>();
		if (conf.mapWarpHomeIcon != null && !conf.mapWarpHomeIcon.trim().isEmpty())
			keys.add(conf.mapWarpHomeIcon.trim().toLowerCase().replace(" ", "_"));
		if (conf.mapWarpOtherIcon != null && !conf.mapWarpOtherIcon.trim().isEmpty())
			keys.add(conf.mapWarpOtherIcon.trim().toLowerCase().replace(" ", "_"));
		return keys;
	}

	/**
	 * Returns an RGB color (0xRRGGBB) for the given icon key. Known keys use fixed colors;
	 * others use a hash-based hue so the same key always gets the same color.
	 *
	 * @param key Normalized icon key (lowercase, spaces to underscores)
	 * @return RGB as 0xRRGGBB
	 */
	public static int colorForIconKey(String key)
	{
		if (key == null) return 0x808080;
		if (ICON_REDFLAG.equals(key)) return 0xFF0000;
		if (ICON_GREENFLAG.equals(key)) return 0x00AA00;
		int hash = key.hashCode();
		float hue = ((hash & 0x7FFFFFFF) % 360) / 360f;
		float s = 0.85f;
		float v = 0.9f;
		return Color.HSBtoRGB(hue, s, v) & 0xFFFFFF;
	}

	/**
	 * Creates a marker image (filled circle with white border) for the given RGB color. The image
	 * is {@code size}×{@code size} pixels; the circle is drawn inset by {@link #DEFAULT_MARKER_PADDING}
	 * on each side so the visible marker is smaller and the rest is transparent (avoids upscaling
	 * when the map plugin displays at a fixed size).
	 *
	 * @param rgb  Color as 0xRRGGBB
	 * @param size Width and height in pixels (e.g. {@link #DEFAULT_MARKER_SIZE})
	 * @return BufferedImage (TYPE_INT_ARGB), never null
	 */
	public static BufferedImage createMarkerImage(int rgb, int size)
	{
		int pad = DEFAULT_MARKER_PADDING;
		int circleSize = size - (2 * pad);
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try
		{
			g.setColor(new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255));
			g.fillOval(pad, pad, circleSize, circleSize);
			g.setColor(Color.WHITE);
			g.drawOval(pad, pad, circleSize, circleSize);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/**
	 * Creates a marker image for the given icon key using {@link #colorForIconKey} and
	 * {@link #createMarkerImage(int, int)} with default size.
	 *
	 * @param key Normalized icon key (lowercase, spaces to underscores)
	 * @return BufferedImage for the key, or null if key is null or blank
	 */
	public static BufferedImage createMarkerImageForKey(String key)
	{
		if (key == null || key.trim().isEmpty()) return null;
		String normalized = key.trim().toLowerCase().replace(" ", "_");
		int color = colorForIconKey(normalized);
		return createMarkerImage(color, DEFAULT_MARKER_SIZE);
	}
}
