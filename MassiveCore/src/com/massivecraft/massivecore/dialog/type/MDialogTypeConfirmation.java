package com.massivecraft.massivecore.dialog.type;

import com.massivecraft.massivecore.dialog.MDialogButton;

/**
 * Confirmation dialog: yes / no buttons.
 */
public final class MDialogTypeConfirmation implements MDialogType
{
	/** Affirmative button. */
	private final MDialogButton yes;
	
	/** Negative button. */
	private final MDialogButton no;
	
	/**
	 * @param yes Yes button.
	 * @param no No button.
	 */
	private MDialogTypeConfirmation(MDialogButton yes, MDialogButton no)
	{
		this.yes = yes;
		this.no = no;
	}
	
	/**
	 * Creates a yes/no confirmation layout.
	 *
	 * @param yes Yes button.
	 * @param no No button.
	 * @return Confirmation type.
	 */
	public static MDialogTypeConfirmation of(MDialogButton yes, MDialogButton no)
	{
		return new MDialogTypeConfirmation(yes, no);
	}
	
	/**
	 * @return Yes button.
	 */
	public MDialogButton getYes() { return this.yes; }
	
	/**
	 * @return No button.
	 */
	public MDialogButton getNo() { return this.no; }
}
