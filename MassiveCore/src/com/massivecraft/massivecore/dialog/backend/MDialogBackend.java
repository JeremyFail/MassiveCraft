package com.massivecraft.massivecore.dialog.backend;

import com.massivecraft.massivecore.dialog.MDialogSession;
import com.massivecraft.massivecore.dialog.MDialogSpec;
import org.bukkit.entity.Player;

/**
 * Runtime dialog renderer selected by {@link com.massivecraft.massivecore.dialog.MDialog}.
 * <p>
 * Implementations map an {@link MDialogSpec} to Paper Dialog, Spigot Bungee Dialog, or ChestGui fallback.
 * </p>
 */
public interface MDialogBackend
{
	/**
	 * Shows the dialog described by {@code spec} to {@code player} and associates it with {@code session}.
	 *
	 * @param player  Viewer; must be online.
	 * @param spec    Dialog layout, inputs, and type.
	 * @param session Mutable working state for inputs and completion handlers.
	 */
	void open(Player player, MDialogSpec spec, MDialogSession session);
}
