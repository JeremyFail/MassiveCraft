package com.massivecraft.massivecore.dialog.backend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.dialog.MDialogAfterAction;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.MDialogSession;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDialog;
import com.massivecraft.massivecore.dialog.MDialogSpec;
import com.massivecraft.massivecore.dialog.MDialogTexts;
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
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEventCustom;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.dialog.ConfirmationDialog;
import net.md_5.bungee.api.dialog.Dialog;
import net.md_5.bungee.api.dialog.DialogBase;
import net.md_5.bungee.api.dialog.DialogListDialog;
import net.md_5.bungee.api.dialog.MultiActionDialog;
import net.md_5.bungee.api.dialog.NoticeDialog;
import net.md_5.bungee.api.dialog.ServerLinksDialog;
import net.md_5.bungee.api.dialog.action.ActionButton;
import net.md_5.bungee.api.dialog.action.CustomClickAction;
import net.md_5.bungee.api.dialog.body.DialogBody;
import net.md_5.bungee.api.dialog.body.PlainMessageBody;
import net.md_5.bungee.api.dialog.input.BooleanInput;
import net.md_5.bungee.api.dialog.input.DialogInput;
import net.md_5.bungee.api.dialog.input.InputOption;
import net.md_5.bungee.api.dialog.input.NumberRangeInput;
import net.md_5.bungee.api.dialog.input.SingleOptionInput;
import net.md_5.bungee.api.dialog.input.TextInput;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCustomClickEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spigot Bungee Dialog backend.
 * <p>
 * Constructed by {@link com.massivecraft.massivecore.dialog.MDialog} only after a Spigot Dialog API
 * classpath probe succeeds. Builds Bungee {@link Dialog} instances and routes
 * {@link PlayerCustomClickEvent} clicks under the {@value #NAMESPACE} namespace back to
 * {@link EngineMassiveCoreDialog}.
 * </p>
 */
public final class SpigotMDialogBackend implements MDialogBackend, Listener
{
	/** Namespace for {@link CustomClickAction} ids on dialog buttons. */
	private static final String NAMESPACE = "massivecore";
	/** Key prefix after namespace; remainder is the button id (lowercase). */
	private static final String KEY_PREFIX = "mdlg/";
	
	/**
	 * Cached {@link PlayerCustomClickEvent#getData()} lookup.
	 * <p>
	 * TODO: Remove reflective getData bridge if MassiveCore drops shaded Gson (or Spigot
	 * exposes click payload without {@code com.google.gson} in the method signature).
	 * </p>
	 */
	private static final Method GET_DATA = resolveGetDataMethod();
	
	/** Ensures {@link #onCustomClick} is registered once per JVM. */
	private boolean listenerRegistered;
	
	/**
	 * Registers this listener with Bukkit if not already done.
	 */
	private void ensureListener()
	{
		if (this.listenerRegistered) return;
		Bukkit.getPluginManager().registerEvents(this, MassiveCore.get());
		this.listenerRegistered = true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void open(Player player, MDialogSpec spec, MDialogSession session)
	{
		this.ensureListener();
		showBungeeDialog(player, buildDialog(spec));
	}
	
	/**
	 * Builds a Bungee {@link Dialog} from a spec (no session needed until click).
	 *
	 * @param spec Source specification.
	 * @return Concrete dialog subtype matching {@code spec.getType()}.
	 */
	private Dialog buildDialog(MDialogSpec spec)
	{
		DialogBase base = new DialogBase(MDialogTexts.bungee(spec.getTitle()))
			.canCloseWithEscape(spec.canCloseWithEscape())
			.pause(spec.isPause())
			.afterAction(toBungeeAfterAction(spec.getAfterAction()))
			.body(toBungeeBodies(spec.getBodies()))
			.inputs(toBungeeInputs(spec.getInputs()));
		
		if (spec.getExternalTitle() != null)
		{
			base.externalTitle(MDialogTexts.bungee(spec.getExternalTitle()));
		}
		
		MDialogType type = spec.getType();
		if (type instanceof MDialogTypeNotice)
		{
			return new NoticeDialog(base, toActionButton(((MDialogTypeNotice) type).getAction()));
		}
		if (type instanceof MDialogTypeConfirmation)
		{
			MDialogTypeConfirmation conf = (MDialogTypeConfirmation) type;
			return new ConfirmationDialog(base, toActionButton(conf.getYes()), toActionButton(conf.getNo()));
		}
		if (type instanceof MDialogTypeMultiAction)
		{
			MDialogTypeMultiAction multi = (MDialogTypeMultiAction) type;
			List<ActionButton> actions = new ArrayList<>();
			for (MDialogButton button : multi.getActions())
			{
				actions.add(toActionButton(button));
			}
			ActionButton exit = multi.getExitAction() == null ? null : toActionButton(multi.getExitAction());
			return new MultiActionDialog(base, actions, multi.getColumns(), exit);
		}
		if (type instanceof MDialogTypeDialogList)
		{
			MDialogTypeDialogList list = (MDialogTypeDialogList) type;
			List<Dialog> children = new ArrayList<>();
			for (MDialogSpec child : list.getDialogs())
			{
				children.add(buildDialog(child));
			}
			ActionButton exit = list.getExitAction() == null ? null : toActionButton(list.getExitAction());
			return new DialogListDialog(base, children, exit, list.getColumns(), list.getButtonWidth());
		}
		if (type instanceof MDialogTypeServerLinks)
		{
			MDialogTypeServerLinks links = (MDialogTypeServerLinks) type;
			ActionButton exit = links.getExitAction() == null ? null : toActionButton(links.getExitAction());
			return new ServerLinksDialog(base, exit, links.getColumns(), links.getButtonWidth());
		}
		
		return new NoticeDialog(base);
	}
	
	/**
	 * Maps a MassiveCore button to Bungee {@link ActionButton} with a namespaced custom click id.
	 *
	 * @param button MassiveCore button; null yields default OK.
	 * @return Bungee action button.
	 */
	private ActionButton toActionButton(MDialogButton button)
	{
		if (button == null)
		{
			return new ActionButton(MDialogTexts.bungee("OK"), new CustomClickAction(NAMESPACE + ":" + KEY_PREFIX + "ok"));
		}
		BaseComponent label = MDialogTexts.bungee(button.getLabel());
		// Ids in click events are lowercase; match when resolving in onCustomClick.
		CustomClickAction action = new CustomClickAction(NAMESPACE + ":" + KEY_PREFIX + button.getId().toLowerCase(Locale.ROOT));
		if (button.getTooltip() != null)
		{
			return new ActionButton(label, MDialogTexts.bungee(button.getTooltip()), button.getWidth(), action);
		}
		if (button.getWidth() != null)
		{
			return new ActionButton(label, null, button.getWidth(), action);
		}
		return new ActionButton(label, action);
	}
	
	/**
	 * Converts bodies to Bungee {@link DialogBody}; item bodies become plain text fallback.
	 *
	 * @param bodies MassiveCore bodies.
	 * @return Bungee body list.
	 */
	private static List<DialogBody> toBungeeBodies(List<MDialogBody> bodies)
	{
		List<DialogBody> out = new ArrayList<>();
		for (MDialogBody body : bodies)
		{
			if (body instanceof MDialogBodyPlain)
			{
				MDialogBodyPlain plain = (MDialogBodyPlain) body;
				if (plain.getWidth() != null)
				{
					out.add(new PlainMessageBody(MDialogTexts.bungee(plain.getMessageText()), plain.getWidth()));
				}
				else
				{
					out.add(new PlainMessageBody(MDialogTexts.bungee(plain.getMessageText())));
				}
			}
			// TODO: Spigot has no ItemBody API yet; emit a vanilla minecraft:item body for Gson to serialize.
			// Change if this ever gets improved.
			else if (body instanceof MDialogBodyItem)
			{
				MDialogBodyItem itemBody = (MDialogBodyItem) body;
				ItemStack stack = itemBody.getItem();
				String desc = itemBody.getDescription() != null
					? itemBody.getDescription()
					: (stack == null ? "Item" : stack.getType().name());
				BaseComponent component = MDialogTexts.bungee(desc);
				if (itemBody.getClickId() != null) applyCustomClick(component, itemBody.getClickId());
				
				if (stack != null)
				{
					SpigotItemDialogBody item = SpigotItemDialogBody.of(
						stack,
						component,
						itemBody.isShowDecorations(),
						itemBody.isShowTooltip(),
						itemBody.getWidth(),
						itemBody.getHeight()
					);
					if (item != null)
					{
						out.add(item);
						continue;
					}
				}
				out.add(itemBody.getWidth() != null
					? new PlainMessageBody(component, itemBody.getWidth())
					: new PlainMessageBody(component));
			}
		}
		return out;
	}
	
	/**
	 * Attaches the same namespaced custom click used by action buttons so body text completes that id.
	 * <p>
	 * Spigot requires {@link ClickEventCustom} for {@link ClickEvent.Action#CUSTOM}; a plain
	 * {@link ClickEvent} fails dialog serialization with {@link ClassCastException}.
	 * Also applies underline and hover to match Paper clickable body text.
	 * </p>
	 *
	 * @param component Root text; extras are updated recursively.
	 * @param buttonId  Action-button id.
	 */
	private static void applyCustomClick(BaseComponent component, String buttonId)
	{
		String id = NAMESPACE + ":" + KEY_PREFIX + buttonId.toLowerCase(Locale.ROOT);
		applyClickRecursive(component, new ClickEventCustom(id, null));
	}
	
	/**
	 * Sets click, underline, and hover on {@code component} and every extra child.
	 *
	 * @param component Text node.
	 * @param event     Custom click.
	 */
	private static void applyClickRecursive(BaseComponent component, ClickEvent event)
	{
		component.setClickEvent(event);
		component.setUnderlined(Boolean.TRUE);
		component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new TextComponent("Click to select"))));
		List<BaseComponent> extra = component.getExtra();
		if (extra == null) return;
		for (BaseComponent child : extra)
		{
			applyClickRecursive(child, event);
		}
	}
	
	/**
	 * Converts MassiveCore inputs to Bungee {@link DialogInput} instances.
	 *
	 * @param inputs MassiveCore input definitions.
	 * @return Bungee inputs in spec order.
	 */
	private static List<DialogInput> toBungeeInputs(List<MDialogInput> inputs)
	{
		List<DialogInput> out = new ArrayList<>();
		for (MDialogInput input : inputs)
		{
			if (input instanceof MDialogInputBool)
			{
				MDialogInputBool bool = (MDialogInputBool) input;
				BooleanInput bungee = new BooleanInput(bool.getKey(), MDialogTexts.bungee(bool.getLabelText()), bool.getInitial(), bool.getOnTrue(), bool.getOnFalse());
				out.add(bungee);
			}
			else if (input instanceof MDialogInputText)
			{
				MDialogInputText text = (MDialogInputText) input;
				TextInput.Multiline multiline = null;
				if (text.isMultiline())
				{
					multiline = new TextInput.Multiline(text.getMultilineMaxLines(), text.getMultilineHeight());
				}
				TextInput bungee = new TextInput(
					text.getKey(),
					text.getWidth(),
					MDialogTexts.bungee(text.getLabel()),
					text.isLabelVisible(),
					text.getInitial(),
					text.getMaxLength(),
					multiline
				);
				out.add(bungee);
			}
			else if (input instanceof MDialogInputNumber)
			{
				MDialogInputNumber number = (MDialogInputNumber) input;
				// Bungee NumberRangeInput's 8-arg ctor applies step/initial swapped; use fluent setters.
				NumberRangeInput bungee = new NumberRangeInput(
					number.getKey(),
					MDialogTexts.bungee(number.getLabel()),
					number.getStart(),
					number.getEnd()
				);
				if (number.getWidth() != null) bungee.width(number.getWidth());
				if (number.getLabelFormat() != null) bungee.labelFormat(number.getLabelFormat());
				if (number.getStep() != null) bungee.step(number.getStep());
				if (number.getInitial() != null) bungee.initial(number.getInitial());
				out.add(bungee);
			}
			else if (input instanceof MDialogInputSingleOption)
			{
				MDialogInputSingleOption single = (MDialogInputSingleOption) input;
				List<InputOption> options = new ArrayList<>();
				for (MDialogInputOption option : single.getOptions())
				{
					options.add(new InputOption(option.getId(), MDialogTexts.bungee(option.getDisplayText()), option.isInitial()));
				}
				out.add(new SingleOptionInput(single.getKey(), single.getWidth(), MDialogTexts.bungee(single.getLabelText()), single.isLabelVisible(), options));
			}
		}
		return out;
	}
	
	/**
	 * Maps MassiveCore after-action to Bungee {@link DialogBase.AfterAction}.
	 *
	 * @param afterAction MassiveCore setting; null defaults to close.
	 * @return Bungee after-action enum value.
	 */
	private static DialogBase.AfterAction toBungeeAfterAction(MDialogAfterAction afterAction)
	{
		if (afterAction == null) return DialogBase.AfterAction.CLOSE;
		switch (afterAction)
		{
			case NONE: return DialogBase.AfterAction.NONE;
			case WAIT_FOR_RESPONSE: return DialogBase.AfterAction.WAIT_FOR_RESPONSE;
			case CLOSE:
			default: return DialogBase.AfterAction.CLOSE;
		}
	}
	
	/**
	 * Shows a Bungee Dialog via the Spigot {@code Player.showDialog(Dialog)} overload.
	 * <p>
	 * MassiveCore compiles against Paper, where {@code Player.showDialog(DialogLike)} is also
	 * present; a direct call binds to Adventure and rejects Bungee {@link Dialog}. Looking up the
	 * method by exact parameter type selects the Spigot overload without loading Paper backends.
	 * </p>
	 *
	 * @param player Viewer.
	 * @param dialog Built Bungee dialog.
	 */
	private static void showBungeeDialog(Player player, Dialog dialog)
	{
		try
		{
			Player.class.getMethod("showDialog", Dialog.class).invoke(player, dialog);
		}
		catch (ReflectiveOperationException ex)
		{
			throw new IllegalStateException("Spigot Bungee Dialog showDialog unavailable", ex);
		}
	}
	
	/**
	 * Handles custom clicks from dialog buttons registered under {@value #NAMESPACE}.
	 *
	 * @param event Bukkit custom click event.
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onCustomClick(PlayerCustomClickEvent event)
	{
		NamespacedKey id = event.getId();
		if (id == null || !NAMESPACE.equals(id.getNamespace())) return;
		String key = id.getKey();
		if (!key.startsWith(KEY_PREFIX)) return;
		
		Player player = event.getPlayer();
		MDialogSession session = EngineMassiveCoreDialog.get().get(player);
		if (session == null) return;
		
		// Client sends current input values as JSON on the click payload.
		applyJsonData(session, readClickData(event));
		String buttonId = key.substring(KEY_PREFIX.length());
		MDialogButton button = session.findButtonIgnoreCase(buttonId);
		EngineMassiveCoreDialog.get().completeClick(player, button == null ? buttonId : button.getId());
	}
	
	/**
	 * Reads {@link PlayerCustomClickEvent#getData()} without a compile-time Gson return type.
	 * <p>
	 * MassiveCore shades {@code com.google.gson} to {@code lib.gson}. A direct
	 * {@code event.getData()} call would rewrite the invoke to expect the shaded return type,
	 * causing {@link NoSuchMethodError} against Spigot's real {@code com.google.gson.JsonElement}.
	 * Reflection + re-parse keeps both Gson trees separate.
	 * </p>
	 * <p>
	 * TODO: Drop this bridge when MassiveCore no longer shades Gson (or Spigot stops returning Gson).
	 * </p>
	 *
	 * @param event Custom click event.
	 * @return Parsed payload for this backend's Gson, or null.
	 */
	private static JsonElement readClickData(PlayerCustomClickEvent event)
	{
		if (event == null || GET_DATA == null) return null;
		try
		{
			Object raw = GET_DATA.invoke(event);
			if (raw == null) return null;
			return JsonParser.parseString(raw.toString());
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			return null;
		}
	}
	
	/**
	 * Resolves {@link PlayerCustomClickEvent#getData()} once for {@link #GET_DATA}.
	 *
	 * @return Method handle, or null if the API shape changed.
	 */
	private static Method resolveGetDataMethod()
	{
		try
		{
			return PlayerCustomClickEvent.class.getMethod("getData");
		}
		catch (NoSuchMethodException ex)
		{
			return null;
		}
	}
	
	/**
	 * Merges JSON input snapshot from the custom click into session working maps.
	 *
	 * @param session Open dialog session.
	 * @param data    Gson element from the event; must be a JSON object.
	 */
	private static void applyJsonData(MDialogSession session, JsonElement data)
	{
		if (session == null || data == null || !data.isJsonObject()) return;
		JsonObject obj = data.getAsJsonObject();
		for (MDialogInput input : session.getSpec().getInputs())
		{
			String key = input.getKey();
			if (!obj.has(key)) continue;
			JsonElement value = obj.get(key);
			if (input instanceof MDialogInputText || input instanceof MDialogInputSingleOption)
			{
				if (value.isJsonPrimitive()) session.getWorkingTexts().put(key, value.getAsString());
			}
			else if (input instanceof MDialogInputBool)
			{
				if (value.isJsonPrimitive()) session.getWorkingBooleans().put(key, value.getAsBoolean());
			}
			else if (input instanceof MDialogInputNumber)
			{
				if (value.isJsonPrimitive()) session.getWorkingNumbers().put(key, value.getAsFloat());
			}
		}
	}
}

