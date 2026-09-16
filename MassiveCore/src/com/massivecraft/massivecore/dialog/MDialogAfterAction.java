package com.massivecraft.massivecore.dialog;

/**
 * What happens after a dialog button is clicked (mirrors vanilla DialogAfterAction).
 * <p>
 * Passed through {@link MDialogSpec#getAfterAction()} to native dialog backends.
 * </p>
 */
public enum MDialogAfterAction
{
	/** Close the dialog after the click. */
	CLOSE,
	
	/** Leave the dialog open. */
	NONE,
	
	/** Keep dialog open until the server sends another dialog packet. */
	WAIT_FOR_RESPONSE,
	;
}
