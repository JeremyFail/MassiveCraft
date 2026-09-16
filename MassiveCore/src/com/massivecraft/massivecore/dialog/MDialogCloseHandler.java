package com.massivecraft.massivecore.dialog;

import org.bukkit.entity.Player;

/**
 * Called when a dialog is closed without a successful button action (escape / inventory close).
 * <p>
 * Set on {@link MDialogBuilder} via {@code onClose}; not invoked when a button handler runs.
 * </p>
 */
@FunctionalInterface
public interface MDialogCloseHandler
{
	/**
	 * Handles dismiss without a button completion.
	 *
	 * @param player Viewer who closed the dialog.
	 */
	void onClose(Player player);
}
