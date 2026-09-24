package com.massivecraft.massivecore.dialog;

import com.massivecraft.massivecore.dialog.body.MDialogBody;
import com.massivecraft.massivecore.dialog.body.MDialogBodyItem;
import com.massivecraft.massivecore.dialog.body.MDialogBodyPlain;
import com.massivecraft.massivecore.dialog.input.MDialogInput;
import com.massivecraft.massivecore.dialog.input.MDialogInputBool;
import com.massivecraft.massivecore.dialog.input.MDialogInputNumber;
import com.massivecraft.massivecore.dialog.input.MDialogInputSingleOption;
import com.massivecraft.massivecore.dialog.input.MDialogInputText;
import com.massivecraft.massivecore.dialog.type.MDialogType;
import com.massivecraft.massivecore.mson.Mson;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for immutable {@link MDialogSpec} instances.
 * <p>
 * String fields accept MassiveCore {@code Txt} color codes (e.g. {@code <h>Title});
 * backends convert them for Adventure / Bungee as needed.
 * </p>
 */
public final class MDialogBuilder
{
	/** Dialog window title. */
	private String title = "";
	
	/** Optional title shown on buttons that open this dialog from a dialog-list. */
	private String externalTitle;
	
	/** Whether Escape may dismiss the dialog (Dialog API). */
	private boolean canCloseWithEscape = true;
	
	/** Whether the client should pause (singleplayer-style) while open. */
	private boolean pause = false;
	
	/** Behavior after a button is pressed. */
	private MDialogAfterAction afterAction = MDialogAfterAction.CLOSE;
	
	/** Ordered body elements (messages / items). */
	private final List<MDialogBody> bodies = new ArrayList<>();
	
	/** Ordered input fields. */
	private final List<MDialogInput> inputs = new ArrayList<>();
	
	/** Dialog chrome type (notice, confirmation, multi-action, …). */
	private MDialogType type;
	
	/** Invoked when the UI closes without a completing button click. */
	private MDialogCloseHandler closeHandler;
	
	/**
	 * Package-private; use {@link MDialog#builder()}.
	 */
	MDialogBuilder()
	{
	}
	
	/**
	 * Sets the main dialog title.
	 *
	 * @param title Title text; null becomes empty.
	 * @return This builder.
	 */
	public MDialogBuilder title(String title)
	{
		this.title = title == null ? "" : title;
		return this;
	}
	
	/**
	 * Sets the external title used when this dialog appears as a child in a dialog-list.
	 *
	 * @param externalTitle External label; may be null.
	 * @return This builder.
	 */
	public MDialogBuilder externalTitle(String externalTitle)
	{
		this.externalTitle = externalTitle;
		return this;
	}
	
	/**
	 * Controls whether the player may close with Escape on Dialog backends.
	 *
	 * @param canCloseWithEscape True to allow Escape.
	 * @return This builder.
	 */
	public MDialogBuilder canCloseWithEscape(boolean canCloseWithEscape)
	{
		this.canCloseWithEscape = canCloseWithEscape;
		return this;
	}
	
	/**
	 * Sets the pause flag passed through to Dialog backends.
	 *
	 * @param pause True to request client pause.
	 * @return This builder.
	 */
	public MDialogBuilder pause(boolean pause)
	{
		this.pause = pause;
		return this;
	}
	
	/**
	 * Sets what happens after a dialog action (close, none, wait for response).
	 *
	 * @param afterAction After-action policy; may be null (treated as CLOSE at build).
	 * @return This builder.
	 */
	public MDialogBuilder afterAction(MDialogAfterAction afterAction)
	{
		this.afterAction = afterAction;
		return this;
	}
	
	/**
	 * Appends a body element.
	 *
	 * @param body Body entry; ignored if null.
	 * @return This builder.
	 */
	public MDialogBuilder body(MDialogBody body)
	{
		if (body != null) this.bodies.add(body);
		return this;
	}
	
