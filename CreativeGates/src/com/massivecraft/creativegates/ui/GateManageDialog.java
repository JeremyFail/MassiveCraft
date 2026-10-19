package com.massivecraft.creativegates.ui;

import com.massivecraft.creativegates.entity.GateScopeType;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.gate.GateSetting;
import com.massivecraft.creativegates.util.GateChainReorder;
import com.massivecraft.creativegates.util.GateNetworkRename;
import com.massivecraft.creativegates.util.GateNetworkTransfer;
import com.massivecraft.creativegates.util.GateScopeUtil;
import com.massivecraft.massivecore.dialog.MDialog;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.MDialogResponse;
import com.massivecraft.massivecore.dialog.input.MDialogInputText;
import com.massivecraft.massivecore.dialog.type.MDialogTypeConfirmation;
import com.massivecraft.massivecore.dialog.type.MDialogTypeMultiAction;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;

import java.util.List;

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
	
	private static final String NETWORK_NAME_KEY = "networkName";
	
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
		List<UGate> chain = UGateColl.get().getGateChain(gate);
		int chainPos = chain.indexOf(gate) + 1;
		
		MDialogTypeMultiAction.Builder type = MDialogTypeMultiAction.builder()
			.columns(1)
			.exit(MDialogButton.of("close", "Close")
				.tooltip("Close the manage dialog.")
				.width(SETTING_WIDTH));
		
		boolean canChangeScope = GateScopeUtil.canChangeGateScope(player, gate);
		boolean canReorder = GateChainReorder.canReorder(player, gate);
		if (canChangeScope)
		{
			type.action(scopeButton(gate));
		}
		type.action(networkButton(player, gate));
		if (canReorder)
		{
			type.action(orderButton(gate));
		}
		
		for (GateSetting setting : GateSetting.values())
		{
			type.action(settingButton(setting, gate));
		}
		
		if (GateFillPicker.canChangeFill(player, gate))
		{
			type.action(fillButton(gate));
		}
		
		// Editable actions omit matching read-only info lines (Scope / Chain Order).
		StringBuilder body = new StringBuilder();
		body.append(Txt.parse("<k>Creator: <h>%s", ownerName));
		if (!canChangeScope)
		{
			body.append('\n').append(Txt.parse("<k>Scope: <h>%s", gate.getScopeType().name()));
		}
		body.append('\n').append(Txt.parse(
			"<k>Network Owner: <h>%s",
			GateScopeUtil.describeOwner(gate.getScopeType(), gate.getNetworkOwnerId())
		));
		if (!canReorder)
		{
			body.append('\n').append(Txt.parse(
				"<k>Chain Order: <h>%d/%d",
				Math.max(chainPos, 0),
				chain.size()
			));
		}
		
		MDialog.open(player, MDialog.builder()
			.title("<h>Manage Gate")
			.bodyPlain(body.toString())
			.type(type.build())
		);
	}
	
	/**
	 * Opens the chain-order sub-dialog (move up / position / move down).
	 *
	 * @param player Managing player.
	 * @param gate Target gate.
	 */
	public static void openOrder(Player player, UGate gate)
	{
		if (player == null || gate == null) return;
		if (!GateChainReorder.canReorder(player, gate))
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>You may not reorder that gate."));
			open(player, gate);
			return;
		}
		
		int pos = GateChainReorder.displayPos(gate);
		int size = GateChainReorder.chainSize(gate);
		
		MDialogTypeMultiAction.Builder type = MDialogTypeMultiAction.builder()
			.columns(1)
			.exit(MDialogButton.of("back", "Back")
				.tooltip("Return to gate management.")
				.width(SETTING_WIDTH)
				.onClick((p, response) -> open(p, gate)));
		
		type.action(MDialogButton.of("up", "<g>Move Earlier (+)")
			.tooltip("Move this gate earlier in the travel order.")
			.width(SETTING_WIDTH)
			.onClick((p, response) ->
			{
				GateChainReorder.moveUp(p, gate, true);
				openOrder(p, gate);
			}));
		
		type.action(MDialogButton.of("pos", "<k>Position: <h>" + pos + "/" + size)
			.tooltip("Current travel position. Click to refresh.")
			.width(SETTING_WIDTH)
			.onClick((p, response) -> openOrder(p, gate)));
		
		type.action(MDialogButton.of("down", "<i>Move Later (−)")
			.tooltip("Move this gate later in the travel order.")
			.width(SETTING_WIDTH)
			.onClick((p, response) ->
			{
				GateChainReorder.moveDown(p, gate, true);
				openOrder(p, gate);
			}));
		
		MDialog.open(player, MDialog.builder()
			.title("<h>Chain Order")
			.bodyPlain(Txt.parse(
				"<i>Travel follows ascending chain order.\n<k>Network: <h>%s\n<k>Scope: <h>%s",
				String.valueOf(gate.getNetworkId()),
				gate.getScopeType().name()
			))
			.type(type.build())
		);
	}
	
	/**
	 * Opens the scope sub-dialog (cycle through scopes).
	 * 
	 * @param gate The gate to display the scope for.
	 * @return The action button.
	 */
	private static MDialogButton scopeButton(UGate gate)
	{
		return MDialogButton.of("scope", "<k>Scope<i>: <h>" + gate.getScopeType().name())
			.tooltip("Click to cycle this gate into the next scope you may use.")
			.width(SETTING_WIDTH)
			.onClick((player, response) ->
			{
				GateScopeType next = GateScopeUtil.nextOfferedScope(player, gate.getScopeType());
				if (next == null)
				{
					MixinMessage.get().messageOne(player, Txt.parse("<b>No other scopes are available to you."));
					open(player, gate);
					return;
				}
				if (next == gate.getScopeType())
				{
					MixinMessage.get().messageOne(player, Txt.parse("<i>Already <h>%s<i>.", next.name()));
					open(player, gate);
					return;
				}
				GateNetworkTransfer.transfer(player, gate, next, null, true);
				open(player, gate);
			});
	}
	
	/**
	 * Opens the network sub-dialog (rename / transfer).
	 * 
	 * @param viewer The player who is viewing the network.
	 * @param gate The gate that is being viewed.
	 * @return The action button.
	 */
	private static MDialogButton networkButton(Player viewer, UGate gate)
	{
		String name = String.valueOf(gate.getNetworkId());
		return MDialogButton.of("network", "<k>Network<i>: <h>" + name)
			.tooltip(GateNetworkRename.costTooltip(viewer))
			.width(SETTING_WIDTH)
			.onClick((player, response) ->
			{
				if (!GateNetworkRename.canAfford(player))
				{
					MixinMessage.get().messageOne(player, Txt.parse(
						"<b>You need <h>%d <b>XP level%s to rename a network.",
						GateNetworkRename.xpCostLevels(player),
						GateNetworkRename.xpCostLevels(player) == 1 ? "" : "s"
					));
					open(player, gate);
					return;
				}
				openNetworkRename(player, gate);
			});
	}
	
	/**
	 * Opens a confirmation dialog with a text field for the new network name.
	 *
	 * @param player Managing player.
	 * @param gate Target gate.
	 */
	public static void openNetworkRename(Player player, UGate gate)
	{
		if (player == null || gate == null) return;
		if (!GateNetworkRename.canAfford(player))
		{
			MixinMessage.get().messageOne(player, Txt.parse(
				"<b>You need <h>%d <b>XP level%s to rename a network.",
				GateNetworkRename.xpCostLevels(player),
				GateNetworkRename.xpCostLevels(player) == 1 ? "" : "s"
			));
			open(player, gate);
			return;
		}
		
		int cost = GateNetworkRename.xpCostLevels(player);
		String costLine = cost > 0
			? Txt.parse("\n<k>Cost: <h>%d <k>XP level%s.", cost, cost == 1 ? "" : "s")
			: "";
		
		MDialog.open(player, MDialog.builder()
			.title("<h>Change Network")
			.bodyPlain(Txt.parse(
				"<i>Current: <h>%s%s\n<i>Enter a new network name. This gate leaves its old chain.",
				String.valueOf(gate.getNetworkId()),
				costLine
			))
			.canCloseWithEscape(true)
			.onClose(p -> open(p, gate))
			.input(MDialogInputText.of(NETWORK_NAME_KEY, "Network name")
				.initial(String.valueOf(gate.getNetworkId()))
				.maxLength(64)
				.width(SETTING_WIDTH))
			.type(MDialogTypeConfirmation.of(
				MDialogButton.of("save", "Save")
					.tooltip(GateNetworkRename.costTooltip(player))
					.onClick((p, response) -> finishNetworkRename(p, gate, response)),
				MDialogButton.of("back", "Back")
					.tooltip("Return to gate management.")
					.onClick((p, response) -> open(p, gate))
			))
		);
	}
	
	/**
	 * Finishes the network rename process.
	 * 
	 * @param player The player who is renaming the network.
	 * @param gate The gate that is being renamed.
	 * @param response The response from the dialog.
	 */
	private static void finishNetworkRename(Player player, UGate gate, MDialogResponse response)
	{
		String raw = response != null ? response.getText(NETWORK_NAME_KEY) : null;
		GateNetworkRename.rename(player, gate, raw, true);
		open(player, gate);
	}
	
	/**
	 * Opens the chain-order sub-dialog (move up / position / move down).
	 * 
	 * @param gate The gate to display the chain order for.
	 * @return The action button.
	 */
	private static MDialogButton orderButton(UGate gate)
	{
		int pos = GateChainReorder.displayPos(gate);
		int size = GateChainReorder.chainSize(gate);
		return MDialogButton.of("order", "<k>Chain Order<i>: <h>" + pos + "/" + size)
			.tooltip("Open controls to move this gate earlier or later in the chain.")
			.width(SETTING_WIDTH)
			.onClick((player, response) -> openOrder(player, gate));
	}
	
	/**
	 * One setting as an action button: click toggles and reopens; tooltip shows the description.
	 * 
	 * @param setting The setting to display.
	 * @param gate The gate to display the setting for.
	 * @return The action button.
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
	 * 
	 * @param gate The gate to display the fill for.
	 * @return The action button.
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
	 * 
	 * @param setting The setting to get the color for.
	 * @return The color.
	 */
	private static String settingColor(GateSetting setting)
	{
		switch (setting)
		{
			case SECRET:
			case ENTRY:
			case EXIT:
				return "<h>";
			case PLAYERS:
			case MOBS:
			case VEHICLES:
			default:
				return "<i>";
		}
	}
	
}
