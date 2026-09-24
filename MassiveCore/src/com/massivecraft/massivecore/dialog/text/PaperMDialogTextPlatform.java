package com.massivecraft.massivecore.dialog.text;

import com.massivecraft.massivecore.dialog.MDialogText;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Paper / Adventure conversion for {@link MDialogText}.
 * <p>
 * Used only by {@code PaperMDialogBackend} (after the Paper Dialog API probe),
 * so Spigot never loads this class.
 * </p>
 */
public final class PaperMDialogTextPlatform
{
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
	private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
	
	private PaperMDialogTextPlatform()
	{
	}
	
	/**
	 * Typed Adventure conversion for Paper dialog backends.
	 *
	 * @param text Dialog text; null becomes empty.
	 * @return Adventure component.
	 */
	public static Component toComponent(MDialogText text)
	{
		if (text == null) return Component.empty();
		Object adventure = text.rawAdventure();
		if (adventure instanceof Component) return (Component) adventure;
		Mson mson = text.rawMson();
		if (mson != null) return toComponent(mson);
		String txt = text.rawTxt();
		return LEGACY.deserialize(Txt.parse(txt == null ? "" : txt));
	}
	
	/**
	 * Parses Txt markup to Adventure (Paper backends).
	 *
	 * @param raw Markup string.
	 * @return Adventure component.
	 */
	public static Component toComponent(String raw)
	{
		return LEGACY.deserialize(Txt.parse(raw == null ? "" : raw));
	}
	
	/**
	 * Converts {@link Mson} to Adventure, preserving show-text hover for dialog option displays.
	 *
	 * @param mson Non-null Mson tree.
	 * @return Adventure component.
	 */
	private static Component toComponent(Mson mson)
	{
		Component component = GSON.deserialize(mson.toRaw());
		// Mson JSON still uses legacy hoverEvent keys; re-apply so Paper Dialog option displays
		// keep show_text hover the way vanilla dialog JSON expects.
		String tooltip = mson.getTooltip();
		if (tooltip != null && !tooltip.isEmpty())
		{
			component = component.hoverEvent(HoverEvent.showText(LEGACY.deserialize(tooltip)));
		}
		return component;
	}
}
