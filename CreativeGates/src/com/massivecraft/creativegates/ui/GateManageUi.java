package com.massivecraft.creativegates.ui;

import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.massivecore.dialog.MDialog;
import org.bukkit.entity.Player;

/**
 * Chooses native Dialog manage UI when available; otherwise Factions-style chat manage.
 * Never uses ChestGui for gate management.
 */
public final class GateManageUi
{
	private GateManageUi()
	{
	}
	
	/**
	 * Opens the appropriate manage UI for the player and gate.
	 *
	 * @param player Managing player.
	 * @param gate Target gate.
	 */
	public static void open(Player player, UGate gate)
	{
		if (player == null || gate == null) return;
		
		if (MDialog.isNativeDialogAvailable())
		{
			GateManageDialog.open(player, gate);
		}
		else
		{
			GateManageChat.open(player, gate, 1);
		}
	}
	
}
