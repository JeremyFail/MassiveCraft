package com.massivecraft.creativegates.ui;

import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.gate.GateSetting;
import com.massivecraft.massivecore.dialog.MDialog;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.MDialogResponse;
import com.massivecraft.massivecore.dialog.MDialogText;
import com.massivecraft.massivecore.dialog.input.MDialogInputSingleOption;
import com.massivecraft.massivecore.dialog.type.MDialogTypeConfirmation;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;

/**
 * Native Dialog confirmation UI for editing gate settings
 * (cycling YES/NOO single-option inputs + Save/Cancel).
 */
public final class GateManageDialog
{
	/** Shared control width so every setting row aligns. */
	private static final int SETTING_WIDTH = 200;
	
	private static final String OPTION_YES = "yes";
	private static final String OPTION_NO = "no";
	
	private GateManageDialog()
	{
	}
	
	/**
	 * Opens the manage confirmation dialog for the gate.
	 *
	 * @param player Managing player.
	 * @param gate Target gate.
	 */
	public static void open(Player player, UGate gate)
	{
		if (player == null || gate == null) return;
		
		String ownerName = IdUtil.getName(gate.getCreatorId());
		if (ownerName == null || ownerName.isEmpty()) ownerName = "unknown";
		
		MDialog.open(player, MDialog.builder()
			.title("<h>Manage Gate")
			.bodyPlain(Txt.parse("<k>Network: <h>%s", String.valueOf(gate.getNetworkId())))
			.bodyPlain(Txt.parse("<k>Owner: <h>%s", ownerName))
			.input(settingInput(GateSetting.SECRET, gate))
			.input(settingInput(GateSetting.ENTRY, gate))
			.input(settingInput(GateSetting.EXIT, gate))
			.input(settingInput(GateSetting.PLAYERS, gate))
			.input(settingInput(GateSetting.MOBS, gate))
			.input(settingInput(GateSetting.VEHICLES, gate))
			.type(MDialogTypeConfirmation.of(
				MDialogButton.of("save", "Save").onClick((p, response) -> apply(p, gate, response)),
				MDialogButton.of("cancel", "Cancel")
			))
		);
	}
	
	/**
	 * Click-to-toggle YES/NOO control for one gate setting.
	 */
	private static MDialogInputSingleOption settingInput(GateSetting setting, UGate gate)
	{
		boolean value = setting.get(gate);
		return MDialogInputSingleOption.builder(setting.getId(), settingLabel(setting))
			.width(SETTING_WIDTH)
			.option(OPTION_YES, "<g>YES", value)
			.option(OPTION_NO, "<b>NOO", !value)
			.build();
	}
	
	/**
	 * Colored label with hover description; group colors match inspect sections.
	 */
	private static MDialogText settingLabel(GateSetting setting)
	{
		String color;
		switch (setting)
		{
			case SECRET:
				color = "<h>";
				break;
			case ENTRY:
			case EXIT:
				color = "<k>";
				break;
			case PLAYERS:
			case MOBS:
			case VEHICLES:
			default:
				color = "<i>";
				break;
		}
		
		return MDialogText.txt(color + setting.getDisplayName(), setting.getDescription());
	}
	
	/**
	 * Applies the changes from the manage dialog to the gate.
	 * 
	 * @param player The player who made the changes.
	 * @param gate The gate to apply the changes to.
	 * @param response The response from the manage dialog.
	 */
	private static void apply(Player player, UGate gate, MDialogResponse response)
	{
		if (player == null || gate == null || response == null) return;
		
		for (GateSetting setting : GateSetting.values())
		{
			String selected = response.getText(setting.getId());
			if (selected == null) continue;
			
			if (OPTION_YES.equalsIgnoreCase(selected))
			{
				setting.set(gate, true);
			}
			else if (OPTION_NO.equalsIgnoreCase(selected))
			{
				setting.set(gate, false);
			}
		}
		
		MixinMessage.get().messageOne(player, Txt.parse("<g>Gate settings saved."));
	}
	
}
