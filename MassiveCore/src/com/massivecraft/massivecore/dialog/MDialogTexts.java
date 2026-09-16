package com.massivecraft.massivecore.dialog;

import com.massivecraft.massivecore.util.Txt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Shared text conversion for dialog backends (MassiveCore {@link Txt} → Adventure / Bungee).
 * <p>
 * All user-facing strings should go through {@link #parse(String)} so color/format tokens match chat.
 * </p>
 */
public final class MDialogTexts
{
	/** Section-sign legacy serializer for Adventure components. */
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
	
	/**
	 * Prevents instantiation.
	 */
	private MDialogTexts()
	{
	}
	
	/**
	 * Parses MassiveCore markup and color codes in a raw string.
	 *
	 * @param raw Plugin-provided text; null becomes empty.
	 * @return Parsed plain legacy string.
	 */
	public static String parse(String raw)
	{
		if (raw == null) return "";
		return Txt.parse(raw);
	}
	
	/**
	 * Converts parsed text to an Adventure {@link Component}.
	 *
	 * @param raw Plugin-provided text.
	 * @return Deserialized component.
	 */
	public static Component component(String raw)
	{
		return LEGACY.deserialize(parse(raw));
	}
	
	/**
	 * Converts parsed text to a Bungee {@link BaseComponent} tree.
	 * <p>
	 * {@link TextComponent#fromLegacyText} may return multiple roots; we merge them when needed.
	 * </p>
	 *
	 * @param raw Plugin-provided text.
	 * @return Single root component suitable for Spigot dialog APIs.
	 */
	public static BaseComponent bungee(String raw)
	{
		BaseComponent[] parts = TextComponent.fromLegacyText(parse(raw));
		if (parts == null || parts.length == 0) return new TextComponent("");
		if (parts.length == 1) return parts[0];
		// Combine split legacy segments into one root for APIs expecting one component.
		TextComponent root = new TextComponent("");
		for (BaseComponent part : parts)
		{
			root.addExtra(part);
		}
		return root;
	}
}
