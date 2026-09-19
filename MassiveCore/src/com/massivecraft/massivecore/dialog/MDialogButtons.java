package com.massivecraft.massivecore.dialog;

import com.massivecraft.massivecore.dialog.body.MDialogBody;
import com.massivecraft.massivecore.dialog.body.MDialogBodyItem;
import com.massivecraft.massivecore.dialog.input.MDialogInput;
import com.massivecraft.massivecore.dialog.input.MDialogInputBool;
import com.massivecraft.massivecore.dialog.input.MDialogInputNumber;
import com.massivecraft.massivecore.dialog.input.MDialogInputOption;
import com.massivecraft.massivecore.dialog.input.MDialogInputSingleOption;
import com.massivecraft.massivecore.dialog.input.MDialogInputText;
import com.massivecraft.massivecore.dialog.type.MDialogType;
import com.massivecraft.massivecore.dialog.type.MDialogTypeConfirmation;
import com.massivecraft.massivecore.dialog.type.MDialogTypeDialogList;
import com.massivecraft.massivecore.dialog.type.MDialogTypeMultiAction;
import com.massivecraft.massivecore.dialog.type.MDialogTypeNotice;
import com.massivecraft.massivecore.dialog.type.MDialogTypeServerLinks;

import java.util.Map;

/**
 * Helpers to walk a {@link MDialogSpec} for buttons and default input values.
 * <p>
 * Used when opening a session so {@link MDialogSession} can route clicks and seed
 * {@link MDialogResponse} fields before the player edits anything.
 * </p>
 */
public final class MDialogButtons
{
	/**
	 * Prevents instantiation.
	 */
	private MDialogButtons()
	{
	}
	
	/**
	 * Collects every {@link MDialogButton} reachable from the spec into {@code out}.
	 *
	 * @param spec Dialog definition; ignored if null.
	 * @param out Map to populate; ignored if null.
	 */
	public static void collect(MDialogSpec spec, Map<String, MDialogButton> out)
	{
		if (spec == null || out == null) return;
		collectType(spec.getType(), out);
		collectBodies(spec, out);
	}
	
	/**
	 * Recursively registers buttons for a dialog type (and nested dialog lists).
	 *
	 * @param type Footer type to inspect.
	 * @param out Accumulator map.
	 */
	private static void collectType(MDialogType type, Map<String, MDialogButton> out)
	{
		if (type instanceof MDialogTypeNotice)
		{
			put(out, ((MDialogTypeNotice) type).getAction());
		}
		else if (type instanceof MDialogTypeConfirmation)
		{
			MDialogTypeConfirmation conf = (MDialogTypeConfirmation) type;
			put(out, conf.getYes());
			put(out, conf.getNo());
		}
		else if (type instanceof MDialogTypeMultiAction)
		{
			MDialogTypeMultiAction multi = (MDialogTypeMultiAction) type;
			for (MDialogButton button : multi.getActions()) put(out, button);
			put(out, multi.getExitAction());
		}
		else if (type instanceof MDialogTypeDialogList)
		{
			MDialogTypeDialogList list = (MDialogTypeDialogList) type;
			put(out, list.getExitAction());
			// Nested specs contribute their own buttons to the same session map.
			for (MDialogSpec child : list.getDialogs()) collect(child, out);
		}
		else if (type instanceof MDialogTypeServerLinks)
		{
			put(out, ((MDialogTypeServerLinks) type).getExitAction());
		}
	}
	
	/**
	 * Registers clickable item-body descriptions as virtual buttons when they have an id and handler
	 * and that id is not already used by a footer button.
	 *
	 * @param spec Dialog definition.
	 * @param out Accumulator map.
	 */
	private static void collectBodies(MDialogSpec spec, Map<String, MDialogButton> out)
	{
		for (MDialogBody body : spec.getBodies())
		{
			if (!(body instanceof MDialogBodyItem)) continue;
			MDialogBodyItem item = (MDialogBodyItem) body;
			if (item.getClickId() == null || item.getClickHandler() == null) continue;
			if (out.containsKey(item.getClickId())) continue;
			String label = item.getDescription() == null ? item.getClickId() : item.getDescription();
			put(out, MDialogButton.of(item.getClickId(), label)
				.icon(item.getItem())
				.onClick(item.getClickHandler()));
		}
	}
	
	/**
	 * Inserts a button when it has a non-null id.
	 *
	 * @param out Target map.
	 * @param button Button to register.
	 */
	private static void put(Map<String, MDialogButton> out, MDialogButton button)
	{
		if (button == null || button.getId() == null) return;
		out.put(button.getId(), button);
	}
	
	/**
	 * Seeds working value maps from input definitions on the spec.
	 *
	 * @param spec Dialog spec.
	 * @param texts Text / single-option map to fill; may be null-safe skipped when spec null.
	 * @param booleans Boolean map to fill.
	 * @param numbers Number map to fill.
	 */
	public static void seedWorkingValues(MDialogSpec spec, Map<String, String> texts, Map<String, Boolean> booleans, Map<String, Float> numbers)
	{
		if (spec == null) return;
		for (MDialogInput input : spec.getInputs())
		{
			if (input instanceof MDialogInputText)
			{
				MDialogInputText text = (MDialogInputText) input;
				texts.put(text.getKey(), text.getInitial() == null ? "" : text.getInitial());
			}
			else if (input instanceof MDialogInputBool)
			{
				MDialogInputBool bool = (MDialogInputBool) input;
				booleans.put(bool.getKey(), bool.getInitial());
			}
			else if (input instanceof MDialogInputNumber)
			{
				MDialogInputNumber number = (MDialogInputNumber) input;
				// Prefer explicit initial; otherwise use range start.
				float value = number.getInitial() != null ? number.getInitial() : number.getStart();
				numbers.put(number.getKey(), value);
			}
			else if (input instanceof MDialogInputSingleOption)
			{
				MDialogInputSingleOption single = (MDialogInputSingleOption) input;
				String initialId = null;
				for (MDialogInputOption option : single.getOptions())
				{
					if (option.isInitial())
					{
						initialId = option.getId();
						break;
					}
				}
				// Fall back to first option when none marked initial.
				if (initialId == null && !single.getOptions().isEmpty())
				{
					initialId = single.getOptions().get(0).getId();
				}
				texts.put(single.getKey(), initialId == null ? "" : initialId);
			}
		}
	}
}
