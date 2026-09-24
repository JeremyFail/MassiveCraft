package com.massivecraft.massivecore.dialog.backend;

import com.massivecraft.massivecore.dialog.MDialogAfterAction;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.MDialogSession;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDialog;
import com.massivecraft.massivecore.dialog.MDialogSpec;
import com.massivecraft.massivecore.dialog.text.PaperMDialogTextPlatform;
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
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Paper Dialog API backend.
 * <p>
 * Constructed by {@link com.massivecraft.massivecore.dialog.MDialog} only after a Paper Dialog API
 * classpath probe succeeds, so Spigot never links this class.
 * </p>
 */
public final class PaperMDialogBackend implements MDialogBackend
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void open(Player player, MDialogSpec spec, MDialogSession session)
	{
		// Paper opens a registry-backed dialog on the client.
		player.showDialog(buildDialog(player, spec, session));
	}
	
	/**
	 * Assembles a full Paper {@link Dialog} from spec and session (needed for nested dialog lists).
	 *
	 * @param player  Viewer used for button click routing.
	 * @param spec    Source specification.
	 * @param session Session whose inputs are updated on button click.
	 * @return Built Paper dialog ready for {@link Player#showDialog(Dialog)}.
	 */
	private Dialog buildDialog(Player player, MDialogSpec spec, MDialogSession session)
	{
		DialogBase.Builder base = DialogBase.builder(PaperMDialogTextPlatform.toComponent(spec.getTitle()))
			.canCloseWithEscape(spec.canCloseWithEscape())
			.pause(spec.isPause())
			.afterAction(toPaperAfterAction(spec.getAfterAction()))
			.body(toPaperBodies(player, spec.getBodies()))
			.inputs(toPaperInputs(spec.getInputs()));
		
		if (spec.getExternalTitle() != null)
		{
			base.externalTitle(PaperMDialogTextPlatform.toComponent(spec.getExternalTitle()));
		}
		
		return Dialog.create(factory -> factory.empty()
			.base(base.build())
			.type(toPaperType(player, spec.getType(), session))
		);
	}
	
	/**
	 * Maps MassiveCore dialog type to Paper {@link DialogType} (notice, confirmation, list, etc.).
	 *
	 * @param player  Passed through for action buttons and nested dialogs.
	 * @param type    MassiveCore type instance.
	 * @param session Session for nested {@link MDialogTypeDialogList} children.
	 * @return Paper dialog type configuration.
	 */
	private io.papermc.paper.registry.data.dialog.type.DialogType toPaperType(Player player, MDialogType type, MDialogSession session)
	{
		if (type instanceof MDialogTypeNotice)
		{
			return DialogType.notice(toActionButton(player, ((MDialogTypeNotice) type).getAction(), session));
		}
		if (type instanceof MDialogTypeConfirmation)
		{
			MDialogTypeConfirmation conf = (MDialogTypeConfirmation) type;
			return DialogType.confirmation(
				toActionButton(player, conf.getYes(), session),
				toActionButton(player, conf.getNo(), session)
			);
		}
		if (type instanceof MDialogTypeMultiAction)
		{
			MDialogTypeMultiAction multi = (MDialogTypeMultiAction) type;
			List<ActionButton> actions = new ArrayList<>();
			for (MDialogButton button : multi.getActions())
			{
				actions.add(toActionButton(player, button, session));
			}
			var builder = DialogType.multiAction(actions).columns(multi.getColumns());
			if (multi.getExitAction() != null)
			{
				builder.exitAction(toActionButton(player, multi.getExitAction(), session));
			}
			return builder.build();
		}
		if (type instanceof MDialogTypeDialogList)
		{
			MDialogTypeDialogList list = (MDialogTypeDialogList) type;
			List<Dialog> children = new ArrayList<>();
			for (MDialogSpec child : list.getDialogs())
			{
				// Each list entry is a full nested dialog in the dialog registry set.
				children.add(buildDialog(player, child, session));
			}
			var builder = DialogType.dialogList(RegistrySet.valueSet(RegistryKey.DIALOG, children))
				.columns(list.getColumns());
			if (list.getButtonWidth() != null) builder.buttonWidth(list.getButtonWidth());
			if (list.getExitAction() != null) builder.exitAction(toActionButton(player, list.getExitAction(), session));
			return builder.build();
		}
		if (type instanceof MDialogTypeServerLinks)
		{
			MDialogTypeServerLinks links = (MDialogTypeServerLinks) type;
			ActionButton exit = links.getExitAction() == null
				? ActionButton.builder(Component.text("Close")).build()
				: toActionButton(player, links.getExitAction(), session);
			int buttonWidth = links.getButtonWidth() == null ? 150 : links.getButtonWidth();
			return DialogType.serverLinks(exit, links.getColumns(), buttonWidth);
		}
		
		// Unknown or unset type: empty notice.
		return DialogType.notice();
	}
	
	/**
	 * Builds a Paper action button with a one-shot custom click that completes the MassiveCore session.
	 *
	 * @param player  Default clicker if the audience is not a player.
	 * @param button  MassiveCore button; null yields a default OK button.
	 * @param session Session to receive input values from the response view.
	 * @return Paper {@link ActionButton}.
	 */
	private ActionButton toActionButton(Player player, MDialogButton button, MDialogSession session)
	{
		if (button == null)
		{
			return ActionButton.builder(Component.text("OK")).build();
		}
		
		ActionButton.Builder builder = ActionButton.builder(PaperMDialogTextPlatform.toComponent(button.getLabel()));
		if (button.getTooltip() != null) builder.tooltip(PaperMDialogTextPlatform.toComponent(button.getTooltip()));
		if (button.getWidth() != null) builder.width(button.getWidth());
		
		final String buttonId = button.getId();
		builder.action(DialogAction.customClick((view, audience) -> {
			Player clicker = audience instanceof Player ? (Player) audience : player;
			// Snapshot widget values before the engine clears the session.
			applyResponseView(session, view);
			EngineMassiveCoreDialog.get().completeClick(clicker, buttonId);
		}, ClickCallback.Options.builder().uses(1).build()));
		
		return builder.build();
	}
	
	/**
	 * Copies Paper dialog input values into the session working maps by input key.
	 *
	 * @param session Open dialog session.
	 * @param view    Response view from the clicked button; may be null.
	 */
	private static void applyResponseView(MDialogSession session, DialogResponseView view)
	{
		if (session == null || view == null) return;
		for (MDialogInput input : session.getSpec().getInputs())
		{
			String key = input.getKey();
			if (input instanceof MDialogInputText || input instanceof MDialogInputSingleOption)
			{
				String text = view.getText(key);
				if (text != null) session.getWorkingTexts().put(key, text);
			}
			else if (input instanceof MDialogInputBool)
			{
				Boolean value = view.getBoolean(key);
				if (value != null) session.getWorkingBooleans().put(key, value);
			}
			else if (input instanceof MDialogInputNumber)
			{
				Float value = view.getFloat(key);
				if (value != null) session.getWorkingNumbers().put(key, value);
			}
		}
	}
	
	/**
	 * Converts MassiveCore body entries to Paper {@link DialogBody} list (plain text and items).
	 *
	 * @param player Viewer used for description click routing.
	 * @param bodies MassiveCore bodies; may be empty.
	 * @return Paper body list in display order.
	 */
	private static List<DialogBody> toPaperBodies(Player player, List<MDialogBody> bodies)
	{
		List<DialogBody> out = new ArrayList<>();
		for (MDialogBody body : bodies)
		{
			if (body instanceof MDialogBodyPlain)
			{
				MDialogBodyPlain plain = (MDialogBodyPlain) body;
				if (plain.getWidth() != null)
				{
					out.add(DialogBody.plainMessage(PaperMDialogTextPlatform.toComponent(plain.getMessageText()), plain.getWidth()));
				}
				else
				{
					out.add(DialogBody.plainMessage(PaperMDialogTextPlatform.toComponent(plain.getMessageText())));
				}
			}
			else if (body instanceof MDialogBodyItem)
			{
				MDialogBodyItem itemBody = (MDialogBodyItem) body;
				ItemStack item = itemBody.getItem();
				// Null item = text-only body (clickable when clickId is set).
				if (item == null)
				{
					if (itemBody.getDescription() == null) continue;
					Component descComponent = PaperMDialogTextPlatform.toComponent(itemBody.getDescription());
					if (itemBody.getClickId() != null)
					{
						descComponent = clickable(descComponent, player, itemBody.getClickId());
					}
					if (itemBody.getWidth() != null)
					{
						out.add(DialogBody.plainMessage(descComponent, itemBody.getWidth()));
					}
					else
					{
						out.add(DialogBody.plainMessage(descComponent));
					}
					continue;
				}
				var builder = DialogBody.item(item)
					.showDecorations(itemBody.isShowDecorations())
					.showTooltip(itemBody.isShowTooltip());
				if (itemBody.getDescription() != null)
				{
					Component descComponent = PaperMDialogTextPlatform.toComponent(itemBody.getDescription());
					if (itemBody.getClickId() != null)
					{
						descComponent = clickable(descComponent, player, itemBody.getClickId());
					}
					PlainMessageDialogBody desc = itemBody.getWidth() != null
						? DialogBody.plainMessage(descComponent, itemBody.getWidth())
						: DialogBody.plainMessage(descComponent);
					builder.description(desc);
				}
				if (itemBody.getWidth() != null) builder.width(itemBody.getWidth());
				if (itemBody.getHeight() != null) builder.height(itemBody.getHeight());
				out.add(builder.build());
			}
		}
		return out;
	}
	
	/**
	 * Marks body text as a dialog action: click completes {@code clickId}, same as the matching button.
	 *
	 * @param component Description text.
	 * @param player    Default clicker if the audience is not a player.
	 * @param clickId   Action-button id to complete.
	 * @return Component with click, hover, and underline applied to the whole tree.
	 */
	private static Component clickable(Component component, Player player, String clickId)
	{
		ClickEvent click = ClickEvent.callback(audience -> {
			Player clicker = audience instanceof Player ? (Player) audience : player;
			EngineMassiveCoreDialog.get().completeClick(clicker, clickId);
		}, ClickCallback.Options.builder().uses(1).build());
		Component hover = Component.text("Click to select");
		return applyClick(component, click, hover);
	}
	
	/**
	 * Applies click, hover, and underline to {@code component} and all children.
	 * Legacy-deserialized trees put style on children, so the root event alone would not fire.
	 *
	 * @param component Text tree.
	 * @param click     Click event.
	 * @param hover     Hover text.
	 * @return Styled copy.
	 */
	private static Component applyClick(Component component, ClickEvent click, Component hover)
	{
		Component out = component.clickEvent(click)
			.hoverEvent(HoverEvent.showText(hover))
			.decorate(TextDecoration.UNDERLINED);
		if (component.children().isEmpty()) return out;
		List<Component> children = new ArrayList<>();
		for (Component child : component.children())
		{
			children.add(applyClick(child, click, hover));
		}
		return out.children(children);
	}
	
	/**
	 * Converts MassiveCore inputs to Paper {@link DialogInput} widgets.
	 *
	 * @param inputs MassiveCore input definitions.
	 * @return Paper inputs in spec order.
	 */
	private static List<DialogInput> toPaperInputs(List<MDialogInput> inputs)
	{
		List<DialogInput> out = new ArrayList<>();
		for (MDialogInput input : inputs)
		{
			if (input instanceof MDialogInputBool)
			{
				MDialogInputBool bool = (MDialogInputBool) input;
				var builder = DialogInput.bool(bool.getKey(), PaperMDialogTextPlatform.toComponent(bool.getLabelText()))
					.initial(bool.getInitial());
				if (bool.getOnTrue() != null) builder.onTrue(bool.getOnTrue());
				if (bool.getOnFalse() != null) builder.onFalse(bool.getOnFalse());
				out.add(builder.build());
			}
			else if (input instanceof MDialogInputText)
			{
				MDialogInputText text = (MDialogInputText) input;
				var builder = DialogInput.text(text.getKey(), PaperMDialogTextPlatform.toComponent(text.getLabel()))
					.labelVisible(text.isLabelVisible());
				if (text.getWidth() != null) builder.width(text.getWidth());
				if (text.getInitial() != null) builder.initial(text.getInitial());
				if (text.getMaxLength() != null) builder.maxLength(text.getMaxLength());
				if (text.isMultiline())
				{
					builder.multiline(TextDialogInput.MultilineOptions.create(text.getMultilineMaxLines(), text.getMultilineHeight()));
				}
				out.add(builder.build());
			}
			else if (input instanceof MDialogInputNumber)
			{
				MDialogInputNumber number = (MDialogInputNumber) input;
				var builder = DialogInput.numberRange(number.getKey(), PaperMDialogTextPlatform.toComponent(number.getLabel()), number.getStart(), number.getEnd());
				if (number.getWidth() != null) builder.width(number.getWidth());
				if (number.getLabelFormat() != null) builder.labelFormat(number.getLabelFormat());
				if (number.getInitial() != null) builder.initial(number.getInitial());
				if (number.getStep() != null) builder.step(number.getStep());
				out.add(builder.build());
			}
			else if (input instanceof MDialogInputSingleOption)
			{
				MDialogInputSingleOption single = (MDialogInputSingleOption) input;
				List<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>();
				for (MDialogInputOption option : single.getOptions())
				{
					options.add(SingleOptionDialogInput.OptionEntry.create(
						option.getId(),
						PaperMDialogTextPlatform.toComponent(option.getDisplayText()),
						option.isInitial()
					));
				}
				var builder = DialogInput.singleOption(single.getKey(), PaperMDialogTextPlatform.toComponent(single.getLabelText()), options)
					.labelVisible(single.isLabelVisible());
				if (single.getWidth() != null) builder.width(single.getWidth());
				out.add(builder.build());
			}
		}
		return out;
	}
	
	/**
	 * Maps MassiveCore after-action to Paper {@link DialogBase.DialogAfterAction}.
	 *
	 * @param afterAction MassiveCore setting; null defaults to close.
	 * @return Paper after-action enum value.
	 */
	private static DialogBase.DialogAfterAction toPaperAfterAction(MDialogAfterAction afterAction)
	{
		if (afterAction == null) return DialogBase.DialogAfterAction.CLOSE;
		switch (afterAction)
		{
			case NONE: return DialogBase.DialogAfterAction.NONE;
			case WAIT_FOR_RESPONSE: return DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE;
			case CLOSE:
			default: return DialogBase.DialogAfterAction.CLOSE;
		}
	}
}

