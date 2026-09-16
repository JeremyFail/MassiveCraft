package com.massivecraft.massivecore.dialog.input;

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
	private final String display;
	
	/** Whether this option is selected when the dialog opens. */
	private final boolean initial;
	
	/**
	 * @param id Option id.
	 * @param display Display string.
	 * @param initial Initial selection flag.
	 */
	private MDialogInputOption(String id, String display, boolean initial)
	{
		this.id = id;
		this.display = display;
		this.initial = initial;
	}
	
	/**
	 * Creates an option that is not initially selected.
	 *
	 * @param id Option id.
	 * @param display Visible label.
	 * @return New option.
	 */
	public static MDialogInputOption of(String id, String display)
	{
		return new MDialogInputOption(id, display, false);
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
	 * @return Display label.
	 */
	public String getDisplay() { return this.display; }
	
	/**
	 * @return True if this option starts selected.
	 */
	public boolean isInitial() { return this.initial; }
}
