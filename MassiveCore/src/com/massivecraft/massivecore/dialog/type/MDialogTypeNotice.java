package com.massivecraft.massivecore.dialog.type;

import com.massivecraft.massivecore.dialog.MDialogButton;

/**
 * Notice dialog: one OK-style button.
 * <p>
 * Default spec type when {@link com.massivecraft.massivecore.dialog.MDialogBuilder} does not set another.
 * </p>
 */
public final class MDialogTypeNotice implements MDialogType
{
	/** Primary dismiss / acknowledge button. */
	private final MDialogButton action;
	
	/**
	 * @param action Button shown at the bottom.
	 */
	private MDialogTypeNotice(MDialogButton action)
	{
		this.action = action;
	}
	
	/**
	 * Creates a notice with id {@code ok} and label {@code OK}.
	 *
	 * @return Default notice type.
	 */
	public static MDialogTypeNotice of()
	{
		return new MDialogTypeNotice(MDialogButton.of("ok", "OK"));
	}
	
	/**
	 * Creates a notice with a custom action button.
	 *
	 * @param action Button to show.
	 * @return Notice type.
	 */
	public static MDialogTypeNotice of(MDialogButton action)
	{
		return new MDialogTypeNotice(action);
	}
	
	/**
	 * @return The single action button.
	 */
	public MDialogButton getAction() { return this.action; }
}
