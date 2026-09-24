package com.massivecraft.massivecore.dialog;

import org.bukkit.entity.Player;

/**
 * Called when a dialog button is activated.
 * <p>
 * Registered on {@link MDialogButton#onClick(MDialogClickHandler)}; invoked by
 * {@link com.massivecraft.massivecore.engine.EngineMassiveCoreDialog#completeClick} after
 * backends update session working values.
 * </p>
 */
@FunctionalInterface
public interface MDialogClickHandler
{
	/**
	 * Handles a successful button click.
	 *
	 * @param player Viewer who clicked.
	 * @param response Button id plus current input values.
	 */
	void onClick(Player player, MDialogResponse response);
}
