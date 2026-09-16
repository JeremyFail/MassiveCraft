package com.massivecraft.creativegates.engine.create;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.engine.PendingGateCreates;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Shared gate creation completion after fill type is known.
 */
public final class GateCreate
{
	private GateCreate()
	{
	}
	
	/**
	 * Creates the gate from a pending snapshot and selected fill type.
	 *
	 * @return True if the gate was created.
	 */
	public static boolean complete(Player player, PendingGateCreate pending, GateType gateType)
	{
		if (player == null || pending == null || gateType == null) return false;
		
		PendingGateCreates.get().remove(player);
		
		if (!Perm.CREATE.has(player, MConf.get().verboseCreatePermission)) return false;
		
		if (!player.getUniqueId().equals(pending.getPlayerId())) return false;
		if (!player.getWorld().getName().equals(pending.getWorldName()))
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>Gate creation cancelled because you changed worlds."));
			return false;
		}
		
		World world = Bukkit.getWorld(pending.getWorldName());
		if (world == null)
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>Gate creation cancelled because the world is unavailable."));
			return false;
		}
		
		if (!MConf.get().isGateTypeAllowed(gateType, pending.getOrientation()))
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>That gate fill is no longer allowed."));
			return false;
		}
		
		for (PS coord : pending.getCoords())
		{
			Block block = world.getBlockAt(coord.getBlockX(), coord.getBlockY(), coord.getBlockZ());
			if (UGate.get(block) != null)
			{
				MixinMessage.get().messageOne(player, Txt.parse("<b>There is no room for a new gate since there is already one here."));
				return false;
			}
		}
		
		for (PS coord : pending.getInteriorCoords())
		{
			Block block = world.getBlockAt(coord.getBlockX(), coord.getBlockY(), coord.getBlockZ());
			if (!CreativeGates.isVoid(block))
			{
				MixinMessage.get().messageOne(player, Txt.parse("<b>The gate frame is no longer clear."));
				return false;
			}
		}
		
		EquipmentSlot hand = pending.getHand() != null ? pending.getHand() : EquipmentSlot.HAND;
		ItemStack currentItem = InventoryUtil.getSlot(player, hand);
		Material createMaterial = MConf.get().getMaterialCreate();
		if (currentItem == null || currentItem.getType() != createMaterial)
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>Gate creation cancelled because you are no longer holding the create tool."));
			return false;
		}
		
		ItemMeta meta = InventoryUtil.createMeta(currentItem);
		if (!meta.hasDisplayName())
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>Gate creation cancelled because the create tool is no longer named."));
			return false;
		}
		String networkId = ChatColor.stripColor(meta.getDisplayName());
		if (!pending.getNetworkId().equals(networkId))
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>Gate creation cancelled because the create tool name changed."));
			return false;
		}
		
		UGate newGate = UGateColl.get().create();
		newGate.setCreatorId(IdUtil.getId(player));
		newGate.setNetworkId(pending.getNetworkId());
		newGate.setExit(pending.getExit());
		newGate.setCoords(pending.getCoords());
		newGate.setInteriorCoords(pending.getInteriorCoords());
		newGate.setOrientation(pending.getOrientation());
		newGate.setFillType(gateType);
		
		newGate.fill();
		newGate.fxKitCreate(player);
		
		MixinMessage.get().messageOne(player, Txt.parse("<g>A \"<h>%s<g>\" gate takes form in front of you.", pending.getNetworkId()));
		
		applyCreateToolCost(player, hand, currentItem, createMaterial);
		return true;
	}
	
	public static void cancel(Player player, boolean message)
	{
		PendingGateCreate removed = PendingGateCreates.get().remove(player);
		if (removed == null) return;
		if (message)
		{
			MixinMessage.get().messageOne(player, Txt.parse("<i>Gate creation cancelled."));
		}
	}
	
	private static void applyCreateToolCost(Player player, EquipmentSlot hand, ItemStack currentItem, Material createMaterial)
	{
		if (MConf.get().isRemovingCreateToolItem())
		{
			decreaseOne(player, hand, currentItem);
			MixinMessage.get().messageOne(player, Txt.parse("<i>The %s disappears.", Txt.getMaterialName(createMaterial)));
		}
		else if (MConf.get().isRemovingCreateToolName())
		{
			decreaseOne(player, hand, currentItem);
			
			ItemStack newItemUnnamed = new ItemStack(currentItem);
			ItemMeta newItemUnnamedMeta = InventoryUtil.createMeta(newItemUnnamed);
			newItemUnnamedMeta.setDisplayName(null);
			newItemUnnamed.setItemMeta(newItemUnnamedMeta);
			newItemUnnamed.setAmount(1);
			if (player.getInventory().addItem(newItemUnnamed).size() > 0)
			{
				player.getWorld().dropItemNaturally(player.getLocation(), newItemUnnamed);
			}
			
			InventoryUtil.updateSoon(player);
			MixinMessage.get().messageOne(player, Txt.parse("<i>The %s seems to have lost it's power.", Txt.getMaterialName(createMaterial)));
		}
	}
	
	private static void decreaseOne(Player player, EquipmentSlot hand, ItemStack currentItem)
	{
		ItemStack newItem = new ItemStack(currentItem);
		newItem.setAmount(newItem.getAmount() - 1);
		InventoryUtil.setSlot(player, newItem, hand);
	}
}
