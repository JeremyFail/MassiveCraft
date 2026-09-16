package com.massivecraft.creativegates.ui;

import com.massivecraft.creativegates.engine.create.GateCreate;
import com.massivecraft.creativegates.engine.create.PendingGateCreate;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.massivecore.dialog.MDialog;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.type.MDialogTypeMultiAction;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Gate fill selection via MassiveCore {@link MDialog} (Paper / Spigot / ChestGui).
 */
public final class GateFillPicker
{
	private GateFillPicker()
	{
	}
	
	public static void open(Player player, PendingGateCreate pending, List<GateType> selectable)
	{
		if (player == null || pending == null || selectable == null || selectable.isEmpty()) return;
		
		MDialogTypeMultiAction.Builder type = MDialogTypeMultiAction.builder()
			.columns(Math.min(3, Math.max(1, selectable.size())))
			.exit(MDialogButton.of("cancel", "Cancel")
				.tooltip("Cancel gate creation")
				.onClick((p, response) -> GateCreate.cancel(p, true)));
		
		for (GateType gateType : selectable)
		{
			String typeId = gateType.getConfigId();
			String label = prettyName(gateType);
			type.action(MDialogButton.of(typeId, label)
				.tooltip("Use " + label)
				.icon(iconFor(gateType))
				.onClick((p, response) -> GateCreate.complete(p, pending, gateType)));
		}
		
		MDialog.open(player, MDialog.builder()
			.title("<h>Select Gate Fill")
			.bodyPlain("Choose the fill material for this gate.")
			.canCloseWithEscape(true)
			.type(type.build())
			.onClose(p -> GateCreate.cancel(p, true))
			.build());
	}
	
	private static ItemStack iconFor(GateType type)
	{
		Material material = type.getBaseMaterial();
		if (material == null || !material.isItem())
		{
			material = Material.BARRIER;
		}
		ItemStack stack = new ItemStack(material);
		ItemMeta meta = stack.getItemMeta();
		if (meta != null)
		{
			meta.setDisplayName(Txt.parse("<h>%s", prettyName(type)));
			stack.setItemMeta(meta);
		}
		return stack;
	}
	
	private static String prettyName(GateType type)
	{
		return Txt.getMaterialName(type.getBaseMaterial());
	}
}
