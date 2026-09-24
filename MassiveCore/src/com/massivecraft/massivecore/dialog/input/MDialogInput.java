package com.massivecraft.massivecore.dialog.input;

/**
 * Dialog input field keyed for {@link com.massivecraft.massivecore.dialog.MDialogResponse}.
 * <p>
 * Each implementation exposes a stable {@link #getKey()} used in working session maps and responses.
 * </p>
 */
public interface MDialogInput
{
	/**
	 * @return Response map key for this input.
	 */
	String getKey();
}