	/**
	 * Convenience for {@link MDialogBodyPlain}.
	 *
	 * @param message Plain message (Txt codes allowed).
	 * @return This builder.
	 */
	public MDialogBuilder bodyPlain(String message)
	{
		return this.body(MDialogBodyPlain.of(message));
	}
	
	/**
	 * Convenience for {@link MDialogBodyPlain} with rich {@link MDialogText}.
	 *
	 * @param message Body text.
	 * @return This builder.
	 */
	public MDialogBuilder bodyPlain(MDialogText message)
	{
		return this.body(MDialogBodyPlain.of(message));
	}
	
	/**
	 * Convenience for {@link MDialogBodyPlain} with {@link Mson}.
	 *
	 * @param message Body text.
	 * @return This builder.
	 */
	public MDialogBuilder bodyPlain(Mson message)
	{
		return this.body(MDialogBodyPlain.of(message));
	}
	
	/**
	 * Convenience for {@link MDialogBodyItem}.
	 *
	 * @param item Item shown in the body.
	 * @return This builder.
	 */
	public MDialogBuilder bodyItem(ItemStack item)
	{
		return this.body(MDialogBodyItem.of(item));
	}
	
	/**
	 * Appends an input field.
	 *
	 * @param input Input definition; ignored if null.
	 * @return This builder.
	 */
	public MDialogBuilder input(MDialogInput input)
	{
		if (input != null) this.inputs.add(input);
		return this;
	}
	
	/**
	 * Adds a boolean input with defaults.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return This builder.
	 */
	public MDialogBuilder inputBool(String key, String label)
	{
		return this.input(MDialogInputBool.of(key, label));
	}
	
	/**
	 * Adds a boolean input with rich {@link MDialogText} label.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return This builder.
	 */
	public MDialogBuilder inputBool(String key, MDialogText label)
	{
		return this.input(MDialogInputBool.of(key, label));
	}
	
	/**
	 * Adds a boolean input with {@link Mson} label.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return This builder.
	 */
	public MDialogBuilder inputBool(String key, Mson label)
	{
		return this.input(MDialogInputBool.of(key, label));
	}
	
	/**
	 * Adds a text input with defaults.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return This builder.
	 */
	public MDialogBuilder inputText(String key, String label)
	{
		return this.input(MDialogInputText.of(key, label));
	}
	
	/**
	 * Adds a number-range input with the given bounds.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @param start Inclusive minimum.
	 * @param end Inclusive maximum.
	 * @return This builder.
	 */
	public MDialogBuilder inputNumber(String key, String label, float start, float end)
	{
		return this.input(MDialogInputNumber.of(key, label, start, end));
	}
	
	/**
	 * Adds a single-option input (use the input's own builder for options).
	 *
	 * @param input Built single-option input.
	 * @return This builder.
	 */
	public MDialogBuilder inputSingleOption(MDialogInputSingleOption input)
	{
		return this.input(input);
	}
	
	/**
	 * Sets the dialog type / bottom chrome.
	 *
	 * @param type Type implementation; null becomes notice at build time.
	 * @return This builder.
	 */
	public MDialogBuilder type(MDialogType type)
	{
		this.type = type;
		return this;
	}
	
	/**
	 * Sets the cancel/close handler (no completing button).
	 *
	 * @param closeHandler Handler; may be null.
	 * @return This builder.
	 */
	public MDialogBuilder onClose(MDialogCloseHandler closeHandler)
	{
		this.closeHandler = closeHandler;
		return this;
	}
	
	/**
	 * Creates an immutable {@link MDialogSpec} from the current state.
	 *
	 * @return Built spec; never null.
	 */
	public MDialogSpec build()
	{
		return new MDialogSpec(this.title, this.externalTitle, this.canCloseWithEscape, this.pause, this.afterAction, this.bodies, this.inputs, this.type, this.closeHandler);
	}
}
