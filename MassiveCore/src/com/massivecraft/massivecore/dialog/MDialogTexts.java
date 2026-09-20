package com.massivecraft.massivecore.dialog;

import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Shared Spigot-safe text helpers for dialog backends.
 * <p>
 * Prefer {@link MDialogText} for new code (Txt or {@link Mson}).
 * Adventure conversion lives only on {@code PaperMDialogTextPlatform}
 * (used by {@code PaperMDialogBackend}).
 * </p>
 */
public final class MDialogTexts
{
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
	 * Converts parsed Txt markup to a Bungee {@link BaseComponent} tree.
	 *
	 * @param raw Plugin-provided text.
	 * @return Single root component suitable for Spigot dialog APIs.
	 */
	public static BaseComponent bungee(String raw)
	{
		BaseComponent[] parts = TextComponent.fromLegacyText(parse(raw));
		if (parts == null || parts.length == 0) return new TextComponent("");
		if (parts.length == 1) return parts[0];
		TextComponent root = new TextComponent("");
		for (BaseComponent part : parts)
		{
			root.addExtra(part);
		}
		return root;
	}
	
	/**
	 * Converts {@link MDialogText} to Bungee for Spigot Dialog APIs.
	 *
	 * @param text Dialog text; null becomes empty.
	 * @return Single root Bungee component.
	 */
	public static BaseComponent bungee(MDialogText text)
	{
		if (text == null) return new TextComponent("");
		return text.toBungee();
	}
	
	/**
	 * Plain / legacy string for ChestGui and similar.
	 *
	 * @param text Dialog text; null becomes empty.
	 * @return Styled plain string.
	 */
	public static String plain(MDialogText text)
	{
		if (text == null) return "";
		return text.toPlain();
	}
}
