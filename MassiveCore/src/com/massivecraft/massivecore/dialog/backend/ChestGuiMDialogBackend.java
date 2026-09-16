package com.massivecraft.massivecore.dialog.backend;

import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.chestgui.ChestAction;
import com.massivecraft.massivecore.chestgui.ChestGui;
import com.massivecraft.massivecore.dialog.MDialog;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.MDialogSession;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDialog;
import com.massivecraft.massivecore.dialog.MDialogSpec;
import com.massivecraft.massivecore.dialog.body.MDialogBody;
import com.massivecraft.massivecore.dialog.body.MDialogBodyItem;
import com.massivecraft.massivecore.dialog.body.MDialogBodyPlain;
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
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * ChestGui fallback for servers without the Dialog API.
 * <p>
 * Lays out bodies, clickable inputs, and type actions as inventory slots and completes the session
 * on button click or inventory close (when no completing action was chosen).
 * </p>
 */
public final class ChestGuiMDialogBackend implements MDialogBackend
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void open(Player player, MDialogSpec spec, MDialogSession session)
	{
		List<SlotAction> slots = new ArrayList<>();
		
		// Bodies first, then inputs, then type-specific buttons / child dialogs.
		for (MDialogBody body : spec.getBodies())
		{
			if (body instanceof MDialogBodyItem)
			{
				ItemStack item = ((MDialogBodyItem) body).getItem();
				if (item != null) slots.add(SlotAction.display(item));
			}
			else if (body instanceof MDialogBodyPlain)
			{
				slots.add(SlotAction.display(named(Material.PAPER, ((MDialogBodyPlain) body).getMessage())));
			}
		}
		
		for (MDialogInput input : spec.getInputs())
		{
			slots.add(SlotAction.input(input));
		}
		
		collectTypeButtons(spec.getType(), slots);
		
		// Round up to a valid chest row count, capped at six rows.
		int size = Math.max(9, ((slots.size() + 8) / 9) * 9);
		if (size > 54) size = 54;
		
		ChestGui gui = ChestGui.create(size, spec.getTitle() == null ? "" : spec.getTitle());
		gui.setBottomInventoryAllow(false);
		gui.setAutoclosing(false);
		gui.setAutoremoving(true);
		
		// Distinguish explicit button/child clicks from ESC close.
		final boolean[] selected = {false};
		
		for (int i = 0; i < slots.size() && i < gui.getLayout().getSize(); i++)
		{
			SlotAction slot = slots.get(i);
			final int index = i;
			gui.setItem(i, slot.render(session), new ChestAction()
			{
				/**
				 * Routes slot clicks to button completion, nested dialog, or input cycling.
				 *
				 * @param event Already-cancelled inventory click for this slot.
				 * @return {@code true} if this click completed a button or opened a child dialog.
				 */
				@Override
				public boolean onClick(InventoryClickEvent event)
				{
					if (slot.button != null)
					{
						selected[0] = true;
						gui.setAutoclosing(true);
						EngineMassiveCoreDialog.get().completeClick(player, slot.button.getId());
						return true;
					}
					if (slot.childSpec != null)
					{
						selected[0] = true;
						// Re-open on next tick so this inventory can close cleanly.
						Bukkit.getScheduler().runTask(MassiveCore.get(), () -> MDialog.open(player, slot.childSpec));
						return true;
					}
					if (slot.input != null)
					{
						cycleInput(player, session, slot.input, event.isShiftClick());
						if (gui.getInventory() != null) gui.getInventory().setItem(index, slot.render(session));
						return false;
					}
					return false;
				}
			});
		}
		
		gui.getRunnablesClose().add(() -> {
			if (!selected[0])
			{
				EngineMassiveCoreDialog.get().completeClose(player);
			}
		});
		
		gui.open(player);
	}
	
	/**
	 * Appends slot actions for buttons, child specs, or placeholders implied by the dialog type.
	 *
	 * @param type  MassiveCore dialog type.
	 * @param slots Mutable slot list to append to.
	 */
	private static void collectTypeButtons(MDialogType type, List<SlotAction> slots)
	{
		if (type instanceof MDialogTypeNotice)
		{
			slots.add(SlotAction.button(((MDialogTypeNotice) type).getAction()));
		}
		else if (type instanceof MDialogTypeConfirmation)
		{
			MDialogTypeConfirmation conf = (MDialogTypeConfirmation) type;
			slots.add(SlotAction.button(conf.getYes()));
			slots.add(SlotAction.button(conf.getNo()));
		}
		else if (type instanceof MDialogTypeMultiAction)
		{
			MDialogTypeMultiAction multi = (MDialogTypeMultiAction) type;
			for (MDialogButton button : multi.getActions()) slots.add(SlotAction.button(button));
			if (multi.getExitAction() != null) slots.add(SlotAction.button(multi.getExitAction()));
		}
		else if (type instanceof MDialogTypeDialogList)
		{
			MDialogTypeDialogList list = (MDialogTypeDialogList) type;
			for (MDialogSpec child : list.getDialogs())
			{
				String label = child.getExternalTitle() != null ? child.getExternalTitle() : child.getTitle();
				slots.add(SlotAction.child(child, named(Material.BOOK, label)));
			}
			if (list.getExitAction() != null) slots.add(SlotAction.button(list.getExitAction()));
		}
		else if (type instanceof MDialogTypeServerLinks)
		{
			MDialogTypeServerLinks links = (MDialogTypeServerLinks) type;
			slots.add(SlotAction.display(named(Material.COMPASS, "<i>Server links require Dialog API (1.21.6+).")));
			if (links.getExitAction() != null) slots.add(SlotAction.button(links.getExitAction()));
		}
	}
	
	/**
	 * Updates session working state when the player clicks an input slot (shift reverses cycle direction).
	 *
	 * @param player  Viewer (used for text-input message).
	 * @param session Session holding working maps.
	 * @param input   Input definition for the clicked slot.
	 * @param shift   {@code true} to cycle backward / decrement.
	 */
	private static void cycleInput(Player player, MDialogSession session, MDialogInput input, boolean shift)
	{
		if (input instanceof MDialogInputBool)
		{
			boolean current = Boolean.TRUE.equals(session.getWorkingBooleans().get(input.getKey()));
			session.getWorkingBooleans().put(input.getKey(), !current);
		}
		else if (input instanceof MDialogInputNumber)
		{
			MDialogInputNumber number = (MDialogInputNumber) input;
			float step = number.getStep() == null ? 1f : number.getStep();
			if (shift) step = -step;
			float current = session.getWorkingNumbers().getOrDefault(input.getKey(), number.getStart());
			float next = Math.max(number.getStart(), Math.min(number.getEnd(), current + step));
			session.getWorkingNumbers().put(input.getKey(), next);
		}
		else if (input instanceof MDialogInputSingleOption)
		{
			MDialogInputSingleOption single = (MDialogInputSingleOption) input;
			List<MDialogInputOption> options = single.getOptions();
			if (options.isEmpty()) return;
			String current = session.getWorkingTexts().get(input.getKey());
			int index = 0;
			for (int i = 0; i < options.size(); i++)
			{
				if (options.get(i).getId().equals(current))
				{
					index = i;
					break;
				}
			}
			index = (index + (shift ? options.size() - 1 : 1)) % options.size();
			session.getWorkingTexts().put(input.getKey(), options.get(index).getId());
		}
		else if (input instanceof MDialogInputText)
		{
			MixinMessage.get().msgOne(player, "<i>Text input requires Dialog API (1.21.6+). Current: <h>%s", session.getWorkingTexts().getOrDefault(input.getKey(), ""));
		}
	}
	
	/**
	 * Creates a single-item stack with a parsed display name.
	 *
	 * @param material Item type; null uses paper.
	 * @param name     Raw name string for {@link Txt#parse}.
	 * @return New item stack with meta display name set.
	 */
	private static ItemStack named(Material material, String name)
	{
		ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
		ItemMeta meta = stack.getItemMeta();
		if (meta != null)
		{
			meta.setDisplayName(Txt.parse(name == null ? "" : name));
			stack.setItemMeta(meta);
		}
		return stack;
	}
	
	/**
	 * Builds the inventory icon for a dialog button (icon, label, optional lore tooltip).
	 *
	 * @param button MassiveCore button.
	 * @return Item stack representing the button.
	 */
	private static ItemStack buttonItem(MDialogButton button)
	{
		ItemStack icon = button.getIcon();
		if (icon == null) icon = new ItemStack(Material.NAME_TAG);
		ItemMeta meta = icon.getItemMeta();
		if (meta != null)
		{
			meta.setDisplayName(Txt.parse(button.getLabel() == null ? button.getId() : button.getLabel()));
			if (button.getTooltip() != null)
			{
				List<String> lore = new ArrayList<>();
				lore.add(Txt.parse(button.getTooltip()));
				meta.setLore(lore);
			}
			icon.setItemMeta(meta);
		}
		return icon;
	}
	
	/**
	 * One chest slot: display-only, clickable button, cyclable input, or child dialog opener.
	 */
	private static final class SlotAction
	{
		private final MDialogButton button;
		private final MDialogInput input;
		private final MDialogSpec childSpec;
		private final ItemStack display;
		
		/**
		 * @param button    Non-null when this slot completes a button click.
		 * @param input     Non-null when this slot cycles an input.
		 * @param childSpec Non-null when this slot opens a nested dialog.
		 * @param display   Non-null for static display stacks.
		 */
		private SlotAction(MDialogButton button, MDialogInput input, MDialogSpec childSpec, ItemStack display)
		{
			this.button = button;
			this.input = input;
			this.childSpec = childSpec;
			this.display = display;
		}
		
		/**
		 * @param button Dialog button for this slot.
		 * @return Slot action that completes {@code button} on click.
		 */
		static SlotAction button(MDialogButton button)
		{
			return new SlotAction(button, null, null, null);
		}
		
		/**
		 * @param input Input widget for this slot.
		 * @return Slot action that cycles {@code input} on click.
		 */
		static SlotAction input(MDialogInput input)
		{
			return new SlotAction(null, input, null, null);
		}
		
		/**
		 * @param child   Nested dialog spec to open.
		 * @param display Icon shown in the chest slot.
		 * @return Slot action that opens {@code child} on click.
		 */
		static SlotAction child(MDialogSpec child, ItemStack display)
		{
			return new SlotAction(null, null, child, display);
		}
		
		/**
		 * @param display Item to show without click side effects beyond cancel.
		 * @return Display-only slot action.
		 */
		static SlotAction display(ItemStack display)
		{
			return new SlotAction(null, null, null, display);
		}
		
		/**
		 * Renders the current visual for this slot from session state.
		 *
		 * @param session Open dialog session for input values.
		 * @return Item stack to place in the chest GUI.
		 */
		ItemStack render(MDialogSession session)
		{
			if (this.button != null) return buttonItem(this.button);
			if (this.display != null) return this.display.clone();
			if (this.input instanceof MDialogInputBool)
			{
				boolean value = Boolean.TRUE.equals(session.getWorkingBooleans().get(this.input.getKey()));
				return named(value ? Material.LIME_DYE : Material.GRAY_DYE, ((MDialogInputBool) this.input).getLabel() + ": " + value);
			}
			if (this.input instanceof MDialogInputNumber)
			{
				Float value = session.getWorkingNumbers().get(this.input.getKey());
				return named(Material.REPEATER, ((MDialogInputNumber) this.input).getLabel() + ": " + value);
			}
			if (this.input instanceof MDialogInputSingleOption)
			{
				String id = session.getWorkingTexts().get(this.input.getKey());
				String display = id;
				for (MDialogInputOption option : ((MDialogInputSingleOption) this.input).getOptions())
				{
					if (option.getId().equals(id))
					{
						display = option.getDisplay();
						break;
					}
				}
				return named(Material.HOPPER, ((MDialogInputSingleOption) this.input).getLabel() + ": " + display);
			}
			if (this.input instanceof MDialogInputText)
			{
				String value = session.getWorkingTexts().getOrDefault(this.input.getKey(), "");
				return named(Material.WRITABLE_BOOK, ((MDialogInputText) this.input).getLabel() + ": " + value);
			}
			return named(Material.BARRIER, "?");
		}
	}
}

