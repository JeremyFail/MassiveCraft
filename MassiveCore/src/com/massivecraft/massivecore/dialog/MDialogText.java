package com.massivecraft.massivecore.dialog;

import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

/**
 * Backend-agnostic dialog text: MassiveCore {@link Txt} strings, {@link Mson}, or (Paper) Adventure.
 * <p>
 * Shared conversion uses Bungee / legacy strings only so Spigot never links Adventure.
 * Paper backends convert via {@code PaperMDialogTextPlatform}.
 * </p>
 */
public final class MDialogText
{
	/** Txt-markup source; mutually exclusive with {@link #mson} / {@link #adventure}. */
	private final String txt;
	
	/** Mson source (colors, hover, etc.). */
	private final Mson mson;
	
	/** Opaque Adventure {@code Component} (Paper only); never typed here. */
	private final Object adventure;
	
	private MDialogText(String txt, Mson mson, Object adventure)
	{
		this.txt = txt;
		this.mson = mson;
		this.adventure = adventure;
	}
	
	/**
	 * Text with MassiveCore {@link Txt} color tags (e.g. {@code <h>Secret}).
	 *
	 * @param raw Markup string; null becomes empty.
	 * @return Dialog text.
	 */
	public static MDialogText txt(String raw)
	{
		return new MDialogText(raw == null ? "" : raw, null, null);
	}
	
	/**
	 * Txt label with a parsed hover tooltip (implemented as {@link Mson}).
	 * <p>
	 * Useful for chat and dialog <em>body</em> text. Minecraft ignores {@code hover_event}
	 * on dialog input controls (MC-298405); action buttons need
	 * {@link MDialogButton#tooltip(String)} instead.
	 * </p>
	 *
	 * @param raw Label markup.
	 * @param tooltipParse Tooltip markup (passed to {@link Mson#tooltipParse(String)}).
	 * @return Dialog text.
	 */
	public static MDialogText txt(String raw, String tooltipParse)
	{
		Mson label = Mson.mson(Txt.parse(raw == null ? "" : raw));
		if (tooltipParse != null && !tooltipParse.isEmpty())
		{
			label = label.tooltipParse(tooltipParse);
		}
		return mson(label);
	}
	
	/**
	 * Rich MassiveCore {@link Mson} (preferred for hover / click styling shared with chat).
	 *
	 * @param mson Mson tree; null becomes empty txt.
	 * @return Dialog text.
	 */
	public static MDialogText mson(Mson mson)
	{
		if (mson == null) return txt("");
		return new MDialogText(null, mson, null);
	}
	
	/**
	 * Paper Adventure component stored opaquely so this class never links Adventure.
	 *
	 * @param adventureComponent Adventure component, or null.
	 * @return Dialog text.
	 */
	public static MDialogText adventure(Object adventureComponent)
	{
		if (adventureComponent == null) return txt("");
		return new MDialogText(null, null, adventureComponent);
	}
	
	/**
	 * @return Single Bungee root component for Spigot Dialog APIs.
	 */
	public BaseComponent toBungee()
	{
		if (this.mson != null) return merge(ComponentSerializer.parse(this.mson.toRaw()));
		if (this.txt != null) return merge(TextComponent.fromLegacyText(Txt.parse(this.txt)));
		// Opaque Adventure payloads are Paper-only; Spigot falls back to empty.
		return new TextComponent("");
	}
	
	/**
	 * Plain / legacy string for ChestGui titles and similar.
	 *
	 * @return Styled legacy text when possible.
	 */
	public String toPlain()
	{
		if (this.mson != null) return this.mson.toPlain(true);
		if (this.txt != null) return Txt.parse(this.txt);
		return "";
	}
	
	/**
	 * Raw Txt source for Paper conversion; prefer {@link #toPlain()} / {@link #toBungee()}.
	 *
	 * @return Txt markup or null when sourced from Mson/Adventure.
	 */
	public String rawTxt() { return this.txt; }
	
	/**
	 * Raw Mson source for Paper conversion.
	 *
	 * @return Mson tree or null.
	 */
	public Mson rawMson() { return this.mson; }
	
	/**
	 * Opaque Adventure payload for Paper conversion.
	 *
	 * @return Adventure component object or null.
	 */
	public Object rawAdventure() { return this.adventure; }
	
	private static BaseComponent merge(BaseComponent[] parts)
	{
		if (parts == null || parts.length == 0) return new TextComponent("");
		if (parts.length == 1) return parts[0];
		TextComponent root = new TextComponent("");
		for (BaseComponent part : parts)
		{
			root.addExtra(part);
		}
		return root;
	}
}
