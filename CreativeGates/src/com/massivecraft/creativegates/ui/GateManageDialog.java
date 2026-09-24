package com.massivecraft.creativegates.ui;

import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.gate.GateSetting;
import com.massivecraft.massivecore.dialog.MDialog;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.type.MDialogTypeMultiAction;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;

/**
 * Native Dialog manage UI for gate settings.
 * <p>
 * Uses action buttons (not {@code single_option} inputs) because Minecraft ignores
 * {@code hover_event} on dialog input controls - see MC-298405. Action buttons have a
 * real {@code tooltip} field that the client shows.
 * </p>
 */
public final class GateManageDialog
{
	/** Shared button width so every setting row aligns. */
	private static final int SETTING_WIDTH = 200;
	
	private GateManageDialog()
	{
	}
	
	/**
	 * Opens the manage dialog for the gate.
	 *
	 * @param player Managing player.
	 * @param gate Target gate.
	 */
	public static void open(Player player, UGate gate)
	{
		if (player == null || gate == null) return;
		
		String ownerName = IdUtil.getName(gate.getCreatorId());
		if (ownerName == null || ownerName.isEmpty()) ownerName = "unknown";
		
		MDialogTypeMultiAction.Builder type = MDialogTypeMultiAction.builder()
			.columns(1)
			.exit(MDialogButton.of("close", "Close")
				.tooltip("Close the manage dialog.")
				.width(SETTING_WIDTH));
		
		for (GateSetting setting : GateSetting.values())
		{
			type.action(settingButton(setting, gate));
		}
		
		if (GateFillPicker.canChangeFill(player, gate))
		{
			type.action(fillButton(gate));
		}
		
		MDialog.open(player, MDialog.builder()
			.title("<h>Manage Gate")
			.bodyPlain(Txt.parse("<k>Owner: <h>%s\n<k>Network: <h>%s", ownerName, String.valueOf(gate.getNetworkId())))
			.type(type.build())
		);
	}
	
	/**
	 * One setting as an action button: click toggles and reopens; tooltip shows the description.
	 */
	private static MDialogButton settingButton(GateSetting setting, UGate gate)
	{
		boolean value = setting.get(gate);
		String yesNo = value ? "<g>YES" : "<b>NOO";
		return MDialogButton.of(setting.getId(), settingColor(setting) + setting.getDisplayName() + "<i>: " + yesNo)
			.tooltip(setting.getDescription())
			.width(SETTING_WIDTH)
			.onClick((player, response) ->
			{
				setting.set(gate, !value);
				open(player, gate);
			});
	}
	
	/**
	 * Opens {@link GateFillPicker} for this gate. Label green, current fill purple.
	 */
	private static MDialogButton fillButton(UGate gate)
	{
		String fillName = GateFillPicker.currentFillLabel(gate);
		return MDialogButton.of("fill", "<g>Gate Fill<i>: <v>" + fillName)
			.tooltip("The current gate fill block/particle. Click to modify.")
			.width(SETTING_WIDTH)
			.onClick((player, response) -> GateFillPicker.open(player, gate));
	}
	
	/**
	 * Group colors match inspect sections.
	 */
	private static String settingColor(GateSetting setting)
	{
		switch (setting)
		{
			case SECRET:
				return "<h>";
			case ENTRY:
			case EXIT:
				return "<k>";
			case PLAYERS:
			case MOBS:
			case VEHICLES:
			default:
				return "<i>";
		}
	}
	
}
