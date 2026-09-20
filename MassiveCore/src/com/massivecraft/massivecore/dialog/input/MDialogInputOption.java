package com.massivecraft.massivecore.dialog.input;

import com.massivecraft.massivecore.dialog.MDialogText;
import com.massivecraft.massivecore.mson.Mson;

/**
 * One choice in a single-option dialog input.
 * <p>
 * The {@link #id} is stored in {@link com.massivecraft.massivecore.dialog.MDialogResponse#getText(String)}
 * for the parent {@link MDialogInputSingleOption} key.
 * </p>
 */
public final class MDialogInputOption
{
	/** Stable option id returned in responses. */
	private final String id;
	
	/** Text shown to the player. */
	private final MDialogText display;
	
	/** Whether this option is selected when the dialog opens. */
	private final boolean initial;
	
	private MDialogInputOption(String id, MDialogText display, boolean initial)
	{
		this.id = id;
		this.display = display == null ? MDialogText.txt(id == null ? "" : id) : display;
		this.initial = initial;
	}
	
	/**
	 * Creates an option that is not initially selected.
	 *
	 * @param id Option id.
	 * @param display Visible label (Txt tags allowed).
	 * @return New option.
	 */
	public static MDialogInputOption of(String id, String display)
	{
		return new MDialogInputOption(id, MDialogText.txt(display), false);
	}
	
	/**
	 * Creates an option that is not initially selected.
	 *
	 * @param id Option id.
	 * @param display Visible label.
	 * @return New option.
	 */
	public static MDialogInputOption of(String id, MDialogText display)
	{
		return new MDialogInputOption(id, display, false);
	}
	
	/**
	 * Creates an option that is not initially selected.
	 *
	 * @param id Option id.
	 * @param display Visible label.
	 * @return New option.
	 */
	public static MDialogInputOption of(String id, Mson display)
	{
		return new MDialogInputOption(id, MDialogText.mson(display), false);
	}
	
	/**
	 * Returns a copy marked as the initial selection.
	 *
	 * @param initial True to select on open.
	 * @return New option instance.
	 */
	public MDialogInputOption initial(boolean initial)
	{
		return new MDialogInputOption(this.id, this.display, initial);
	}
	
	/**
	 * @return Option id.
	 */
	public String getId() { return this.id; }
	
	/**
	 * @return Rich display text.
	 */
	public MDialogText getDisplayText() { return this.display; }
	
	/**
	 * Plain / legacy display for ChestGui and string callers.
	 *
	 * @return Styled plain display.
	 */
	public String getDisplay() { return this.display.toPlain(); }
	
	/**
	 * @return True if this option starts selected.
	 */
	public boolean isInitial() { return this.initial; }
}
